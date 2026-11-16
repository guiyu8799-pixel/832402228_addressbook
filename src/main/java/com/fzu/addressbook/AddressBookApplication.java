package com.fzu.addressbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the address book application.
 *
 * <p>The Spring Boot application also serves the single page front end from
 * {@code src/main/resources/static}, so a single {@code java -jar} process exposes both the REST API
 * and the user interface.</p>
 */
@SpringBootApplication
public class AddressBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddressBookApplication.class, args);
    }
}
