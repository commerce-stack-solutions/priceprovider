package de.ebusyness.priceproviderservice.facade.publicprice.restentity;

import de.ebusyness.commons.web.rest.RestEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * REST entity for list of public prices.
 * Used for all-prices endpoints that return ranked list of matching prices.
 */
@Schema(description = "List of public prices ranked by matching priority")
public class PublicPriceListRestEntity extends RestEntity<Object, Object> {
    
    @Schema(description = "List of matching prices, ordered by rank (best match first)")
    private List<PublicPriceRestEntity> items = new ArrayList<>();
    
    public List<PublicPriceRestEntity> getItems() {
        return items;
    }
    
    public void setItems(List<PublicPriceRestEntity> items) {
        this.items = items;
    }
}
