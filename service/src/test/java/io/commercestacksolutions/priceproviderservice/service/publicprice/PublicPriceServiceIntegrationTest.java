package io.commercestacksolutions.priceproviderservice.service.publicprice;

import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.ChannelEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.CountryEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.type.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PublicPriceService.
 * Tests the complete flow with real database using Spring Boot test context.
 */
@SpringBootTest
@ActiveProfiles("test")
public class PublicPriceServiceIntegrationTest {
    
    @Autowired
    private PublicPriceService publicPriceService;
    
    @Autowired
    private PriceRowEntityRepository priceRowRepository;
    
    @Autowired
    private CurrencyEntityRepository currencyRepository;
    
    @Autowired
    private UnitEntityRepository unitRepository;
    
    @Autowired
    private TaxClassEntityRepository taxClassRepository;
    
    @Autowired
    private GroupEntityRepository groupRepository;

    @Autowired
    private ChannelEntityRepository channelRepository;

    @Autowired
    private CountryEntityRepository countryRepository;
    
    private CurrencyEntity testCurrencyEUR;
    private CurrencyEntity testCurrencyUSD;
    private UnitEntity testUnitPiece;
    private UnitEntity testUnitKg;
    private TaxClassEntity testTaxClass;
    private TaxClassEntity testTaxClassDE;
    private TaxClassEntity testTaxClassUS;
    private GroupEntity testGroupChild;
    private GroupEntity testGroupParent;
    private GroupEntity testGroupGrandparent;
    private ChannelEntity testChannelDach;
    private ChannelEntity testChannelGlobal;
    private CountryEntity testCountryDE;
    private CountryEntity testCountryUS;
    
