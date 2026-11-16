package com.fzu.addressbook.dto;

import com.fzu.addressbook.entity.Contact;
import com.fzu.addressbook.entity.ContactMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A contact as returned to the browser, including all of its contact methods.
 */
public class ContactView {

    private Long id;
    private String name;
    private String company;
    private String title;
    private String group;
    private String notes;
    private boolean bookmarked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MethodView> methods = new ArrayList<>();

    /**
     * Maps an entity to the response shape.
     *
     * @param contact the entity, must not be {@code null}
     * @return the view object
     */
    public static ContactView from(Contact contact) {
        ContactView view = new ContactView();
        view.id = contact.getId();
        view.name = contact.getName();
        view.company = contact.getCompany();
        view.title = contact.getTitle();
        view.group = contact.getGroupName();
        view.notes = contact.getNotes();
        view.bookmarked = contact.isBookmarked();
        view.createdAt = contact.getCreatedAt();
        view.updatedAt = contact.getUpdatedAt();
        for (ContactMethod method : contact.getMethods()) {
            view.methods.add(new MethodView(method.getId(), method.getType().name(),
                    method.getType().getLabel(), method.getLabel(), method.getValue()));
        }
        return view;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getGroup() {
        return group;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<MethodView> getMethods() {
        return methods;
    }
}
