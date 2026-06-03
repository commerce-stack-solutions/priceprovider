package io.commercestacksolutions.priceproviderservice.service.pricerow;

import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for before/after permission checks on PriceRow operations.
 *
 * Tests that write and delete operations check permissions against both:
 * - The existing object in the database (before state)
 * - The changed object (after state)
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@Transactional
public class PriceRowBeforeAfterPermissionIntegrationTest {

    @Autowired
    private PriceRowService priceRowService;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private UnitEntityRepository unitRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @Autowired
    private EntityManager entityManager;

    private CurrencyEntity eur;
    private CurrencyEntity usd;
    private UnitEntity piece;
    private TaxClassEntity taxClass;

    @BeforeEach
    public void setup() {
        // Setup mock HTTP request context for API context resolution
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/public/api/pricerows");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Enable bootstrap mode to allow initial data setup
        AuthorizationContext.enableBootstrapMode();

        // Setup test currencies
        eur = currencyRepository.findById("EUR").orElseGet(() -> {
            CurrencyEntity currency = new CurrencyEntity("EUR");
            currency.setSymbol("€");
            Map<String, String> names = new HashMap<>();
            names.put("en", "Euro");
            currency.setName(names);
            return currencyRepository.saveAndFlush(currency);
        });

        usd = currencyRepository.findById("USD").orElseGet(() -> {
            CurrencyEntity currency = new CurrencyEntity("USD");
            currency.setSymbol("$");
            Map<String, String> names = new HashMap<>();
            names.put("en", "US Dollar");
            currency.setName(names);
            return currencyRepository.saveAndFlush(currency);
        });

        // Setup test unit
        piece = unitRepository.findById("piece").orElseGet(() -> {
            UnitEntity unit = new UnitEntity("piece");
            Map<String, String> names = new HashMap<>();
            names.put("en", "Piece");
            unit.setName(names);
            return unitRepository.saveAndFlush(unit);
        });

        // Setup test tax class
        taxClass = taxClassRepository.findById("STANDARD").orElseGet(() -> {
            TaxClassEntity tc = new TaxClassEntity();
            tc.setTaxClassId("STANDARD");
            tc.setTaxRate(new BigDecimal("0.19"));
            return taxClassRepository.saveAndFlush(tc);
        });

        // Disable bootstrap mode after setup
        AuthorizationContext.disableBootstrapMode();
    }