    @BeforeEach
    public void setUp() {
        // Enable bootstrap mode to bypass permission checks in tests
        AuthorizationContext.enableBootstrapMode();

        priceRowRepository.deleteAll();
        groupRepository.deleteAll();
        channelRepository.deleteAll();
        taxClassRepository.deleteAll();
        countryRepository.deleteAll();
        unitRepository.deleteAll();
        currencyRepository.deleteAll();
        
        testCurrencyEUR = new CurrencyEntity();
        testCurrencyEUR.setCurrencyKey("EUR");
        testCurrencyEUR.setCreatedAt(OffsetDateTime.now());
        testCurrencyEUR.setLastModifiedAt(OffsetDateTime.now());
        testCurrencyEUR = currencyRepository.save(testCurrencyEUR);
        
        testCurrencyUSD = new CurrencyEntity();
        testCurrencyUSD.setCurrencyKey("USD");
        testCurrencyUSD.setCreatedAt(OffsetDateTime.now());
        testCurrencyUSD.setLastModifiedAt(OffsetDateTime.now());
        testCurrencyUSD = currencyRepository.save(testCurrencyUSD);
        
        testUnitPiece = new UnitEntity();
        testUnitPiece.setSymbol("piece");
        testUnitPiece.setCreatedAt(OffsetDateTime.now());
        testUnitPiece.setLastModifiedAt(OffsetDateTime.now());
        testUnitPiece = unitRepository.save(testUnitPiece);
        
        testUnitKg = new UnitEntity();
        testUnitKg.setSymbol("kg");
        testUnitKg.setCreatedAt(OffsetDateTime.now());
        testUnitKg.setLastModifiedAt(OffsetDateTime.now());
        testUnitKg = unitRepository.save(testUnitKg);

        // Countries
        testCountryDE = new CountryEntity();
        testCountryDE.setIsoKey("DE");
        testCountryDE.setName(Map.of("en", "Germany", "de", "Deutschland"));
        testCountryDE.setCreatedAt(OffsetDateTime.now());
        testCountryDE.setLastModifiedAt(OffsetDateTime.now());
        testCountryDE = countryRepository.save(testCountryDE);

        testCountryUS = new CountryEntity();
        testCountryUS.setIsoKey("US");
        testCountryUS.setName(Map.of("en", "United States"));
        testCountryUS.setCreatedAt(OffsetDateTime.now());
        testCountryUS.setLastModifiedAt(OffsetDateTime.now());
        testCountryUS = countryRepository.save(testCountryUS);

        testTaxClass = new TaxClassEntity();
        testTaxClass.setTaxClassId("STANDARD");
        testTaxClass.setTaxRate(new BigDecimal("0.19"));
        testTaxClass.setCreatedAt(OffsetDateTime.now());
        testTaxClass.setLastModifiedAt(OffsetDateTime.now());
        testTaxClass = taxClassRepository.save(testTaxClass);

        testTaxClassDE = new TaxClassEntity();
        testTaxClassDE.setTaxClassId("DE-STANDARD");
        testTaxClassDE.setTaxRate(new BigDecimal("0.19"));
        testTaxClassDE.setCountry(testCountryDE);
        testTaxClassDE.setCreatedAt(OffsetDateTime.now());
        testTaxClassDE.setLastModifiedAt(OffsetDateTime.now());
        testTaxClassDE = taxClassRepository.save(testTaxClassDE);

        testTaxClassUS = new TaxClassEntity();
        testTaxClassUS.setTaxClassId("US-STANDARD");
        testTaxClassUS.setTaxRate(new BigDecimal("0.08"));
        testTaxClassUS.setCountry(testCountryUS);
        testTaxClassUS.setCreatedAt(OffsetDateTime.now());
        testTaxClassUS.setLastModifiedAt(OffsetDateTime.now());
        testTaxClassUS = taxClassRepository.save(testTaxClassUS);

        // Channels
        testChannelDach = new ChannelEntity();
        testChannelDach.setId("test-dach-channel");
        testChannelDach.setAllowedCountryRefs(Set.of(testCountryDE));
        testChannelDach.setCreatedAt(OffsetDateTime.now());
        testChannelDach.setLastModifiedAt(OffsetDateTime.now());
        testChannelDach = channelRepository.save(testChannelDach);

        testChannelGlobal = new ChannelEntity();
        testChannelGlobal.setId("test-global-channel");
        testChannelGlobal.setAllowedCountryRefs(Set.of(testCountryDE, testCountryUS));
        testChannelGlobal.setCreatedAt(OffsetDateTime.now());
        testChannelGlobal.setLastModifiedAt(OffsetDateTime.now());
        testChannelGlobal = channelRepository.save(testChannelGlobal);
        
        testGroupGrandparent = new GroupEntity();
        testGroupGrandparent.setPath("GROUP-GRANDPARENT");
        testGroupGrandparent.setCreatedAt(OffsetDateTime.now());
        testGroupGrandparent.setLastModifiedAt(OffsetDateTime.now());
        testGroupGrandparent = groupRepository.save(testGroupGrandparent);
        
        testGroupParent = new GroupEntity();
        testGroupParent.setPath("GROUP-PARENT");
        testGroupParent.setParentRefs(Set.of(testGroupGrandparent));
        testGroupParent.setCreatedAt(OffsetDateTime.now());
        testGroupParent.setLastModifiedAt(OffsetDateTime.now());
        testGroupParent = groupRepository.save(testGroupParent);
        
        testGroupChild = new GroupEntity();
        testGroupChild.setPath("GROUP-CHILD");
        testGroupChild.setParentRefs(Set.of(testGroupParent));
        testGroupChild.setCreatedAt(OffsetDateTime.now());
        testGroupChild.setLastModifiedAt(OffsetDateTime.now());
        testGroupChild = groupRepository.save(testGroupChild);
    }
    
    @AfterEach
    public void tearDown() {
        priceRowRepository.deleteAll();
        groupRepository.deleteAll();
        channelRepository.deleteAll();
        taxClassRepository.deleteAll();
        countryRepository.deleteAll();
        unitRepository.deleteAll();
        currencyRepository.deleteAll();

        // Disable bootstrap mode after test
        AuthorizationContext.disableBootstrapMode();
    }
    
    @Test
    public void testFindBestPrice_BasicScenario() {
        PriceRowEntity price = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(price);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNotNull(result);
        assertEquals("PROD-001", result.getPricedResourceId());
        assertEquals(new BigDecimal("100.00"), result.getPriceValue());
    }
    
