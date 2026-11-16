package com.fzu.addressbook.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * One person in the address book.
 *
 * <p>Table {@code contacts}:</p>
 * <pre>
 * id          INTEGER PRIMARY KEY AUTOINCREMENT
 * name        VARCHAR(100)  NOT NULL
 * company     VARCHAR(100)
 * title       VARCHAR(100)
 * grp         VARCHAR(50)   group / category, "grp" avoids the SQL keyword
 * notes       VARCHAR(1000)
 * bookmarked  BOOLEAN       NOT NULL, 1 when the contact is a favourite
 * created_at  TIMESTAMP     NOT NULL
 * updated_at  TIMESTAMP     NOT NULL
 * </pre>
 *
 * <p>Contact methods are stored in {@code contact_methods} and cascade with the contact, which keeps
 * the "one contact, many phone numbers / e-mails / addresses" relation in a normalised shape.</p>
 */
@Entity
@Table(name = "contacts", indexes = {
        @Index(name = "idx_contacts_bookmarked", columnList = "bookmarked"),
        @Index(name = "idx_contacts_group", columnList = "grp")
})
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "company", length = 100)
    private String company;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "grp", length = 50)
    private String groupName;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "bookmarked", nullable = false)
    private boolean bookmarked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ContactMethod> methods = new ArrayList<>();

    public Contact() {
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Adds a method and keeps both sides of the relation in sync.
     *
     * @param method the method to attach; its sort order is set when it is still zero
     */
    public void addMethod(ContactMethod method) {
        if (method.getSortOrder() == 0) {
            method.setSortOrder(methods.size());
        }
        method.setContact(this);
        methods.add(method);
    }

    /**
     * Replaces every method of this contact. Orphan removal deletes the rows that are no longer
     * referenced, so editing a contact never leaves stale methods behind.
     *
     * @param replacements the new methods, possibly empty
     */
    public void replaceMethods(List<ContactMethod> replacements) {
        methods.clear();
        for (ContactMethod method : replacements) {
            addMethod(method);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ContactMethod> getMethods() {
        return methods;
    }
}
