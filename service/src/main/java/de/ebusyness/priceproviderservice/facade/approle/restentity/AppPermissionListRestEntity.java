package de.ebusyness.priceproviderservice.facade.approle.restentity;

import de.ebusyness.commons.web.rest.*;

import java.util.Collection;

public class AppPermissionListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {
    private Collection<AppPermissionRestEntity> items;

    public AppPermissionListRestEntity(PagingInfo pagingInfo, Collection<AppPermissionRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public AppPermissionListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<AppPermissionRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<AppPermissionRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<AppPermissionRestEntity> items) {
        this.items = items;
    }
}
