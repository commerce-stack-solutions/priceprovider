package de.ebusyness.priceproviderservice.facade.language.restentity;

import de.ebusyness.commons.web.rest.*;

import java.util.Collection;

public class LanguageListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<LanguageRestEntity> items;

    public LanguageListRestEntity(PagingInfo pagingInfo, Collection<LanguageRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public LanguageListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<LanguageRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<LanguageRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<LanguageRestEntity> items) {
        this.items = items;
    }
}
