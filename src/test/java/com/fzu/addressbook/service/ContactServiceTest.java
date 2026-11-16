package com.fzu.addressbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fzu.addressbook.dto.ContactPayload;
import com.fzu.addressbook.dto.ContactView;
import com.fzu.addressbook.dto.MethodPayload;
import com.fzu.addressbook.dto.SummaryView;
import com.fzu.addressbook.exception.ApiException;
import com.fzu.addressbook.exception.ErrorCode;
import com.fzu.addressbook.repository.ContactRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Behaviour of the contact service against a real SQLite database.
 */
@SpringBootTest
@ActiveProfiles("test")
class ContactServiceTest {

    @Autowired
    private ContactService contactService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        contactRepository.deleteAll();
    }

    @Test
    @DisplayName("新建联系人时保存全部联系方式，并保持用户输入的顺序")
    void createsContactWithAllMethods() {
        ContactPayload payload = payload("张三");
        payload.setMethods(Arrays.asList(
                method("PHONE", "手机", "13800000000"),
                method("PHONE", "宿舍", "0591-1234567"),
                method("EMAIL", null, "zhangsan@example.com"),
                method("SOCIAL", "微信", "zhangsan_wx"),
                method("ADDRESS", null, "福州市闽侯县学园路 2 号")));

        ContactView created = contactService.create(payload);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("张三");
        assertThat(created.getMethods()).hasSize(5);
        assertThat(created.getMethods().get(0).getLabel()).isEqualTo("手机");
        assertThat(created.getMethods().get(1).getValue()).isEqualTo("0591-1234567");
        assertThat(created.getMethods().get(2).getType()).isEqualTo("EMAIL");
        assertThat(created.getMethods().get(2).getTypeLabel()).isEqualTo("邮箱");
    }

    @Test
    @DisplayName("未知的联系方式类型会被拒绝")
    void rejectsUnknownMethodType() {
        ContactPayload payload = payload("李四");
        payload.setMethods(Arrays.asList(method("FAX", null, "123")));

        assertThatThrownBy(() -> contactService.create(payload))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_METHOD_TYPE));
    }

    @Test
    @DisplayName("空白联系方式会被忽略而不是产生空行")
    void ignoresBlankMethodValues() {
        ContactPayload payload = payload("王五");
        payload.setMethods(Arrays.asList(method("PHONE", "手机", "   "),
                method("EMAIL", null, "ww@example.com")));

        ContactView created = contactService.create(payload);

        assertThat(created.getMethods()).hasSize(1);
        assertThat(created.getMethods().get(0).getType()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("收藏的联系人排在列表最前面")
    void bookmarkedContactsSortFirst() {
        contactService.create(payload("张三"));
        ContactPayload favourite = payload("李四");
        favourite.setBookmarked(true);
        contactService.create(favourite);

        List<ContactView> contacts = contactService.list(null, null, null);

        assertThat(contacts).extracting(ContactView::getName).containsExactly("李四", "张三");
    }

    @Test
    @DisplayName("收藏开关可以反复切换")
    void togglesBookmark() {
        ContactPayload payload = payload("张三");
        ContactView created = contactService.create(payload);

        ContactView on = contactService.setBookmarked(created.getId(), true);
        assertThat(on.isBookmarked()).isTrue();
        assertThat(contactService.list(null, null, true)).hasSize(1);

        ContactView off = contactService.setBookmarked(created.getId(), false);
        assertThat(off.isBookmarked()).isFalse();
        assertThat(contactService.list(null, null, true)).isEmpty();
    }

    @Test
    @DisplayName("关键词可以命中姓名、公司和任何一个联系方式")
    void searchesAcrossFields() {
        ContactPayload payload = payload("张三");
        payload.setCompany("福州大学");
        payload.setMethods(Arrays.asList(method("PHONE", "手机", "13800001111")));
        contactService.create(payload);
        contactService.create(payload("李四"));

        assertThat(contactService.list("张三", null, null)).hasSize(1);
        assertThat(contactService.list("福州大学", null, null)).hasSize(1);
        assertThat(contactService.list("13800001111", null, null)).hasSize(1);
        assertThat(contactService.list("不存在", null, null)).isEmpty();
    }

    @Test
    @DisplayName("按分组过滤只返回该分组的联系人")
    void filtersByGroup() {
        ContactPayload classmate = payload("张三");
        classmate.setGroup("同学");
        contactService.create(classmate);
        ContactPayload colleague = payload("李四");
        colleague.setGroup("同事");
        contactService.create(colleague);

        List<ContactView> contacts = contactService.list(null, "同学", null);

        assertThat(contacts).extracting(ContactView::getName).containsExactly("张三");
    }

    @Test
    @DisplayName("统计接口给出总数、收藏数和分组计数")
    void summarisesCounters() {
        ContactPayload first = payload("张三");
        first.setGroup("同学");
        first.setBookmarked(true);
        contactService.create(first);
        ContactPayload second = payload("李四");
        second.setGroup("同事");
        contactService.create(second);

        SummaryView summary = contactService.summary();

        assertThat(summary.getTotal()).isEqualTo(2);
        assertThat(summary.getBookmarked()).isEqualTo(1);
        assertThat(summary.getGroups()).extracting(SummaryView.GroupCount::getName)
                .containsExactlyInAnyOrder("同学", "同事");
    }

    @Test
    @DisplayName("更新联系人会用新联系方式整体替换旧列表，不留下孤儿行")
    void updateReplacesMethodsCompletely() {
        ContactPayload payload = payload("张三");
        payload.setMethods(Arrays.asList(
                method("PHONE", "手机", "13800000000"),
                method("EMAIL", null, "old@example.com")));
        ContactView created = contactService.create(payload);

        ContactPayload update = payload("张三丰");
        update.setMethods(Arrays.asList(method("PHONE", "新手机", "13900000000")));
        contactService.update(created.getId(), update);

        ContactView reloaded = contactService.get(created.getId());
        assertThat(reloaded.getName()).isEqualTo("张三丰");
        assertThat(reloaded.getMethods()).hasSize(1);
        assertThat(reloaded.getMethods().get(0).getValue()).isEqualTo("13900000000");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from contact_methods", Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("删除联系人会级联删除其联系方式")
    void deleteCascadesMethods() {
        ContactPayload payload = payload("张三");
        payload.setMethods(Arrays.asList(method("PHONE", "手机", "13800000000")));
        ContactView created = contactService.create(payload);

        contactService.delete(created.getId());

        assertThat(contactRepository.findById(created.getId())).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from contact_methods", Long.class)).isZero();
    }

    @Test
    @DisplayName("访问不存在的联系人返回 404")
    void missingContactIsNotFound() {
        assertThatThrownBy(() -> contactService.get(9999L))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getStatus().value())
                        .isEqualTo(404));
    }

    private ContactPayload payload(String name) {
        ContactPayload payload = new ContactPayload();
        payload.setName(name);
        return payload;
    }

    private MethodPayload method(String type, String label, String value) {
        MethodPayload method = new MethodPayload();
        method.setType(type);
        method.setLabel(label);
        method.setValue(value);
        return method;
    }
}
