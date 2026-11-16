package com.fzu.addressbook.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * One contact method of a person, for example a mobile number, a work e-mail or a campus address.
 *
 * <p>Table {@code contact_methods}:</p>
 * <pre>
 * id          INTEGER PRIMARY KEY AUTOINCREMENT
 * contact_id  INTEGER     NOT NULL, foreign key to contacts.id
 * type        VARCHAR(20) NOT NULL, PHONE / EMAIL / SOCIAL / ADDRESS
 * label       VARCHAR(50) optional free text such as "手机" or "工作邮箱"
 * value       VARCHAR(300) NOT NULL
 * sort_order  INTEGER     NOT NULL, keeps the order chosen by the user
 * </pre>
 */
@Entity
@Table(name = "contact_methods")
public class ContactMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ContactMethodType type;

    @Column(name = "label", length = 50)
    private String label;

    @Column(name = "value", nullable = false, length = 300)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ContactMethod() {
    }

    public ContactMethod(ContactMethodType type, String label, String value) {
        this.type = type;
        this.label = label;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public ContactMethodType getType() {
        return type;
    }

    public void setType(ContactMethodType type) {
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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
