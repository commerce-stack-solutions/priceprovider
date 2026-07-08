package io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class TaxClassListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<TaxClassRestEntity> items;

    public TaxClassListRestEntity(PagingInfo pagingInfo, Collection<TaxClassRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public TaxClassListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<TaxClassRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<TaxClassRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<TaxClassRestEntity> items) {
        this.items = items;
    }
}
