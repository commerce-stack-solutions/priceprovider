package io.commercestacksolutions.priceproviderservice.facade.currency.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class CurrencyListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<CurrencyRestEntity> items;

    public CurrencyListRestEntity(PagingInfo pagingInfo, Collection<CurrencyRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public CurrencyListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<CurrencyRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<CurrencyRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<CurrencyRestEntity> items) {
        this.items = items;
    }
}
