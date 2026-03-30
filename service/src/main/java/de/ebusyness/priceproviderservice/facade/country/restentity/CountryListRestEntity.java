package de.ebusyness.priceproviderservice.facade.country.restentity;

import de.ebusyness.commons.web.rest.*;

import java.util.Collection;

public class CountryListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {

    private Collection<CountryRestEntity> items;

    public CountryListRestEntity(PagingInfo pagingInfo, Collection<CountryRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public CountryListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<CountryRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<CountryRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<CountryRestEntity> items) {
        this.items = items;
    }
}
