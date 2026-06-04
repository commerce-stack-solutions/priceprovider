package io.commercestacksolutions.priceproviderservice.facade.pricerow.mapper;

import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.mapper.CurrencyRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper.TaxClassRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.unit.mapper.UnitRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.TaxCalculationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceRowRestEntityMapperTest {

    @Mock
    private UnitRestEntityMapper unitRestEntityMapper;
    @Mock
    private CurrencyRestEntityMapper currencyRestEntityMapper;
    @Mock
    private TaxClassRestEntityMapper taxClassRestEntityMapper;
    @Mock
    private TaxCalculationStrategy taxCalculationStrategy;

    private PriceRowRestEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PriceRowRestEntityMapper(unitRestEntityMapper, currencyRestEntityMapper, taxClassRestEntityMapper, taxCalculationStrategy);
    }

    @Test
    void convert_taxExcludedTaxation_calculatesTaxValueFromPercentageRate() throws DataMappingException {
        PriceRowEntity source = createPriceRow(new BigDecimal("109.99"), false, new BigDecimal("7.00"));
        when(taxCalculationStrategy.calculateTaxFromNet(new BigDecimal("109.99"), new BigDecimal("7.00")))
                .thenReturn(new BigDecimal("7.70"));

        PriceRowRestEntity target = mapper.convert(source, taxationContext());

        assertNotNull(target.getInfo());
        assertNotNull(target.getInfo().getTaxation());
        assertEquals(new BigDecimal("7.70"), target.getInfo().getTaxation().getTaxValue());
    }

    @Test
    void convert_taxIncludedTaxation_calculatesTaxPortionFromPercentageRate() throws DataMappingException {
        PriceRowEntity source = createPriceRow(new BigDecimal("107.00"), true, new BigDecimal("7.00"));
        when(taxCalculationStrategy.calculateTaxFromGross(new BigDecimal("107.00"), new BigDecimal("7.00")))
                .thenReturn(new BigDecimal("7.00"));

        PriceRowRestEntity target = mapper.convert(source, taxationContext());

        assertNotNull(target.getInfo());
        assertNotNull(target.getInfo().getTaxation());
        assertEquals(new BigDecimal("7.00"), target.getInfo().getTaxation().getTaxValue());
    }

    @Test
    void convert_taxIncludedTaxation_usesPreciseIntermediateNetValue() throws DataMappingException {
        PriceRowEntity source = createPriceRow(new BigDecimal("0.03"), true, new BigDecimal("20.00"));
        when(taxCalculationStrategy.calculateTaxFromGross(new BigDecimal("0.03"), new BigDecimal("20.00")))
                .thenReturn(new BigDecimal("0.01"));

        PriceRowRestEntity target = mapper.convert(source, taxationContext());

        assertNotNull(target.getInfo());
        assertNotNull(target.getInfo().getTaxation());
        assertEquals(new BigDecimal("0.01"), target.getInfo().getTaxation().getTaxValue());
    }

    private static PriceRowEntity createPriceRow(BigDecimal priceValue, boolean taxIncluded, BigDecimal taxRate) {
        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxRate(taxRate);

        PriceRowEntity source = new PriceRowEntity();
        source.setPriceValue(priceValue);
        source.setMinQuantity(BigDecimal.ONE);
        source.setTaxIncluded(taxIncluded);
        source.setTaxClass(taxClass);
        return source;
    }

    private static RestResponseMappingContext taxationContext() {
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(Set.of("$info.taxation"));
        return context;
    }
}
