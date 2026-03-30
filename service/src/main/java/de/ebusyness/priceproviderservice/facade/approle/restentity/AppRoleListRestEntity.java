package de.ebusyness.priceproviderservice.facade.approle.restentity;

import de.ebusyness.commons.web.rest.*;

import java.util.Collection;

public class AppRoleListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<AppRoleRestEntity> items;

    public AppRoleListRestEntity(PagingInfo pagingInfo, Collection<AppRoleRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public AppRoleListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<AppRoleRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<AppRoleRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<AppRoleRestEntity> items) {
        this.items = items;
    }
}
