package com.fzu.addressbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fzu.addressbook.dto.ContactPayload;
import com.fzu.addressbook.dto.ContactView;
import com.fzu.addressbook.dto.ImportResult;
import com.fzu.addressbook.dto.MethodPayload;
import com.fzu.addressbook.dto.MethodView;
import com.fzu.addressbook.entity.ContactMethodType;
import com.fzu.addressbook.exception.ApiException;
import com.fzu.addressbook.repository.ContactRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

/**
 * Excel export/import behaviour, including the round trip that matters most: an exported file must
 * be importable again without losing any contact method.
 */
@SpringBootTest
@ActiveProfiles("test")
class ExcelServiceTest {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void cleanDatabase() {
        contactRepository.deleteAll();
    }

    @Test
    @DisplayName("导出文件包含固定的表头和每个联系人的一行数据")
    void exportsHeaderAndRows() throws Exception {
        ContactPayload payload = payload("张三", "同学");
        payload.setBookmarked(true);
        payload.setMethods(Arrays.asList(
                method("PHONE", "手机", "13800000000"),
                method("PHONE", "宿舍", "0591-1234567"),
                method("EMAIL", null, "zhangsan@example.com")));
        contactService.create(payload);

        byte[] bytes = excelService.export(contactService.list(null, null, null));
        Sheet sheet = firstSheet(bytes);

        assertThat(cell(sheet, 0, 0)).isEqualTo("姓名");
        assertThat(cell(sheet, 0, 5)).isEqualTo("电话");
        assertThat(cell(sheet, 0, 9)).isEqualTo("备注");
        assertThat(sheet.getLastRowNum()).isEqualTo(1);
        assertThat(cell(sheet, 1, 0)).isEqualTo("张三");
        assertThat(cell(sheet, 1, 1)).isEqualTo("同学");
        assertThat(cell(sheet, 1, 4)).isEqualTo("是");
        assertThat(cell(sheet, 1, 5)).isEqualTo("手机: 13800000000; 宿舍: 0591-1234567");
        assertThat(cell(sheet, 1, 6)).isEqualTo("zhangsan@example.com");
    }

    @Test
    @DisplayName("导出后再导入，联系方式与标签完全保留")
    void exportImportRoundTripKeepsEveryMethod() {
        ContactPayload first = payload("张三", "同学");
        first.setBookmarked(true);
        first.setCompany("福州大学");
        first.setTitle("本科生");
        first.setNotes("同班同学");
        first.setMethods(Arrays.asList(
                method("PHONE", "手机", "13800000000"),
                method("PHONE", "宿舍", "0591-1234567"),
                method("EMAIL", "邮箱", "zhangsan@example.com"),
                method("SOCIAL", "微信", "zhangsan_wx"),
                method("ADDRESS", "宿舍地址", "福州市闽侯县学园路 2 号")));
        contactService.create(first);

        ContactPayload second = payload("李四", "同事");
        second.setMethods(Arrays.asList(method("EMAIL", "工作邮箱", "lisi@example.com")));
        contactService.create(second);

        byte[] exported = excelService.export(contactService.list(null, null, null));
        ImportResult result = excelService.importFrom(file(exported, "export.xlsx"));

        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getFailed()).isZero();

        List<ContactView> all = contactService.list(null, null, null);
        assertThat(all).hasSize(4);

