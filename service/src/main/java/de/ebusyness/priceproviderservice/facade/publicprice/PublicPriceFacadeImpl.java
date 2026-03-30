package de.ebusyness.priceproviderservice.facade.publicprice;

import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.facade.publicprice.mapper.PublicPriceMapper;
import de.ebusyness.priceproviderservice.facade.publicprice.restentity.PublicPriceListRestEntity;
import de.ebusyness.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;
import de.ebusyness.priceproviderservice.service.channel.ChannelService;
import de.ebusyness.priceproviderservice.service.country.CountryService;
import de.ebusyness.priceproviderservice.service.publicprice.PublicPriceService;
import de.ebusyness.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import de.ebusyness.priceproviderservice.service.publicprice.strategy.PriceRepresentationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PublicPriceFacadeImpl implements PublicPriceFacade {

    private final PublicPriceService publicPriceService;
    private final PublicPriceMapper publicPriceMapper;
    private final ChannelCountryGuard channelCountryGuard;
    private final ChannelService channelService;
    private final CountryService countryService;
    private final ApplicationContext applicationContext;

    @Autowired
    public PublicPriceFacadeImpl(
            PublicPriceService publicPriceService,
            PublicPriceMapper publicPriceMapper,
            ChannelCountryGuard channelCountryGuard,
            ChannelService channelService,
            CountryService countryService,
            ApplicationContext applicationContext) {
        this.publicPriceService = publicPriceService;
        this.publicPriceMapper = publicPriceMapper;
        this.channelCountryGuard = channelCountryGuard;
        this.channelService = channelService;
        this.countryService = countryService;
        this.applicationContext = applicationContext;
    }

    private PriceRepresentationMode resolvePriceRepresentationMode(String channelId) {
        ChannelEntity channel = channelService.getChannel(channelId);
        if (channel != null
                && channel.getPriceRepresentationMode() != null
                && !channel.getPriceRepresentationMode().isBlank()) {
            try {
                return applicationContext.getBean(
                        channel.getPriceRepresentationMode(), PriceRepresentationMode.class);
            } catch (Exception ignored) {
            }
        }
        return applicationContext.getBean("FORCE_GROSS", PriceRepresentationMode.class);
    }

    /**
     * Resolves the currency to use for the price query.
     * If currency is provided, returns it as-is.
     * If currency is null or blank, returns the country's primary currency.
     *
     * @param currencyRef the currency provided by the caller (may be null)
     * @param countryKey the country ISO key
     * @return the resolved currency key
     * @throws NotFoundException if currency is not provided and country has no primary currency
     */
    private String resolveCurrency(String currencyRef, String countryKey) throws NotFoundException {
        if (currencyRef != null && !currencyRef.isBlank()) {
            return currencyRef;
        }

        // Currency not provided - use country's primary currency
        CountryEntity country = countryService.getCountry(countryKey);
        if (country == null) {
            throw new NotFoundException("Country not found: " + countryKey);
        }

        if (country.getPrimaryCurrencyRef() == null || country.getPrimaryCurrencyRef().getCurrencyKey() == null) {
            throw new NotFoundException(
                    "No currency specified and country '" + countryKey + "' has no primary currency defined.");
        }

        return country.getPrimaryCurrencyRef().getCurrencyKey();
    }

    private PriceMatchingCriteria buildCriteria(
            String channelId, String countryKey, String groupId, String pricedResourceId,
            BigDecimal quantity, String unitRef, String currencyRef,
            PriceType priceType, PriceRepresentationMode mode
    ) {
        PriceMatchingCriteria criteria = new PriceMatchingCriteria();
        criteria.setPricedResourceId(pricedResourceId);
        criteria.setQuantity(quantity);
        criteria.setUnitRef(unitRef);
        criteria.setCurrencyRef(currencyRef);
        criteria.setPriceType(priceType);
        criteria.setGroupId(groupId);
        criteria.setChannelId(channelId);
        criteria.setCountryKey(countryKey);
        criteria.setTaxationMode(mode.getTaxationMode());
        criteria.setTaxIncludedFilter(mode.getTaxIncludedFilter());
        return criteria;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPriceRestEntity getBestPrice(
            String channelId, String countryKey, String groupId, String pricedResourceId,
            BigDecimal quantity, String unitRef, String currencyRef,
            PriceType priceType, Set<String> expand)
            throws NotFoundException, DataMappingException {
        channelCountryGuard.assertCountryAllowedInChannel(channelId, countryKey);
        String resolvedCurrency = resolveCurrency(currencyRef, countryKey);
        PriceRepresentationMode mode = resolvePriceRepresentationMode(channelId);
        PriceMatchingCriteria criteria = buildCriteria(
                channelId, countryKey, groupId, pricedResourceId, quantity, unitRef, resolvedCurrency, priceType, mode);
        PriceRowEntity bestPrice = publicPriceService.findBestPrice(criteria);
        if (bestPrice == null) throw new NotFoundException("No matching price found");
        return mapToRestEntity(bestPrice, criteria.getTaxationMode(), expand);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPriceListRestEntity getAllPrices(
            String channelId, String countryKey, String groupId, String pricedResourceId,
            BigDecimal quantity, String unitRef, String currencyRef,
            PriceType priceType, Set<String> expand)
            throws DataMappingException, NotFoundException {

        channelCountryGuard.assertCountryAllowedInChannel(channelId, countryKey);

        String resolvedCurrency = resolveCurrency(currencyRef, countryKey);
        PriceRepresentationMode mode = resolvePriceRepresentationMode(channelId);
        PriceMatchingCriteria criteria = buildCriteria(
                channelId, countryKey, groupId, pricedResourceId, quantity, unitRef, resolvedCurrency, priceType, mode);
        return mapToListRestEntity(publicPriceService.findAllPrices(criteria), criteria.getTaxationMode(), expand);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPriceListRestEntity getAllQuantityBestPrices(
            String channelId, String countryKey, String groupId, String pricedResourceId,
            String unitRef, String currencyRef,
            PriceType priceType, Set<String> expand)
            throws DataMappingException, NotFoundException {

        channelCountryGuard.assertCountryAllowedInChannel(channelId, countryKey);

        String resolvedCurrency = resolveCurrency(currencyRef, countryKey);
        PriceRepresentationMode mode = resolvePriceRepresentationMode(channelId);
        PriceMatchingCriteria criteria = buildCriteria(
                channelId, countryKey, groupId, pricedResourceId, null, unitRef, resolvedCurrency, priceType, mode);
        return mapToListRestEntity(publicPriceService.findAllQuantityBestPrices(criteria), criteria.getTaxationMode(), expand);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPriceListRestEntity getBestPrices(
            String channelId, String countryKey, String groupId, Set<String> pricedResourceIds,
            BigDecimal quantity, String unitRef, String currencyRef,
            PriceType priceType, Set<String> expand)
            throws DataMappingException, NotFoundException {

        channelCountryGuard.assertCountryAllowedInChannel(channelId, countryKey);

        String resolvedCurrency = resolveCurrency(currencyRef, countryKey);
        PriceRepresentationMode mode = resolvePriceRepresentationMode(channelId);
        List<PriceRowEntity> bestPrices = pricedResourceIds.stream()
                .map(id -> {
                    PriceMatchingCriteria criteria = buildCriteria(
                            channelId, countryKey, groupId, id, quantity, unitRef, resolvedCurrency, priceType, mode);
                    return publicPriceService.findBestPrice(criteria);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        PriceMatchingCriteria baseCriteria = buildCriteria(channelId, countryKey, groupId, null, quantity, unitRef, resolvedCurrency, priceType, mode);
        return mapToListRestEntity(bestPrices, baseCriteria.getTaxationMode(), expand);
    }

    private PublicPriceListRestEntity emptyPriceList() {
        PublicPriceListRestEntity result = new PublicPriceListRestEntity();
        result.setItems(List.of());
        return result;
    }

    private PublicPriceRestEntity mapToRestEntity(PriceRowEntity priceRow, PriceMatchingCriteria.TaxationMode taxationMode, Set<String> expand) throws DataMappingException {
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        context.setProperty("taxationMode", taxationMode);
        return publicPriceMapper.convert(priceRow, context);
    }

    private PublicPriceListRestEntity mapToListRestEntity(List<PriceRowEntity> priceRows, PriceMatchingCriteria.TaxationMode taxationMode, Set<String> expand) throws DataMappingException {
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);
        context.setProperty("taxationMode", taxationMode);
        List<PublicPriceRestEntity> items = new java.util.ArrayList<>();
        for (PriceRowEntity pr : priceRows) {
            items.add(publicPriceMapper.convert(pr, context));
        }
        PublicPriceListRestEntity result = new PublicPriceListRestEntity();
        result.setItems(items);
        return result;
    }
}