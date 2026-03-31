package io.commercestacksolutions.commons.web.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoListResult {

    @JsonProperty("paging")
    private PagingInfo pagingInfo;

    @JsonProperty("sorting")
    private SortingInfo sortingInfo;

    public InfoListResult(PagingInfo pagingInfo) {
        this.pagingInfo = pagingInfo;
    }

    public InfoListResult(PagingInfo pagingInfo, SortingInfo sortingInfo) {
        this.pagingInfo = pagingInfo;
        this.sortingInfo = sortingInfo;
    }

    public PagingInfo getPagingInfo() {
        return pagingInfo;
    }

    public void setPagingInfo(PagingInfo pagingInfo) {
        this.pagingInfo = pagingInfo;
    }

    public SortingInfo getSortingInfo() {
        return sortingInfo;
    }

    public void setSortingInfo(SortingInfo sortingInfo) {
        this.sortingInfo = sortingInfo;
    }
}
