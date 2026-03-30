package de.ebusyness.priceproviderservice.facade.publicprice;

import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.facade.publicprice.restentity.PublicPriceListRestEntity;
import de.ebusyness.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;

import java.math.BigDecimal;
import java.util.Set;

public interface PublicPriceFacade {
    PublicPriceRestEntity getBestPrice(
            String channelId,
            String countryKey,
            String groupId,
            String pricedResourceId,
            BigDecimal quantity,
            String unitRef,
            String currencyRef,
            PriceType priceType,
            Set<String> expand
    ) throws NotFoundException, DataMappingException;

    PublicPriceListRestEntity getAllPrices(
            String channelId,
            String countryKey,
            String groupId,
            String pricedResourceId,
            BigDecimal quantity,
            String unitRef,
            String currencyRef,
            PriceType priceType,
            Set<String> expand
    ) throws DataMappingException, NotFoundException;

    PublicPriceListRestEntity getBestPrices(
            String channelId,
            String countryKey,
            String groupId,
            Set<String> pricedResourceIds,
            BigDecimal quantity,
            String unitRef,
            String currencyRef,
            PriceType priceType,
            Set<String> expand
    ) throws DataMappingException, NotFoundException;

    PublicPriceListRestEntity getAllQuantityBestPrices(
            String channelId,
            String countryKey,
            String groupId,
            String pricedResourceId,
            String unitRef,
            String currencyRef,
            PriceType priceType,
            Set<String> expand
    ) throws DataMappingException, NotFoundException;
}