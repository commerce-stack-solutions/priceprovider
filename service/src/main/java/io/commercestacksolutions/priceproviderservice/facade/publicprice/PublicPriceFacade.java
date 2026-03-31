package io.commercestacksolutions.priceproviderservice.facade.publicprice;

import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;

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