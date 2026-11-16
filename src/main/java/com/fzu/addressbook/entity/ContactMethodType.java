package com.fzu.addressbook.entity;

/**
 * The kind of a contact method.
 *
 * <p>The value stored in the database is the enum name ({@code PHONE}, {@code EMAIL},
 * {@code SOCIAL}, {@code ADDRESS}); the Chinese label is only used by the user interface and by the
 * Excel template.</p>
 */
public enum ContactMethodType {

    PHONE("电话"),
    EMAIL("邮箱"),
    SOCIAL("社交"),
    ADDRESS("地址");

    private final String label;

    ContactMethodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Resolves a type from either the enum name or the Chinese label, case insensitively.
     *
     * @param raw the raw text, for example {@code "PHONE"}, {@code "phone"} or {@code "电话"}
     * @return the matching type, or {@code null} when the text is unknown
     */
    public static ContactMethodType from(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        for (ContactMethodType type : values()) {
            if (type.name().equalsIgnoreCase(normalized) || type.label.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
