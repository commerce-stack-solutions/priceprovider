package io.commercestacksolutions.priceproviderservice.facade.unit;

import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class UnitBulkDeleteIntegrationTest {

    @Autowired
    private UnitFacadeService unitFacade;

    @Autowired
    private UnitEntityRepository unitRepository;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    private CurrencyEntity testCurrency;
    private TaxClassEntity testTaxClass;

    @BeforeEach
    public void setup() {
        // Set up authentication context
        var authorities = AuthorityUtils.createAuthorityList(
            "priceprovider.admin:Unit:write",
            "priceprovider.admin:Unit:read",
            "priceprovider.admin:Unit:delete",
            "priceprovider.admin:PriceRow:write",
            "priceprovider.admin:Currency:write",
            "priceprovider.admin:TaxClass:write"
        );
        var auth = new UsernamePasswordAuthenticationToken("test-admin", "test", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Clean up test data
        priceRowRepository.findAll().stream()
            .filter(pr -> pr.getPricedResourceId() != null && pr.getPricedResourceId().startsWith("unit-test-"))
            .forEach(priceRowRepository::delete);
        
        unitRepository.findAll().stream()
            .filter(u -> u.getSymbol().contains("UNITTST"))
            .forEach(u -> {
                try {
                    unitRepository.delete(u);
                } catch (Exception e) {
                    // Ignore
                }
            });
        
        testCurrency = currencyRepository.findById("EUR").orElseGet(() -> {
            CurrencyEntity currency = new CurrencyEntity();
            currency.setCurrencyKey("EUR");
            currency.setSymbol("€");
            currency.setCreatedAt(OffsetDateTime.now());
            currency.setLastModifiedAt(OffsetDateTime.now());
            return currencyRepository.save(currency);
        });

        testTaxClass = taxClassRepository.findById("UNIT-TAX").orElseGet(() -> {
            TaxClassEntity tax = new TaxClassEntity();
            tax.setTaxClassId("UNIT-TAX");
            tax.setTaxRate(new BigDecimal("0.19"));
            tax.setCreatedAt(OffsetDateTime.now());
            tax.setLastModifiedAt(OffsetDateTime.now());
            return taxClassRepository.save(tax);
        });
    }

    @Test
    public void testBulkDeleteUnits_PartialDelete_DeletesUnreferencedOnly() {
        // Create test units
        UnitEntity kg = createUnit("UNITTST1", "Kilogram");
        UnitEntity g = createUnit("UNITTST2", "Gram");
        UnitEntity mg = createUnit("UNITTST3", "Milligram");

        // Create price row that references only one unit
        createPriceRow("unit-test-1", "UNITTST1");

        // Try to bulk delete all units
        List<String> symbols = Arrays.asList("UNITTST1", "UNITTST2", "UNITTST3");
        
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            unitFacade.bulkDeleteUnits(symbols);
        });

        assertNotNull(exception);

        // UNITTST2 and UNITTST3 should be deleted (not referenced) - PARTIAL DELETE
        assertFalse(unitRepository.existsById("UNITTST2"));
        assertFalse(unitRepository.existsById("UNITTST3"));

        // UNITTST1 should still exist (it is referenced)
        assertTrue(unitRepository.existsById("UNITTST1"));
    }

    private UnitEntity createUnit(String symbol, String name) {
        UnitEntity unit = new UnitEntity();
        unit.setSymbol(symbol);
        unit.setFactor(new BigDecimal("1.0"));
        unit.setCreatedAt(OffsetDateTime.now());
        unit.setLastModifiedAt(OffsetDateTime.now());
        return unitRepository.save(unit);
    }

    private PriceRowEntity createPriceRow(String resourceId, String unitSymbol) {
        UnitEntity unit = unitRepository.findById(unitSymbol)
            .orElseThrow(() -> new RuntimeException("Unit not found: " + unitSymbol));
        
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId(resourceId);
        priceRow.setPriceValue(new BigDecimal("100.00"));
        priceRow.setMinQuantity(new BigDecimal("1.0"));
        priceRow.setUnit(unit);
        priceRow.setCurrency(testCurrency);
        priceRow.setTaxClass(testTaxClass);
        priceRow.setPriceType(new PriceType("SALES_PRICE"));
        priceRow.setTaxIncluded(false);
        priceRow.setCreatedAt(OffsetDateTime.now());
        priceRow.setLastModifiedAt(OffsetDateTime.now());
        return priceRowRepository.save(priceRow);
    }
}
