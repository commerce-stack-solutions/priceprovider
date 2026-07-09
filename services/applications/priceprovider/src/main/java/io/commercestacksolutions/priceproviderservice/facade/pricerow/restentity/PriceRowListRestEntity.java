package io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class PriceRowListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {

    private Collection<PriceRowRestEntity> items;

    public PriceRowListRestEntity(PagingInfo pagingInfo, Collection<PriceRowRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public PriceRowListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<PriceRowRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<PriceRowRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<PriceRowRestEntity> items) {
        this.items = items;
    }
}

