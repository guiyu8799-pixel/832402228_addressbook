package com.fzu.addressbook.service;

import com.fzu.addressbook.dto.ContactPayload;
import com.fzu.addressbook.dto.ContactView;
import com.fzu.addressbook.dto.MethodPayload;
import com.fzu.addressbook.dto.SummaryView;
import com.fzu.addressbook.entity.Contact;
import com.fzu.addressbook.entity.ContactMethod;
import com.fzu.addressbook.entity.ContactMethodType;
import com.fzu.addressbook.exception.ApiException;
import com.fzu.addressbook.exception.ErrorCode;
import com.fzu.addressbook.repository.ContactRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for contacts: CRUD, bookmarking, filtering and the sidebar summary.
 */
@Service
@Transactional
public class ContactService {

    private final ContactRepository repository;

    public ContactService(ContactRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists contacts, optionally filtered by keyword, group and bookmark state.
     *
     * <p>The keyword searches name, company, title, group and every contact method value/label, so
     * typing a phone number finds the person who owns it. Bookmarked contacts always sort first,
     * then by name.</p>
     *
     * @param keyword   free text, may be {@code null} or blank
     * @param group     exact group name, may be {@code null} or blank
     * @param bookmarked only bookmarked ({@code true}) or only normal ({@code false}) contacts
     * @return the matching contacts
     */
    @Transactional(readOnly = true)
    public List<ContactView> list(String keyword, String group, Boolean bookmarked) {
        Specification<Contact> specification = buildSpecification(keyword, group, bookmarked);
        Sort sort = Sort.by(Sort.Order.desc("bookmarked"), Sort.Order.asc("name"));
        return repository.findAll(specification, sort).stream()
                .map(ContactView::from)
                .collect(Collectors.toList());
    }

    /**
     * Loads a single contact.
     *
     * @param id contact id
     * @return the contact
     * @throws ApiException with 404 when the id is unknown
     */
    @Transactional(readOnly = true)
    public ContactView get(Long id) {
        return ContactView.from(requireContact(id));
    }

    /**
     * Creates a contact from a request payload.
     *
     * @param payload validated payload
     * @return the stored contact
     */
    public ContactView create(ContactPayload payload) {
        Contact contact = new Contact();
        applyPayload(contact, payload);
        return ContactView.from(repository.save(contact));
    }

    /**
     * Replaces the editable fields and the full method list of a contact.
     *
     * @param id      contact id
     * @param payload validated payload
     * @return the updated contact
     */
    public ContactView update(Long id, ContactPayload payload) {
        Contact contact = requireContact(id);
        applyPayload(contact, payload);
        return ContactView.from(repository.save(contact));
    }

    /**
     * Deletes a contact and, by cascade, all of its methods.
     *
     * @param id contact id
     */
    public void delete(Long id) {
        repository.delete(requireContact(id));
    }

    /**
     * Sets the bookmark flag of a contact.
     *
     * @param id         contact id
     * @param bookmarked new flag value
     * @return the updated contact
     */
    public ContactView setBookmarked(Long id, boolean bookmarked) {
        Contact contact = requireContact(id);
        contact.setBookmarked(bookmarked);
        return ContactView.from(repository.save(contact));
    }

    /**
     * Counts contacts and groups for the sidebar.
     *
     * @return totals plus one row per group
     */
    @Transactional(readOnly = true)
    public SummaryView summary() {
        List<Contact> contacts = repository.findAll();
        SummaryView summary = new SummaryView();
        summary.setTotal(contacts.size());
        summary.setBookmarked(contacts.stream().filter(Contact::isBookmarked).count());

        Map<String, Long> grouped = new LinkedHashMap<>();
        for (Contact contact : contacts) {
            String group = normalize(contact.getGroupName());
            if (group != null) {
                grouped.merge(group, 1L, Long::sum);
            }
        }
        summary.setGroups(grouped.entrySet().stream()
                .sorted((left, right) -> {
                    int byCount = Long.compare(right.getValue(), left.getValue());
                    return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
                })
                .map(entry -> new SummaryView.GroupCount(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
        return summary;
    }

    /**
     * Copies the payload onto an entity, translating and validating the method list.
     *
     * @param contact target entity
     * @param payload source payload
     */
    private void applyPayload(Contact contact, ContactPayload payload) {
        contact.setName(payload.getName().trim());
        contact.setCompany(trimToNull(payload.getCompany()));
        contact.setTitle(trimToNull(payload.getTitle()));
        contact.setGroupName(trimToNull(payload.getGroup()));
        contact.setNotes(trimToNull(payload.getNotes()));
        contact.setBookmarked(Boolean.TRUE.equals(payload.getBookmarked()));

        List<ContactMethod> methods = new ArrayList<>();
        if (payload.getMethods() != null) {
            for (MethodPayload methodPayload : payload.getMethods()) {
                ContactMethodType type = ContactMethodType.from(methodPayload.getType());
                if (type == null) {
                    throw new ApiException(ErrorCode.INVALID_METHOD_TYPE, HttpStatus.BAD_REQUEST,
                            "未知的联系方式类型：" + methodPayload.getType());
                }
                String value = trimToNull(methodPayload.getValue());
                if (value == null) {
                    continue;
                }
                methods.add(new ContactMethod(type, trimToNull(methodPayload.getLabel()), value));
            }
        }
        contact.replaceMethods(methods);
    }

    private Contact requireContact(Long id) {
        return repository.findWithMethodsById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.CONTACT_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "联系人不存在：" + id));
    }

    private Specification<Contact> buildSpecification(String keyword, String group, Boolean bookmarked) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedKeyword = normalize(keyword);
            if (normalizedKeyword != null) {
                String like = "%" + normalizedKeyword.toLowerCase() + "%";
                Join<Contact, ContactMethod> methods = root.join("methods", JoinType.LEFT);
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name").as(String.class)), like),
                        builder.like(builder.lower(builder.coalesce(
                                root.get("company").as(String.class), "")), like),
                        builder.like(builder.lower(builder.coalesce(
                                root.get("title").as(String.class), "")), like),
                        builder.like(builder.lower(builder.coalesce(
                                root.get("groupName").as(String.class), "")), like),
                        builder.like(builder.lower(builder.coalesce(
                                methods.get("value").as(String.class), "")), like),
                        builder.like(builder.lower(builder.coalesce(
                                methods.get("label").as(String.class), "")), like)));
                query.distinct(true);
            }
            String normalizedGroup = normalize(group);
            if (normalizedGroup != null) {
                predicates.add(builder.equal(root.get("groupName"), normalizedGroup));
            }
            if (bookmarked != null) {
                predicates.add(builder.equal(root.get("bookmarked"), bookmarked));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
