package io.commercestacksolutions.priceproviderservice.facade.unit.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class UnitListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<UnitRestEntity> items;

    public UnitListRestEntity(PagingInfo pagingInfo, Collection<UnitRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public UnitListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<UnitRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<UnitRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<UnitRestEntity> items) {
        this.items = items;
    }
}