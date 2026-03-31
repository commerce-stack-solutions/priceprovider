package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for entity reference filtering.
 * This test specifically validates the fix for filtering by entity references (e.g., currencyRef:EUR).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EntityReferenceFilterIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private UnitEntityRepository unitRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    private final QueryParser parser = new QueryParser();

    private CurrencyEntity eur;
    private CurrencyEntity usd;
    private UnitEntity meter;
    private UnitEntity centimeter;

    @BeforeEach
    public void setup() {
        // Delete in correct order (foreign key constraints)
        priceRowRepository.deleteAll();
        priceRowRepository.flush();
        taxClassRepository.deleteAll();
        taxClassRepository.flush();
        unitRepository.deleteAll();
        unitRepository.flush();
        currencyRepository.deleteAll();
        currencyRepository.flush();

        // Setup currencies - use findById to check if already exists, otherwise create
        eur = currencyRepository.findById("EUR").orElseGet(() -> {
            CurrencyEntity newEur = new CurrencyEntity("EUR");
            newEur.setSymbol("€");
            Map<String, String> eurNames = new HashMap<>();
            eurNames.put("en", "Euro");
            eurNames.put("de", "Euro");
            newEur.setName(eurNames);
            return currencyRepository.saveAndFlush(newEur);
        });

        usd = currencyRepository.findById("USD").orElseGet(() -> {
            CurrencyEntity newUsd = new CurrencyEntity("USD");
            newUsd.setSymbol("$");
            Map<String, String> usdNames = new HashMap<>();
            usdNames.put("en", "US Dollar");
            usdNames.put("de", "US-Dollar");
            newUsd.setName(usdNames);
            return currencyRepository.saveAndFlush(newUsd);
        });

        // Setup units
        meter = new UnitEntity("m");
        Map<String, String> meterNames = new HashMap<>();
        meterNames.put("en", "Meter");
        meterNames.put("de", "Meter");
        meter.setName(meterNames);
        meter.setMeasure("length");
        meter = unitRepository.saveAndFlush(meter);

        centimeter = new UnitEntity("cm");
        Map<String, String> cmNames = new HashMap<>();
        cmNames.put("en", "Centimeter");
        cmNames.put("de", "Zentimeter");
        centimeter.setName(cmNames);
        centimeter.setMeasure("length");
        centimeter.setBaseUnit(meter);
        centimeter.setFactor(new BigDecimal("0.01"));
        centimeter = unitRepository.saveAndFlush(centimeter);

        // Setup tax class
        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxClassId("TAX19");
        taxClass.setTaxRate(new BigDecimal("0.19"));
        taxClass = taxClassRepository.saveAndFlush(taxClass);

        // Setup price rows
        PriceRowEntity priceRow1 = new PriceRowEntity();
        priceRow1.setPricedResourceId("PROD_EUR_1");
        priceRow1.setPriceValue(new BigDecimal("100.00"));
        priceRow1.setMinQuantity(BigDecimal.ONE);
        priceRow1.setUnit(meter);
        priceRow1.setCurrency(eur);
        priceRow1.setTaxClass(taxClass);
        priceRow1.setPriceType(PriceType.SALES_PRICE);
        priceRow1.setTaxIncluded(true);
        priceRowRepository.saveAndFlush(priceRow1);

        PriceRowEntity priceRow2 = new PriceRowEntity();
        priceRow2.setPricedResourceId("PROD_EUR_2");
        priceRow2.setPriceValue(new BigDecimal("200.00"));
        priceRow2.setMinQuantity(BigDecimal.ONE);
        priceRow2.setUnit(meter);
        priceRow2.setCurrency(eur);
        priceRow2.setTaxClass(taxClass);
        priceRow2.setPriceType(PriceType.SALES_PRICE);
        priceRow2.setTaxIncluded(true);
        priceRowRepository.saveAndFlush(priceRow2);

        PriceRowEntity priceRow3 = new PriceRowEntity();
        priceRow3.setPricedResourceId("PROD_USD_1");
        priceRow3.setPriceValue(new BigDecimal("120.00"));
        priceRow3.setMinQuantity(BigDecimal.ONE);
        priceRow3.setUnit(meter);
        priceRow3.setCurrency(usd);
        priceRow3.setTaxClass(taxClass);
        priceRow3.setPriceType(PriceType.SALES_PRICE);
        priceRow3.setTaxIncluded(false);
        priceRowRepository.saveAndFlush(priceRow3);
    }

    @Test
    public void testFilterByCurrencyEntityReference_EUR() throws Exception {
        // THE MAIN BUG FIX TEST: currencyRef:EUR should filter by currency.currencyKey = 'EUR'
        QueryExpression expr = parser.parse("currencyRef:EUR");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(pr -> "EUR".equals(pr.getCurrency().getCurrencyKey())));
    }

    @Test
    public void testFilterByCurrencyEntityReference_USD() throws Exception {
        QueryExpression expr = parser.parse("currencyRef:USD");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(1, results.size());
        assertTrue(results.stream().allMatch(pr -> "USD".equals(pr.getCurrency().getCurrencyKey())));
    }

    @Test
    public void testFilterByUnitEntityReference() throws Exception {
        QueryExpression expr = parser.parse("unitRef:m");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(pr -> "m".equals(pr.getUnit().getSymbol())));
    }

    @Test
    public void testFilterByNestedEntityReference() throws Exception {
        // Test filtering by baseUnit on Unit entity
        QueryExpression expr = parser.parse("baseUnitRef:m");
        Specification<UnitEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UnitEntity> query = cb.createQuery(UnitEntity.class);
        Root<UnitEntity> root = query.from(UnitEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<UnitEntity> typedQuery = entityManager.createQuery(query);
        List<UnitEntity> results = typedQuery.getResultList();

        assertEquals(1, results.size());
        assertEquals("cm", results.get(0).getSymbol());
        assertEquals("m", results.get(0).getBaseUnit().getSymbol());
    }

    @Test
    public void testFilterByNestedEntityReferenceWithPath() throws Exception {
        // Test filtering using nested path: unit.symbol:m
        QueryExpression expr = parser.parse("unitRef.symbol:m");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(pr -> "m".equals(pr.getUnit().getSymbol())));
    }

    @Test
    public void testComplexFilterWithEntityReferences() throws Exception {
        // Complex filter: currencyRef:EUR AND unitRef:m
        QueryExpression expr = parser.parse("currencyRef:EUR AND unitRef:m");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(pr -> "EUR".equals(pr.getCurrency().getCurrencyKey())));
        assertTrue(results.stream().allMatch(pr -> "m".equals(pr.getUnit().getSymbol())));
    }

    @Test
    public void testFilterByTaxClassEntityReference() throws Exception {
        QueryExpression expr = parser.parse("taxClassRef:TAX19");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(pr -> "TAX19".equals(pr.getTaxClass().getTaxClassId())));
    }

    @Test
    public void testFilterByCurrencyWithExplicitPath() throws Exception {
        // Filter by ID field with explicit path (should work same as currencyRef:USD)
        QueryExpression expr = parser.parse("currencyRef.currencyKey:USD");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        assertEquals(1, results.size());
        assertTrue(results.stream().allMatch(pr -> "USD".equals(pr.getCurrency().getCurrencyKey())));
    }

    @Test
    public void testInvalidEntityReferenceThrowsException() throws Exception {
        // Test that an invalid entity reference still throws QueryFilterRuntimeException
        QueryExpression expr = parser.parse("currencyRef:INVALID_CURRENCY_CODE");
        Specification<PriceRowEntity> spec = SpecificationBuilder.build(expr);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PriceRowEntity> query = cb.createQuery(PriceRowEntity.class);
        Root<PriceRowEntity> root = query.from(PriceRowEntity.class);
        query.where(spec.toPredicate(root, query, cb));

        TypedQuery<PriceRowEntity> typedQuery = entityManager.createQuery(query);
        List<PriceRowEntity> results = typedQuery.getResultList();

        // Should return empty result, not throw exception
        assertEquals(0, results.size());
    }
}

