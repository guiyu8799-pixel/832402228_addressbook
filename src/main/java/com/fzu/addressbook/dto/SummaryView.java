package com.fzu.addressbook.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Sidebar counters and the list of groups that actually contain contacts.
 */
public class SummaryView {

    private long total;
    private long bookmarked;
    private List<GroupCount> groups = new ArrayList<>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(long bookmarked) {
        this.bookmarked = bookmarked;
    }

    public List<GroupCount> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupCount> groups) {
        this.groups = groups;
    }

    /**
     * One row of the group filter.
     */
    public static class GroupCount {

        private String name;
        private long count;

        public GroupCount() {
        }

        public GroupCount(String name, long count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
