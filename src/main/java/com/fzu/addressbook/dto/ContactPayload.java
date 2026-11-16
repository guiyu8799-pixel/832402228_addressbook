package com.fzu.addressbook.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Request body used to create or update a contact.
 */
public class ContactPayload {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名不能超过 100 个字符")
    private String name;

    @Size(max = 100, message = "公司不能超过 100 个字符")
    private String company;

    @Size(max = 100, message = "职位不能超过 100 个字符")
    private String title;

    @Size(max = 50, message = "分组不能超过 50 个字符")
    private String group;

    @Size(max = 1000, message = "备注不能超过 1000 个字符")
    private String notes;

    private Boolean bookmarked;

    @Valid
    private List<MethodPayload> methods = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(Boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public List<MethodPayload> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodPayload> methods) {
        this.methods = methods;
    }
}
