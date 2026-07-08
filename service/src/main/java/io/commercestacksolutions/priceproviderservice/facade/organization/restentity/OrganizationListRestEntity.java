package io.commercestacksolutions.priceproviderservice.facade.organization.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class OrganizationListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<OrganizationRestEntity> items;

    public OrganizationListRestEntity(PagingInfo pagingInfo, Collection<OrganizationRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public OrganizationListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<OrganizationRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<OrganizationRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<OrganizationRestEntity> items) {
        this.items = items;
    }
}