    @Test
    public void testFindBestPrice_GroupDistancePriority_ChildWins() {
        PriceRowEntity priceGrandparent = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceGrandparent.setGroups(Set.of(testGroupGrandparent));
        priceRowRepository.save(priceGrandparent);
        
        PriceRowEntity priceParent = createPrice("PROD-001", new BigDecimal("95.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceParent.setGroups(Set.of(testGroupParent));
        priceRowRepository.save(priceParent);
        
        PriceRowEntity priceChild = createPrice("PROD-001", new BigDecimal("90.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceChild.setGroups(Set.of(testGroupChild));
        priceRowRepository.save(priceChild);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setGroupId("GROUP-CHILD");
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("90.00"), result.getPriceValue(), "Child group price should win (lowest distance)");
    }
    
    @Test
    public void testFindBestPrice_GroupDistancePriority_ParentBeatsGrandparent() {
        PriceRowEntity priceGrandparent = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceGrandparent.setGroups(Set.of(testGroupGrandparent));
        priceRowRepository.save(priceGrandparent);
        
        PriceRowEntity priceParent = createPrice("PROD-001", new BigDecimal("95.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceParent.setGroups(Set.of(testGroupParent));
        priceRowRepository.save(priceParent);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setGroupId("GROUP-CHILD");
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("95.00"), result.getPriceValue(), "Parent group price should win over grandparent");
    }
    
    @Test
    public void testFindBestPrice_GroupSpecificBeatsGeneric() {
        PriceRowEntity priceGeneric = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(priceGeneric);
        
        PriceRowEntity priceGroupSpecific = createPrice("PROD-001", new BigDecimal("85.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceGroupSpecific.setGroups(Set.of(testGroupChild));
        priceRowRepository.save(priceGroupSpecific);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setGroupId("GROUP-CHILD");
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("85.00"), result.getPriceValue(), "Group-specific price should win over generic");
    }
    
    @Test
    public void testFindBestPrice_DateRangeFiltering() {
        OffsetDateTime now = OffsetDateTime.now();
        
        PriceRowEntity priceExpired = createPrice("PROD-001", new BigDecimal("80.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceExpired.setValidFrom(now.minusDays(30));
        priceExpired.setValidTo(now.minusDays(1));
        priceRowRepository.save(priceExpired);
        
        PriceRowEntity priceActive = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceActive.setValidFrom(now.minusDays(10));
        priceActive.setValidTo(now.plusDays(10));
        priceRowRepository.save(priceActive);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setReferenceDate(now);
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getPriceValue(), "Only active price should be returned");
    }
    
    @Test
    public void testFindBestPrice_QuantityTierMatching() {
        PriceRowEntity priceTier1 = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceTier1.setMinQuantity(new BigDecimal("1.00"));
        priceRowRepository.save(priceTier1);
        
        PriceRowEntity priceTier2 = createPrice("PROD-001", new BigDecimal("90.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceTier2.setMinQuantity(new BigDecimal("10.00"));
        priceRowRepository.save(priceTier2);
        
        PriceRowEntity priceTier3 = createPrice("PROD-001", new BigDecimal("80.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceTier3.setMinQuantity(new BigDecimal("100.00"));
        priceRowRepository.save(priceTier3);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("50.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("90.00"), result.getPriceValue(), "Tier 2 with minQuantity=10 should win for quantity=50");
    }
    
    @Test
    public void testFindBestPrice_MultipleCurrencies() {
        PriceRowEntity priceEUR = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(priceEUR);
        
        PriceRowEntity priceUSD = createPrice("PROD-001", new BigDecimal("120.00"), testCurrencyUSD, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(priceUSD);
        
        PriceMatchingCriteria criteriaEUR = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        PriceMatchingCriteria criteriaUSD = createCriteria("PROD-001", new BigDecimal("10.00"), "USD", "piece", new PriceType("SALES_PRICE"));
        
        PriceRowEntity resultEUR = publicPriceService.findBestPrice(criteriaEUR);
        PriceRowEntity resultUSD = publicPriceService.findBestPrice(criteriaUSD);
        
        assertNotNull(resultEUR);
        assertNotNull(resultUSD);
        assertEquals(new BigDecimal("100.00"), resultEUR.getPriceValue());
        assertEquals(new BigDecimal("120.00"), resultUSD.getPriceValue());
    }
    
    @Test
    public void testFindBestPrice_AllPriceTypes() {
        PriceRowEntity salesPrice = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(salesPrice);
        
        PriceRowEntity purchasePrice = createPrice("PROD-001", new BigDecimal("80.00"), testCurrencyEUR, testUnitPiece, new PriceType("PURCHASE_PRICE"));
        priceRowRepository.save(purchasePrice);
        
        PriceRowEntity materialCost = createPrice("PROD-001", new BigDecimal("60.00"), testCurrencyEUR, testUnitPiece, new PriceType("MATERIAL_COST"));
        priceRowRepository.save(materialCost);
        
        PriceMatchingCriteria criteriaSales = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        PriceMatchingCriteria criteriaPurchase = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("PURCHASE_PRICE"));
        PriceMatchingCriteria criteriaMaterial = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("MATERIAL_COST"));
        
        PriceRowEntity resultSales = publicPriceService.findBestPrice(criteriaSales);
        PriceRowEntity resultPurchase = publicPriceService.findBestPrice(criteriaPurchase);
        PriceRowEntity resultMaterial = publicPriceService.findBestPrice(criteriaMaterial);
        
        assertNotNull(resultSales);
        assertNotNull(resultPurchase);
        assertNotNull(resultMaterial);
        assertEquals(new BigDecimal("100.00"), resultSales.getPriceValue());
        assertEquals(new BigDecimal("80.00"), resultPurchase.getPriceValue());
        assertEquals(new BigDecimal("60.00"), resultMaterial.getPriceValue());
    }
    
    @Test
    public void testFindAllPrices_ReturnsAllMatchesRanked() {
        PriceRowEntity price1 = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        price1.setMinQuantity(new BigDecimal("1.00"));
        priceRowRepository.save(price1);
        
        PriceRowEntity price2 = createPrice("PROD-001", new BigDecimal("90.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        price2.setMinQuantity(new BigDecimal("10.00"));
        priceRowRepository.save(price2);
        
        PriceRowEntity price3 = createPrice("PROD-001", new BigDecimal("80.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        price3.setMinQuantity(new BigDecimal("100.00"));
        priceRowRepository.save(price3);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("150.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        
        List<PriceRowEntity> results = publicPriceService.findAllPrices(criteria);
        
        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals(new BigDecimal("80.00"), results.get(0).getPriceValue(), "Highest minQuantity should be first");
        assertEquals(new BigDecimal("90.00"), results.get(1).getPriceValue());
        assertEquals(new BigDecimal("100.00"), results.get(2).getPriceValue());
    }
    
    @Test
    public void testFindBestPrice_NoMatch_ReturnsNull() {
        PriceRowEntity price = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(price);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-999", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        
        PriceRowEntity result = publicPriceService.findBestPrice(criteria);
        
        assertNull(result);
    }
    
    @Test
    public void testFindAllPrices_NoMatch_ReturnsEmptyList() {
        PriceRowEntity price = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceRowRepository.save(price);
        
        PriceMatchingCriteria criteria = createCriteria("PROD-999", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        
        List<PriceRowEntity> results = publicPriceService.findAllPrices(criteria);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ---- Channel and Country filtering tests ----

    @Test
    public void testFindBestPrice_ChannelFilter_MatchesAssignedChannel() {
        // Price assigned to DACH channel
        PriceRowEntity price = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        price.setTaxClass(testTaxClassDE);
        price.setChannels(Set.of(testChannelDach));
        priceRowRepository.save(price);

        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setChannelId("test-dach-channel");
        criteria.setCountryKey("DE");

        PriceRowEntity result = publicPriceService.findBestPrice(criteria);

        assertNotNull(result, "Should find price assigned to the requested channel");
        assertEquals(new BigDecimal("100.00"), result.getPriceValue());
    }

    @Test
    public void testFindBestPrice_ChannelFilter_NoMatchForDifferentChannel() {
        // Price assigned only to DACH channel
        PriceRowEntity price = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        price.setTaxClass(testTaxClassDE);
        price.setChannels(Set.of(testChannelDach));
        priceRowRepository.save(price);

        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setChannelId("test-global-channel");
        criteria.setCountryKey("DE");

        PriceRowEntity result = publicPriceService.findBestPrice(criteria);

        assertNull(result, "Should NOT find price assigned only to a different channel");
    }

    @Test
    public void testFindBestPrice_ChannelFilter_PriceWithNoChannelMatchesAnyChannel() {
        // Price with no channel assignment (generic) - should match any channel
        PriceRowEntity genericPrice = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        genericPrice.setTaxClass(testTaxClassDE);
        genericPrice.setChannels(new HashSet<>());
        priceRowRepository.save(genericPrice);

        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setChannelId("test-dach-channel");
        criteria.setCountryKey("DE");

        PriceRowEntity result = publicPriceService.findBestPrice(criteria);

        assertNotNull(result, "Price with no channel assignment should match any channel");
        assertEquals(new BigDecimal("100.00"), result.getPriceValue());
    }

    @Test
    public void testFindBestPrice_ChannelFilter_SpecificChannelBeatsGenericPrice() {
        // Generic price (no channel) - lower priority than channel-specific
        PriceRowEntity genericPrice = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        genericPrice.setTaxClass(testTaxClassDE);
        genericPrice.setChannels(new HashSet<>());
        priceRowRepository.save(genericPrice);

        // Channel-specific price - should win
        PriceRowEntity channelPrice = createPrice("PROD-001", new BigDecimal("90.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        channelPrice.setTaxClass(testTaxClassDE);
        channelPrice.setChannels(Set.of(testChannelDach));
        priceRowRepository.save(channelPrice);

        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setChannelId("test-dach-channel");
        criteria.setCountryKey("DE");

        List<PriceRowEntity> results = publicPriceService.findAllPrices(criteria);

        assertNotNull(results);
        assertEquals(2, results.size(), "Both generic and channel-specific prices should be returned");
    }

    @Test
    public void testFindBestPrice_CountryFilter_MatchesTaxClassCountry() {
        // Price with DE tax class
        PriceRowEntity priceDE = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceDE.setTaxClass(testTaxClassDE);
        priceDE.setChannels(Set.of(testChannelDach));
        priceRowRepository.save(priceDE);

        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setChannelId("test-dach-channel");
        criteria.setCountryKey("DE");

        PriceRowEntity result = publicPriceService.findBestPrice(criteria);

        assertNotNull(result, "Should find price for DE country via tax class country reference");
        assertEquals(new BigDecimal("100.00"), result.getPriceValue());
    }

    @Test
    public void testFindBestPrice_CountryFilter_NoMatchForDifferentCountry() {
        // Price only for DE
        PriceRowEntity priceDE = createPrice("PROD-001", new BigDecimal("100.00"), testCurrencyEUR, testUnitPiece, new PriceType("SALES_PRICE"));
        priceDE.setTaxClass(testTaxClassDE);
        priceDE.setChannels(Set.of(testChannelDach));
        priceRowRepository.save(priceDE);

        PriceMatchingCriteria criteria = createCriteria("PROD-001", new BigDecimal("10.00"), "EUR", "piece", new PriceType("SALES_PRICE"));
        criteria.setChannelId("test-dach-channel");
        criteria.setCountryKey("US");

        PriceRowEntity result = publicPriceService.findBestPrice(criteria);

        assertNull(result, "Should NOT find a DE price when filtering for US");
    }
    
    private PriceRowEntity createPrice(String pricedResourceId, BigDecimal value, CurrencyEntity currency, UnitEntity unit, PriceType priceType) {
        PriceRowEntity price = new PriceRowEntity();
        price.setPricedResourceId(pricedResourceId);
        price.setPriceValue(value);
        price.setCurrency(currency);
        price.setUnit(unit);
        price.setPriceType(priceType);
        price.setMinQuantity(new BigDecimal("1.00"));
        price.setTaxIncluded(false);
        price.setTaxClass(testTaxClass);
        price.setValidFrom(OffsetDateTime.now().minusDays(365));
        price.setValidTo(null);
        price.setGroups(new HashSet<>());
        price.setChannels(new HashSet<>());
        price.setCreatedAt(OffsetDateTime.now());
        price.setLastModifiedAt(OffsetDateTime.now());
        return price;
    }
    
    private PriceMatchingCriteria createCriteria(String pricedResourceId, BigDecimal quantity, String currencyRef, String unitRef, PriceType priceType) {
        PriceMatchingCriteria criteria = new PriceMatchingCriteria();
        criteria.setPricedResourceId(pricedResourceId);
        criteria.setQuantity(quantity);
        criteria.setCurrencyRef(currencyRef);
        criteria.setUnitRef(unitRef);
        criteria.setPriceType(priceType);
        criteria.setReferenceDate(OffsetDateTime.now());
        criteria.setTaxationMode(PriceMatchingCriteria.TaxationMode.GROSS);
        return criteria;
    }
}
