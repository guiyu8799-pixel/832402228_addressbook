package com.fzu.addressbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fzu.addressbook.repository.ContactRepository;
import com.fzu.addressbook.service.ExcelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end HTTP behaviour of the REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ExcelService excelService;

    @BeforeEach
    void cleanDatabase() {
        contactRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/contacts 创建联系人并返回 201")
    void createsContact() throws Exception {
        String body = "{"
                + "\"name\":\"张三\","
                + "\"group\":\"同学\","
                + "\"company\":\"福州大学\","
                + "\"bookmarked\":true,"
                + "\"methods\":["
                + "{\"type\":\"PHONE\",\"label\":\"手机\",\"value\":\"13800000000\"},"
                + "{\"type\":\"EMAIL\",\"value\":\"zhangsan@example.com\"}]"
                + "}";

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("张三"))
                .andExpect(jsonPath("$.bookmarked").value(true))
                .andExpect(jsonPath("$.methods", hasSize(2)))
                .andExpect(jsonPath("$.methods[0].typeLabel").value("电话"));
    }

    @Test
    @DisplayName("姓名为空时返回 400 与 VALIDATION_ERROR")
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0]").value("姓名不能为空"));
    }

    @Test
    @DisplayName("列表接口支持关键词、分组与收藏过滤")
    void listsWithFilters() throws Exception {
        create("张三", "同学", true);
        create("李四", "同事", false);

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("张三"));

        mockMvc.perform(get("/api/contacts").param("q", "李四"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("李四"));

        mockMvc.perform(get("/api/contacts").param("group", "同学"))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/contacts").param("bookmarked", "true"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("张三"));
    }

    @Test
    @DisplayName("PATCH 收藏开关返回更新后的联系人")
    void togglesBookmarkOverHttp() throws Exception {
        long id = create("张三", "同学", false);

        mockMvc.perform(patch("/api/contacts/" + id + "/bookmark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookmarked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    @Test
    @DisplayName("PUT 更新联系人资料")
    void updatesContact() throws Exception {
        long id = create("张三", "同学", false);

        mockMvc.perform(put("/api/contacts/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三丰\",\"group\":\"同学\",\"methods\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("张三丰"))
                .andExpect(jsonPath("$.methods", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE 返回 204，再查询返回 404")
    void deletesContact() throws Exception {
        long id = create("张三", "同学", false);

        mockMvc.perform(delete("/api/contacts/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/contacts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTACT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/contacts/summary 返回统计与分组")
    void returnsSummary() throws Exception {
        create("张三", "同学", true);
        create("李四", "同事", false);

        mockMvc.perform(get("/api/contacts/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.bookmarked").value(1))
                .andExpect(jsonPath("$.groups", hasSize(2)));
    }

    @Test
    @DisplayName("导出接口返回 xlsx 附件")
    void exportsWorkbook() throws Exception {
        create("张三", "同学", true);

        MvcResult result = mockMvc.perform(get("/api/excel/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    @DisplayName("模板接口返回可下载的 xlsx")
    void downloadsTemplate() throws Exception {
        mockMvc.perform(get("/api/excel/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("导入接口接收模板文件并返回导入条数")
    void importsWorkbook() throws Exception {
        byte[] template = excelService.template();
        MockMultipartFile file = new MockMultipartFile("file", "template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", template);

        mockMvc.perform(multipart("/api/excel/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.failed").value(0));

        mockMvc.perform(get("/api/contacts"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("健康检查接口可用")
    void healthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private long create(String name, String group, boolean bookmarked) throws Exception {
        String body = "{"
                + "\"name\":\"" + name + "\","
                + "\"group\":\"" + group + "\","
                + "\"bookmarked\":" + bookmarked + ","
                + "\"methods\":[{\"type\":\"PHONE\",\"label\":\"手机\",\"value\":\"13800000000\"}]"
                + "}";
        MvcResult result = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        int index = json.indexOf("\"id\":") + 5;
        int end = index;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(index, end));
    }
}
