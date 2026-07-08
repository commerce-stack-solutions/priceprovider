package io.commercestacksolutions.priceproviderservice.facade.group.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class GroupListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<GroupRestEntity> items;

    public GroupListRestEntity(PagingInfo pagingInfo, Collection<GroupRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public GroupListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<GroupRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<GroupRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<GroupRestEntity> items) {
        this.items = items;
    }
}
