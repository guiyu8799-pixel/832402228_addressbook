package com.fzu.addressbook.service;

import com.fzu.addressbook.dto.ContactPayload;
import com.fzu.addressbook.dto.ContactView;
import com.fzu.addressbook.dto.ImportResult;
import com.fzu.addressbook.dto.MethodPayload;
import com.fzu.addressbook.dto.MethodView;
import com.fzu.addressbook.entity.ContactMethodType;
import com.fzu.addressbook.exception.ApiException;
import com.fzu.addressbook.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel import and export.
 *
 * <p>Layout: one row per contact, one column per kind of information. Because a contact can own
 * several methods of the same kind (two phones, three e-mails), all methods of one kind are packed
 * into one cell as {@code 标签: 值; 标签: 值}. The import reads the same convention back, so an exported
 * file can be imported again without losing information.</p>
 */
@Service
public class ExcelService {

    /** Column headers, in export order. */
    static final List<String> HEADERS = Arrays.asList(
            "姓名", "分组", "公司", "职位", "收藏", "电话", "邮箱", "社交", "地址", "备注");

    private static final Map<String, Set<String>> HEADER_ALIASES = buildAliases();

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.CHINA);

    private final ContactService contactService;

    public ExcelService(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Exports every contact to an {@code .xlsx} workbook.
     *
     * @param contacts contacts to write, in display order
     * @return the workbook bytes
     */
    public byte[] export(List<ContactView> contacts) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("通讯录");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (ContactView contact : contacts) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, contact.getName(), bodyStyle);
                writeCell(row, 1, contact.getGroup(), bodyStyle);
                writeCell(row, 2, contact.getCompany(), bodyStyle);
                writeCell(row, 3, contact.getTitle(), bodyStyle);
                writeCell(row, 4, contact.isBookmarked() ? "是" : "否", bodyStyle);
                writeCell(row, 5, methodsCell(contact, ContactMethodType.PHONE), bodyStyle);
                writeCell(row, 6, methodsCell(contact, ContactMethodType.EMAIL), bodyStyle);
                writeCell(row, 7, methodsCell(contact, ContactMethodType.SOCIAL), bodyStyle);
                writeCell(row, 8, methodsCell(contact, ContactMethodType.ADDRESS), bodyStyle);
                writeCell(row, 9, contact.getNotes(), bodyStyle);
            }

            for (int index = 0; index < HEADERS.size(); index++) {
                sheet.setColumnWidth(index, widthFor(index));
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(0, rowIndex - 1), 0, HEADERS.size() - 1));

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "导出失败：" + exception.getMessage());
        }
    }

    /**
     * Builds an empty template with the expected headers and two example rows.
     *
     * @return the workbook bytes
     */
    public byte[] template() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("通讯录");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle bodyStyle = bodyStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(HEADERS.get(index));
                cell.setCellStyle(headerStyle);
            }

            List<String[]> examples = new ArrayList<>();
            examples.add(new String[]{"张三", "同学", "福州大学", "本科生", "是",
                    "手机: 13800000000; 宿舍: 0591-1234567",
                    "邮箱: zhangsan@example.com", "微信: zhangsan_wx",
                    "地址: 福州市闽侯县学园路 2 号", "同班同学"});
            examples.add(new String[]{"李四", "同事", "示例科技有限公司", "工程师", "否",
                    "手机: 13900000000", "工作邮箱: lisi@example.com", "企业微信: lisi_wx",
                    "", "项目搭档"});
            int rowIndex = 1;
            for (String[] example : examples) {
                Row row = sheet.createRow(rowIndex++);
                for (int index = 0; index < example.length; index++) {
                    writeCell(row, index, example[index], bodyStyle);
                }
            }

            for (int index = 0; index < HEADERS.size(); index++) {
                sheet.setColumnWidth(index, widthFor(index));
            }
            sheet.createFreezePane(0, 1);

            Sheet help = workbook.createSheet("填写说明");
            String[] notes = {
                    "每行一个联系人，列顺序与表头一致，表头名称不要修改。",
                    "姓名必填，其余列可以留空。",
                    "同一类联系方式有多条时，写在同一格里，用分号分隔，格式为「标签: 内容」。",
                    "例如：手机: 13800000000; 宿舍: 0591-1234567",
                    "收藏列填「是」或「否」。",
                    "导出的文件可以直接再导入，信息不会丢失。"
            };
            for (int index = 0; index < notes.length; index++) {
                help.createRow(index).createCell(0).setCellValue(notes[index]);
            }
            help.setColumnWidth(0, 80 * 256);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "生成模板失败：" + exception.getMessage());
        }
    }

    /**
     * Imports contacts from an Excel file.
     *
     * <p>Rows are imported one by one: a malformed row is reported with its row number and does not
     * abort the whole file. That way a single typo never costs the user the other 200 rows.</p>
     *
     * @param file uploaded workbook
     * @return imported and failed counters plus one message per rejected row
     */
    public ImportResult importFrom(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.BAD_REQUEST, "请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.BAD_REQUEST,
                    "只支持 .xlsx 或 .xls 文件");
        }

        List<String> errors = new ArrayList<>();
        int imported = 0;
        int failed = 0;

        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.BAD_REQUEST, "工作簿中没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> columns = readHeader(sheet.getRow(0
            ));
            if (columns.isEmpty()) {
                throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.BAD_REQUEST,
                        "表头为空，请使用模板或导出的文件作为格式参考");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row)) {
                    continue;
                }
                int humanRow = rowIndex + 1;
                String name = cell(row, columns.get("姓名"));
                if (name == null || name.trim().isEmpty()) {
                    failed++;
                    errors.add("第 " + humanRow + " 行：姓名不能为空，已跳过");
                    continue;
                }
                try {
                    ContactPayload payload = buildPayload(row, columns, name.trim());
                    contactService.create(payload);
                    imported++;
                } catch (ApiException exception) {
                    failed++;
                    errors.add("第 " + humanRow + " 行：" + exception.getMessage());
                } catch (RuntimeException exception) {
                    failed++;
                    errors.add("第 " + humanRow + " 行：无法解析（" + exception.getMessage() + "）");
                }
            }
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.BAD_REQUEST,
                    "无法读取 Excel 文件：" + exception.getMessage());
        } catch (org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException
                 | org.apache.poi.EmptyFileException exception) {
            throw new ApiException(ErrorCode.IMPORT_ERROR, HttpStatus.BAD_REQUEST,
                    "文件不是有效的 Excel 工作簿");
        }

        return new ImportResult(imported, failed, errors);
    }

    private ContactPayload buildPayload(Row row, Map<String, Integer> columns, String name) {
        ContactPayload payload = new ContactPayload();
        payload.setName(name);
        payload.setGroup(cell(row, columns.get("分组")));
        payload.setCompany(cell(row, columns.get("公司")));
        payload.setTitle(cell(row, columns.get("职位")));
        payload.setNotes(cell(row, columns.get("备注")));
        payload.setBookmarked(parseBoolean(cell(row, columns.get("收藏"))));

        List<MethodPayload> methods = new ArrayList<>();
        methods.addAll(parseMethods(cell(row, columns.get("电话")), ContactMethodType.PHONE));
        methods.addAll(parseMethods(cell(row, columns.get("邮箱")), ContactMethodType.EMAIL));
        methods.addAll(parseMethods(cell(row, columns.get("社交")), ContactMethodType.SOCIAL));
        methods.addAll(parseMethods(cell(row, columns.get("地址")), ContactMethodType.ADDRESS));
        payload.setMethods(methods);
        return payload;
    }

    /**
     * Parses one cell such as {@code "手机: 13800000000; 宿舍: 0591-1234567"} into payloads.
     *
     * @param raw  raw cell text, may be {@code null}
     * @param type method type of the column
     * @return parsed methods, never {@code null}
     */
    List<MethodPayload> parseMethods(String raw, ContactMethodType type) {
        List<MethodPayload> methods = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return methods;
        }
        String[] parts = raw.split("[;；\\n\\r]+");
        for (String part : parts) {
            String segment = part.trim();
            if (segment.isEmpty()) {
                continue;
            }
            String label = null;
            String value = segment;
            int separator = isUrlLike(segment) ? -1 : indexOfSeparator(segment);
            if (separator > 0) {
                String candidateLabel = segment.substring(0, separator).trim();
                String candidateValue = segment.substring(separator + 1).trim();
                if (isLabel(candidateLabel) && !candidateValue.isEmpty()) {
                    label = candidateLabel;
                    value = candidateValue;
                }
            }
            if (value.isEmpty()) {
                continue;
            }
            MethodPayload payload = new MethodPayload();
            payload.setType(type.name());
            payload.setLabel(label);
            payload.setValue(value);
            methods.add(payload);
        }
        return methods;
    }

    private int indexOfSeparator(String segment) {
        int fullWidth = segment.indexOf('：');
        int ascii = segment.indexOf(':');
        if (fullWidth < 0) {
            return ascii;
        }
        if (ascii < 0) {
            return fullWidth;
        }
        return Math.min(fullWidth, ascii);
    }

    private boolean isLabel(String candidate) {
        if (candidate.isEmpty() || candidate.length() > 20) {
            return false;
        }
        // "https://..." and "mailto:..." are values, not "label: value" pairs.
        return !candidate.contains("/") && !candidate.contains("\\");
    }

    /**
     * Tells whether a segment is a bare URL or URI instead of a {@code 标签: 值} pair.
     *
     * @param segment trimmed cell segment
     * @return {@code true} when the colon belongs to a scheme, not to a label
     */
    private boolean isUrlLike(String segment) {
        String lower = segment.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("mailto:")
                || lower.startsWith("tel:")
                || segment.matches("^[a-zA-Z][a-zA-Z0-9+.\\-]*://.*");
    }

    private Map<String, Integer> readHeader(Row headerRow) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        if (headerRow == null) {
            return columns;
        }
        for (int index = 0; index < headerRow.getLastCellNum(); index++) {
            String raw = cell(headerRow, index);
            if (raw == null) {
                continue;
            }
            String canonical = canonicalHeader(raw.trim());
            if (canonical != null && !columns.containsKey(canonical)) {
                columns.put(canonical, index);
            }
        }
        return columns;
    }

    private String canonicalHeader(String raw) {
        for (Map.Entry<String, Set<String>> entry : HEADER_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias.equalsIgnoreCase(raw)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private boolean isBlankRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < row.getLastCellNum(); index++) {
            String value = cell(row, index);
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean parseBoolean(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("是") || normalized.equals("true") || normalized.equals("1")
                || normalized.equals("yes") || normalized.equals("★") || normalized.equals("y");
    }

    private String cell(Row row, Integer column) {
        if (row == null || column == null) {
            return null;
        }
        Cell cell = row.getCell(column);
        if (cell == null) {
            return null;
        }
        String value = FORMATTER.formatCellValue(cell);
        return value == null ? null : value.trim();
    }

    private String methodsCell(ContactView contact, ContactMethodType type) {
        return contact.getMethods().stream()
                .filter(method -> method.getType().equals(type.name()))
                .map(method -> method.getLabel() == null || method.getLabel().isEmpty()
                        ? method.getValue()
                        : method.getLabel() + ": " + method.getValue())
                .collect(Collectors.joining("; "));
    }

    private void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorder(style);
        return style;
    }

    private CellStyle bodyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        applyBorder(style);
        return style;
    }

    private void applyBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private int widthFor(int column) {
        switch (column) {
            case 0:
                return 14 * 256;
            case 5:
            case 6:
            case 7:
            case 8:
                return 34 * 256;
            case 9:
                return 28 * 256;
            default:
                return 16 * 256;
        }
    }

    private static Map<String, Set<String>> buildAliases() {
        Map<String, Set<String>> aliases = new HashMap<>();
        aliases.put("姓名", set("姓名", "名字", "name", "contact", "联系人"));
        aliases.put("分组", set("分组", "组", "分类", "group", "category"));
        aliases.put("公司", set("公司", "单位", "company", "organization"));
        aliases.put("职位", set("职位", "职务", "title", "position"));
        aliases.put("收藏", set("收藏", "星标", "收藏?", "bookmark", "bookmarked", "favorite", "favourite"));
        aliases.put("电话", set("电话", "手机", "电话号码", "phone", "mobile", "tel"));
        aliases.put("邮箱", set("邮箱", "邮件", "电子邮箱", "email", "mail", "e-mail"));
        aliases.put("社交", set("社交", "社交账号", "微信", "social", "wechat", "qq"));
        aliases.put("地址", set("地址", "住址", "address"));
        aliases.put("备注", set("备注", "说明", "notes", "note", "remark"));
        return aliases;
    }

    private static Set<String> set(String... values) {
        return Arrays.stream(values).collect(Collectors.toSet());
    }
}
