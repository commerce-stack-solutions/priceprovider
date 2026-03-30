package de.ebusyness.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SortingInfo {

    @JsonProperty("sort-by")
    private List<String> sortBy;

    @JsonProperty("sort-direction")
    private String sortDirection;

    public SortingInfo(List<String> sortBy, String sortDirection) {
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }

    public List<String> getSortBy() {
        return sortBy;
    }

    public void setSortBy(List<String> sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