    @AfterEach
    public void cleanup() {
        // Always disable bootstrap mode after each test
        AuthorizationContext.disableBootstrapMode();
        // Clear security context
        SecurityContextHolder.clearContext();
        // Clear request context
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testCreateWithPermission_Success() throws EntityValidationException {
        // Setup: User with permission to write EUR prices
        setPermissions("priceprovider.public:PriceRow[currencyRef=='EUR']:write");

        // Create a price row with EUR currency
        PriceRowEntity priceRow = createPriceRow(eur);

        // Execute: Save should succeed
        PriceRowEntity saved = priceRowService.save(priceRow);

        // Verify: Price row created successfully
        assertNotNull(saved.getId());
        assertEquals(eur.getCurrencyKey(), saved.getCurrency().getCurrencyKey());
    }

    @Test
    public void testCreateWithoutPermission_Failure() {
        // Setup: User with permission for EUR prices only
        setPermissions("priceprovider.public:PriceRow[currencyRef=='EUR']:write");

        // Create a price row with USD currency (no permission)
        PriceRowEntity priceRow = createPriceRow(usd);

        // Execute & Verify: Save should fail on after state check
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            priceRowService.save(priceRow);
        });
        assertTrue(exception.getMessage().contains("no permission for new state"));
    }

    @Test
    public void testUpdateWithPermissionBeforeAndAfter_Success() throws EntityValidationException {
        // Setup: Create a EUR price row in bootstrap mode
        AuthorizationContext.enableBootstrapMode();
        PriceRowEntity priceRow = createPriceRow(eur);
        PriceRowEntity saved = priceRowRepository.saveAndFlush(priceRow);
        AuthorizationContext.disableBootstrapMode();

        // Setup: User with permission for EUR prices
        setPermissions("priceprovider.public:PriceRow[currencyRef=='EUR']:write");

        // Modify the price value (currency stays EUR)
        saved.setPriceValue(new BigDecimal("150.00"));

        // Execute: Update should succeed (both before and after are EUR)
        PriceRowEntity updated = priceRowService.save(saved);

        // Verify: Price row updated successfully
        assertEquals(new BigDecimal("150.00"), updated.getPriceValue());
        assertEquals(eur.getCurrencyKey(), updated.getCurrency().getCurrencyKey());
    }

    @Test
    public void testUpdateChangingCurrency_FailureOnAfterState() {
        // Setup: Create a EUR price row in bootstrap mode
        AuthorizationContext.enableBootstrapMode();
        PriceRowEntity priceRow = createPriceRow(eur);
        PriceRowEntity saved = priceRowRepository.saveAndFlush(priceRow);
        String savedId = saved.getId();
        AuthorizationContext.disableBootstrapMode();

        PriceRowEntity reloaded = new PriceRowEntity();
        reloaded.setId(savedId);
        reloaded.setPricedResourceId(saved.getPricedResourceId());
        reloaded.setPriceValue(saved.getPriceValue());
        reloaded.setMinQuantity(saved.getMinQuantity());
        reloaded.setUnit(saved.getUnit());
        reloaded.setCurrency(saved.getCurrency());
        reloaded.setTaxClass(saved.getTaxClass());
        reloaded.setPriceType(saved.getPriceType());
        reloaded.setTaxIncluded(saved.isTaxIncluded());

        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();

        // Setup: User with permission for EUR prices only
        setPermissions("priceprovider.public:PriceRow[currencyRef=='EUR']:write");

        // Change currency from EUR to USD
        reloaded.setCurrency(usd);

        // Execute & Verify: Update should fail on after state check (no permission for USD)
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            priceRowService.save(reloaded);
        });
        assertTrue(exception.getMessage().contains("no permission for new state"));
    }

    @Test
    public void testUpdateChangingCurrency_FailureOnBeforeState() {
        // Setup: Create a EUR price row in bootstrap mode
        AuthorizationContext.enableBootstrapMode();
        PriceRowEntity priceRow = createPriceRow(eur);
        PriceRowEntity saved = priceRowRepository.saveAndFlush(priceRow);
        String savedId = saved.getId();
        AuthorizationContext.disableBootstrapMode();

        PriceRowEntity reloaded = new PriceRowEntity();
        reloaded.setId(savedId);
        reloaded.setPricedResourceId(saved.getPricedResourceId());
        reloaded.setPriceValue(saved.getPriceValue());
        reloaded.setMinQuantity(saved.getMinQuantity());
        reloaded.setUnit(saved.getUnit());
        reloaded.setCurrency(saved.getCurrency());
        reloaded.setTaxClass(saved.getTaxClass());
        reloaded.setPriceType(saved.getPriceType());
        reloaded.setTaxIncluded(saved.isTaxIncluded());

        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();

        // Setup: User with permission for USD prices only (no EUR permission)
        setPermissions("priceprovider.public:PriceRow[currencyRef=='USD']:write");


        // Change currency from EUR to USD
        reloaded.setCurrency(usd);

        // Execute & Verify: Update should fail on before state check (no permission for EUR)
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            priceRowService.save(reloaded);
        });
        assertTrue(exception.getMessage().contains("no permission for existing state"));
    }

    @Test
    public void testUpdateChangingCurrency_SuccessWithBothPermissions() throws EntityValidationException {
        // Setup: Create a EUR price row in bootstrap mode
        AuthorizationContext.enableBootstrapMode();
        PriceRowEntity priceRow = createPriceRow(eur);
        PriceRowEntity saved = priceRowRepository.saveAndFlush(priceRow);
        AuthorizationContext.disableBootstrapMode();

        // Setup: User with permissions for BOTH EUR and USD prices
        setPermissions(
            "priceprovider.public:PriceRow[currencyRef=='EUR']:write",
            "priceprovider.public:PriceRow[currencyRef=='USD']:write"
        );

        // Change currency from EUR to USD
        saved.setCurrency(usd);

        // Execute: Update should succeed (permissions for both before and after states)
        PriceRowEntity updated = priceRowService.save(saved);

        // Verify: Currency changed successfully
        assertEquals(usd.getCurrencyKey(), updated.getCurrency().getCurrencyKey());
    }

    @Test
    public void testDeleteWithPermission_Success() {
        // Setup: Create a EUR price row in bootstrap mode
        AuthorizationContext.enableBootstrapMode();
        PriceRowEntity priceRow = createPriceRow(eur);
        PriceRowEntity saved = priceRowRepository.saveAndFlush(priceRow);
        String savedId = saved.getId();
        AuthorizationContext.disableBootstrapMode();

        // Setup: User with permission to delete EUR prices
        setPermissions("priceprovider.public:PriceRow[currencyRef=='EUR']:delete");

        // Execute: Delete should succeed
        priceRowService.deleteById(savedId);

        // Verify: Price row deleted
        Optional<PriceRowEntity> deleted = priceRowRepository.findById(savedId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testDeleteWithoutPermission_Failure() {
        // Setup: Create a EUR price row in bootstrap mode
        AuthorizationContext.enableBootstrapMode();
        PriceRowEntity priceRow = createPriceRow(eur);
        PriceRowEntity saved = priceRowRepository.saveAndFlush(priceRow);
        String savedId = saved.getId();
        AuthorizationContext.disableBootstrapMode();

        // Setup: User with permission for USD prices only (no EUR permission)
        setPermissions("priceprovider.public:PriceRow[currencyRef=='USD']:delete");

        // Execute & Verify: Delete should fail on before state check
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            priceRowService.deleteById(savedId);
        });
        assertTrue(exception.getMessage().contains("no permission for existing state"));

        // Verify: Price row still exists
        Optional<PriceRowEntity> stillExists = priceRowRepository.findById(savedId);
        assertTrue(stillExists.isPresent());
    }

    /**
     * Helper method to create a test price row with the given currency.
     */
    private PriceRowEntity createPriceRow(CurrencyEntity currency) {
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId("TEST-PRODUCT-" + System.currentTimeMillis());
        priceRow.setPriceValue(new BigDecimal("100.00"));
        priceRow.setMinQuantity(BigDecimal.ONE);
        priceRow.setUnit(piece);
        priceRow.setCurrency(currency);
        priceRow.setTaxClass(taxClass);
        priceRow.setPriceType(PriceType.SALES_PRICE);
        priceRow.setTaxIncluded(true);
        return priceRow;
    }

    /**
     * Helper method to set up Spring Security authentication with the given permissions.
     * Also includes read permissions for related entities (Unit, Currency, TaxClass) needed for validation.
     */
    private void setPermissions(String... permissions) {
        // Build combined list of permissions including those needed for validation
        java.util.List<String> allPermissions = new java.util.ArrayList<>();
        allPermissions.addAll(java.util.Arrays.asList(permissions));

        // Add read permissions for related entities needed during validation
        allPermissions.add("priceprovider.public:Unit:read");
        allPermissions.add("priceprovider.public:Currency:read");
        allPermissions.add("priceprovider.public:TaxClass:read");

        var authorities = AuthorityUtils.createAuthorityList(allPermissions.toArray(new String[0]));
        var auth = new UsernamePasswordAuthenticationToken("test-user", "test", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
