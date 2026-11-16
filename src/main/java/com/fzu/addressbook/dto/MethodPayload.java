package com.fzu.addressbook.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * One contact method inside a create/update request.
 *
 * <p>{@code type} is a string instead of the enum so that the service can answer with a friendly
 * "未知的联系方式类型" message instead of a raw Jackson deserialisation error.</p>
 */
public class MethodPayload {

    @NotBlank(message = "联系方式类型不能为空")
    private String type;

    @Size(max = 50, message = "类型标签不能超过 50 个字符")
    private String label;

    @NotBlank(message = "联系方式内容不能为空")
    @Size(max = 300, message = "联系方式内容不能超过 300 个字符")
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
