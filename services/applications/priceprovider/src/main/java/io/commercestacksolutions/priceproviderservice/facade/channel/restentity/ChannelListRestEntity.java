package io.commercestacksolutions.priceproviderservice.facade.channel.restentity;

import io.commercestacksolutions.commons.web.rest.*;

import java.util.Collection;

public class ChannelListRestEntity extends RestEntity<InfoListResult, IncludesListResult> {

    private Collection<ChannelRestEntity> items;

    public ChannelListRestEntity(PagingInfo pagingInfo, Collection<ChannelRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo));
        this.items = items;
    }

    public ChannelListRestEntity(PagingInfo pagingInfo, SortingInfo sortingInfo, Collection<ChannelRestEntity> items) {
        this.setInfo(new InfoListResult(pagingInfo, sortingInfo));
        this.items = items;
    }

    public Collection<ChannelRestEntity> getItems() {
        return items;
    }

    public void setItems(Collection<ChannelRestEntity> items) {
        this.items = items;
    }
}
