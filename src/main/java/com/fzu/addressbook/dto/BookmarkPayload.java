package com.fzu.addressbook.dto;

/**
 * Request body of the bookmark toggle endpoint.
 */
public class BookmarkPayload {

    private boolean bookmarked;

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }
}
