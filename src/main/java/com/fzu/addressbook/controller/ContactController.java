package com.fzu.addressbook.controller;

import com.fzu.addressbook.dto.BookmarkPayload;
import com.fzu.addressbook.dto.ContactPayload;
import com.fzu.addressbook.dto.ContactView;
import com.fzu.addressbook.dto.SummaryView;
import com.fzu.addressbook.service.ContactService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for contacts, bookmarks and the sidebar summary.
 */
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Lists contacts.
     *
     * @param q          optional keyword matched against name, company, title, group and methods
     * @param group      optional exact group name
     * @param bookmarked optional bookmark filter
     * @return matching contacts, bookmarked first
     */
    @GetMapping
    public List<ContactView> list(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "group", required = false) String group,
                                  @RequestParam(value = "bookmarked", required = false) Boolean bookmarked) {
        return contactService.list(q, group, bookmarked);
    }

    /**
     * Returns counters and the list of groups for the sidebar.
     *
     * @return summary of the address book
     */
    @GetMapping("/summary")
    public SummaryView summary() {
        return contactService.summary();
    }

    /**
     * Returns one contact.
     *
     * @param id contact id
     * @return the contact
     */
    @GetMapping("/{id}")
    public ContactView get(@PathVariable("id") Long id) {
        return contactService.get(id);
    }

    /**
     * Creates a contact.
     *
     * @param payload request body
     * @return the created contact
     */
    @PostMapping
    public ResponseEntity<ContactView> create(@Valid @RequestBody ContactPayload payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contactService.create(payload));
    }

    /**
     * Replaces the editable fields of a contact.
     *
     * @param id      contact id
     * @param payload request body
     * @return the updated contact
     */
    @PutMapping("/{id}")
    public ContactView update(@PathVariable("id") Long id,
                              @Valid @RequestBody ContactPayload payload) {
        return contactService.update(id, payload);
    }

    /**
     * Deletes a contact.
     *
     * @param id contact id
     * @return empty 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles the bookmark flag.
     *
     * @param id      contact id
     * @param payload the new flag
     * @return the updated contact
     */
    @PatchMapping("/{id}/bookmark")
    public ContactView setBookmarked(@PathVariable("id") Long id,
                                     @RequestBody BookmarkPayload payload) {
        return contactService.setBookmarked(id, payload.isBookmarked());
    }
}