        ContactView reimported = all.stream()
                .filter(view -> view.getName().equals("张三") && view.getMethods().size() == 5)
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertThat(reimported.isBookmarked()).isTrue();
        assertThat(reimported.getCompany()).isEqualTo("福州大学");
        assertThat(reimported.getMethods())
                .extracting(method -> method.getType() + "|" + method.getLabel() + "|" + method.getValue())
                .containsExactlyInAnyOrder(
                        "PHONE|手机|13800000000",
                        "PHONE|宿舍|0591-1234567",
                        "EMAIL|邮箱|zhangsan@example.com",
                        "SOCIAL|微信|zhangsan_wx",
                        "ADDRESS|宿舍地址|福州市闽侯县学园路 2 号");
    }

    @Test
    @DisplayName("模板包含表头、两行示例和填写说明")
    void templateContainsGuide() throws Exception {
        byte[] bytes = excelService.template();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(cell(sheet, 0, 0)).isEqualTo("姓名");
            assertThat(cell(sheet, 1, 0)).isEqualTo("张三");
            assertThat(cell(sheet, 2, 0)).isEqualTo("李四");
            assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("填写说明");
            assertThat(cell(workbook.getSheetAt(1), 0, 0)).contains("每行一个联系人");
        }
    }

    @Test
    @DisplayName("缺少姓名的行会被跳过并报告行号，其余行照常导入")
    void reportsRowsWithoutName() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"", "同学", "", "", "否", "13800000000", "", "", "", ""});
        rows.add(new String[]{"李四", "同事", "", "", "否", "13900000000", "", "", "", ""});
        byte[] bytes = workbook(Arrays.asList("姓名", "分组", "公司", "职位", "收藏", "电话", "邮箱", "社交", "地址", "备注"), rows);

        ImportResult result = excelService.importFrom(file(bytes, "partial.xlsx"));

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("第 2 行").contains("姓名不能为空");
    }

    @Test
    @DisplayName("英文表头也能识别")
    void acceptsEnglishHeaders() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Alice", "classmate", "FZU", "student", "yes",
                "Mobile: 13800000001", "alice@example.com", "", "", "note"});
        byte[] bytes = workbook(
                Arrays.asList("Name", "Group", "Company", "Title", "Bookmark", "Phone", "Email", "Social", "Address", "Notes"),
                rows);

        ImportResult result = excelService.importFrom(file(bytes, "english.xlsx"));

        assertThat(result.getImported()).isEqualTo(1);
        ContactView contact = contactService.list("Alice", null, null).get(0);
        assertThat(contact.isBookmarked()).isTrue();
        assertThat(contact.getGroup()).isEqualTo("classmate");
        assertThat(contact.getMethods()).extracting(MethodView::getValue)
                .contains("13800000001", "alice@example.com");
    }

    @Test
    @DisplayName("社交账号里的网址不会被误拆成「标签: 值」")
    void keepsUrlAsValue() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"王五", "", "", "", "否", "", "", "https://github.com/wangwu", "", ""});
        byte[] bytes = workbook(
                Arrays.asList("姓名", "分组", "公司", "职位", "收藏", "电话", "邮箱", "社交", "地址", "备注"), rows);

        excelService.importFrom(file(bytes, "url.xlsx"));

        ContactView contact = contactService.list("王五", null, null).get(0);
        assertThat(contact.getMethods()).hasSize(1);
        assertThat(contact.getMethods().get(0).getType()).isEqualTo("SOCIAL");
        assertThat(contact.getMethods().get(0).getLabel()).isNull();
        assertThat(contact.getMethods().get(0).getValue()).isEqualTo("https://github.com/wangwu");
    }

    @Test
    @DisplayName("解析器按分号拆分多条联系方式，并保留值里的冒号")
    void parsesMethodCells() {
        ExcelService parser = new ExcelService(null);

        List<MethodPayload> phone = parser.parseMethods(
                "手机: 13800000000；备用: 13900000000", ContactMethodType.PHONE);
        assertThat(phone).hasSize(2);
        assertThat(phone.get(1).getValue()).isEqualTo("13900000000");

        List<MethodPayload> email = parser.parseMethods("mailto:someone@example.com", ContactMethodType.EMAIL);
        assertThat(email).hasSize(1);
        assertThat(email.get(0).getLabel()).isNull();
        assertThat(email.get(0).getValue()).isEqualTo("mailto:someone@example.com");

        assertThat(parser.parseMethods("   ", ContactMethodType.PHONE)).isEmpty();
        assertThat(parser.parseMethods(null, ContactMethodType.PHONE)).isEmpty();
    }

    @Test
    @DisplayName("非 Excel 文件会被拒绝")
    void rejectsNonExcelFile() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
                "hello".getBytes());

        assertThatThrownBy(() -> excelService.importFrom(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(".xlsx");
    }

    @Test
    @DisplayName("空文件会被拒绝")
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        assertThatThrownBy(() -> excelService.importFrom(file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("请选择");
    }

    private ContactPayload payload(String name, String group) {
        ContactPayload payload = new ContactPayload();
        payload.setName(name);
        payload.setGroup(group);
        return payload;
    }

    private MethodPayload method(String type, String label, String value) {
        MethodPayload method = new MethodPayload();
        method.setType(type);
        method.setLabel(label);
        method.setValue(value);
        return method;
    }

    private MockMultipartFile file(byte[] bytes, String name) {
        return new MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
    }

    private Sheet firstSheet(byte[] bytes) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(bytes)).getSheetAt(0);
    }

    private String cell(Sheet sheet, int rowIndex, int columnIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null || row.getCell(columnIndex) == null) {
            return null;
        }
        return row.getCell(columnIndex).getStringCellValue();
    }

    /**
     * Writes a workbook with the given header and rows so that the test controls the exact layout
     * a user might hand in.
     */
    private byte[] workbook(List<String> headers, List<String[]> rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("通讯录");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.size(); index++) {
                headerRow.createCell(index).setCellValue(headers.get(index));
            }
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                String[] values = rows.get(rowIndex);
                for (int column = 0; column < values.length; column++) {
                    row.createCell(column).setCellValue(values[column]);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
