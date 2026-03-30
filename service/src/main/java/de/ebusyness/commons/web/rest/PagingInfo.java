package de.ebusyness.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PagingInfo {

    @JsonProperty("page")
    private int page;

    @JsonProperty("page-size")
    private int pageSize;

    @JsonProperty("total-items")
    private long totalItems;

    @JsonProperty("total-pages")
    private int totalPages;

    public PagingInfo(int page, int pageSize, long totalItems, int totalPages) {
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}