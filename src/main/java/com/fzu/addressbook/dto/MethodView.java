package com.fzu.addressbook.dto;

/**
 * One contact method as returned to the browser.
 */
public class MethodView {

    private Long id;
    private String type;
    private String typeLabel;
    private String label;
    private String value;

    public MethodView() {
    }

    public MethodView(Long id, String type, String typeLabel, String label, String value) {
        this.id = id;
        this.type = type;
        this.typeLabel = typeLabel;
        this.label = label;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
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
