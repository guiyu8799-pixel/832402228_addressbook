package com.fzu.addressbook.repository;

import com.fzu.addressbook.entity.Contact;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/**
 * Data access for {@link Contact}.
 *
 * <p>Filtering is expressed with {@link org.springframework.data.jpa.domain.Specification}s in the
 * service layer, because every filter (keyword, group, bookmark) is optional. Building predicates
 * conditionally avoids the fragile {@code :param is null} style JPQL that SQLite handles poorly.</p>
 */
public interface ContactRepository extends JpaRepository<Contact, Long>,
        JpaSpecificationExecutor<Contact> {

    /**
     * Loads one contact together with its methods.
     *
     * @param id contact id
     * @return the contact when it exists
     */
    @Query("select distinct c from Contact c left join fetch c.methods where c.id = :id")
    Optional<Contact> findWithMethodsById(Long id);

    /**
     * Counts the contacts that are currently bookmarked.
     *
     * @return number of favourite contacts
     */
    long countByBookmarkedTrue();
}
