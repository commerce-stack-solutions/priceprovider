package io.commercestacksolutions.priceproviderservice.facade.publicprice;

import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.ChannelEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.CountryEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the channel-country consistency enforcement in the Public Price API facade.
 *
 * <h2>Business Rule</h2>
 * <p>Channels define which countries they serve via {@code allowedCountryRefs}.
 * When a client requests a price for a given channel + country combination, the facade
 * must first validate that the country is in the channel's allowed list.
 * If it is not, no price is returned — even if a matching price row exists in the database.</p>
 *
 * <h2>Why this matters</h2>
 * <p>Without this check, a caller could request prices for country "US" through a DACH channel
 * (which only covers DE/AT/CH), potentially receiving incorrect prices with wrong tax rates.
 * This guard prevents cross-country pricing errors in multi-channel setups.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public class PublicPriceFacadeChannelCountryIntegrationTest {

    @Autowired
    private PublicPriceFacade publicPriceFacade;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private UnitEntityRepository unitRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @Autowired
    private ChannelEntityRepository channelRepository;

    @Autowired
    private CountryEntityRepository countryRepository;

    private CurrencyEntity currency;
    private UnitEntity unit;
    private CountryEntity countryDE;
    private CountryEntity countryUS;
    private TaxClassEntity taxClassDE;
    private TaxClassEntity taxClassUS;
    private ChannelEntity channelDACH;   // allows DE only
    private ChannelEntity channelGlobal; // allows DE + US

    @BeforeEach
    void setUp() {
        priceRowRepository.deleteAll();
        channelRepository.deleteAll();
        taxClassRepository.deleteAll();
        countryRepository.deleteAll();
        unitRepository.deleteAll();
        currencyRepository.deleteAll();

        currency = new CurrencyEntity();
        currency.setCurrencyKey("EUR");
        currency.setCreatedAt(OffsetDateTime.now());
        currency.setLastModifiedAt(OffsetDateTime.now());
        currency = currencyRepository.save(currency);

        unit = new UnitEntity();
        unit.setSymbol("piece");
        unit.setCreatedAt(OffsetDateTime.now());
        unit.setLastModifiedAt(OffsetDateTime.now());
        unit = unitRepository.save(unit);

        countryDE = new CountryEntity();
        countryDE.setIsoKey("DE");
        countryDE.setName(Map.of("en", "Germany"));
        countryDE.setCreatedAt(OffsetDateTime.now());
        countryDE.setLastModifiedAt(OffsetDateTime.now());
        countryDE = countryRepository.save(countryDE);

        countryUS = new CountryEntity();
        countryUS.setIsoKey("US");
        countryUS.setName(Map.of("en", "United States"));
        countryUS.setCreatedAt(OffsetDateTime.now());
        countryUS.setLastModifiedAt(OffsetDateTime.now());
        countryUS = countryRepository.save(countryUS);

        taxClassDE = new TaxClassEntity();
        taxClassDE.setTaxClassId("facade-test-de");
        taxClassDE.setTaxRate(new BigDecimal("0.19"));
        taxClassDE.setCountry(countryDE);
        taxClassDE.setCreatedAt(OffsetDateTime.now());
        taxClassDE.setLastModifiedAt(OffsetDateTime.now());
        taxClassDE = taxClassRepository.save(taxClassDE);

        taxClassUS = new TaxClassEntity();
        taxClassUS.setTaxClassId("facade-test-us");
        taxClassUS.setTaxRate(new BigDecimal("0.08"));
        taxClassUS.setCountry(countryUS);
        taxClassUS.setCreatedAt(OffsetDateTime.now());
        taxClassUS.setLastModifiedAt(OffsetDateTime.now());
        taxClassUS = taxClassRepository.save(taxClassUS);

        channelDACH = new ChannelEntity();
        channelDACH.setId("facade-test-dach");
        channelDACH.setAllowedCountryRefs(Set.of(countryDE)); // DE only
        channelDACH.setPriceRepresentationMode("FORCE_GROSS");
        channelDACH.setCreatedAt(OffsetDateTime.now());
        channelDACH.setLastModifiedAt(OffsetDateTime.now());
        channelDACH = channelRepository.save(channelDACH);

        channelGlobal = new ChannelEntity();
        channelGlobal.setId("facade-test-global");
        channelGlobal.setAllowedCountryRefs(new HashSet<>(Set.of(countryDE, countryUS))); // DE + US
        channelGlobal.setPriceRepresentationMode("FORCE_GROSS");
        channelGlobal.setCreatedAt(OffsetDateTime.now());
        channelGlobal.setLastModifiedAt(OffsetDateTime.now());
        channelGlobal = channelRepository.save(channelGlobal);
    }

    @AfterEach
    void tearDown() {
        priceRowRepository.deleteAll();
        channelRepository.deleteAll();
        taxClassRepository.deleteAll();
        countryRepository.deleteAll();
        unitRepository.deleteAll();
        currencyRepository.deleteAll();
    }

    // ---- getBestPrice channel-country validation ----

    @Test
    void getBestPrice_countryAllowedInChannel_returnsPriceSuccessfully() throws Exception {
        // GIVEN: A DE price row assigned to the DACH channel (which allows DE)
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-001", new BigDecimal("100.00"), taxClassDE, channelDACH);
        priceRowRepository.save(priceRow);

        // WHEN: Requesting a price for DE via DACH channel
        PublicPriceRestEntity result = publicPriceFacade.getBestPrice(
                "facade-test-dach", "DE", null, "FACADE-PROD-001",
                new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of());

        // THEN: Price is returned correctly
        assertNotNull(result, "Should return a price when country is allowed in channel");
    }

    @Test
    void getBestPrice_countryNotAllowedInChannel_throwsNotFoundException() {
        // GIVEN: A DE price row assigned to the DACH channel
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-002", new BigDecimal("100.00"), taxClassDE, channelDACH);
        priceRowRepository.save(priceRow);

        // WHEN: Requesting a price for US via DACH channel (US is NOT in DACH allowed countries)
        // THEN: NotFoundException is thrown — the API must not return any price
        assertThrows(NotFoundException.class,
                () -> publicPriceFacade.getBestPrice(
                        "facade-test-dach", "US", null, "FACADE-PROD-002",
                        new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of()),
                "Requesting a price for a country not in the channel's allowedCountryRefs must throw NotFoundException");
    }

    @Test
    void getBestPrice_channelDoesNotExist_throwsNotFoundException() {
        // WHEN: Requesting a price for a non-existent channel
        // THEN: NotFoundException is thrown
        assertThrows(NotFoundException.class,
                () -> publicPriceFacade.getBestPrice(
                        "non-existent-channel", "DE", null, "FACADE-PROD-001",
                        new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of()),
                "Non-existent channel must throw NotFoundException");
    }

    @Test
    void getBestPrice_countryAllowedInGlobalChannel_returnsPrice() throws Exception {
        // GIVEN: A US price row assigned to the global channel (which allows DE + US)
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-003", new BigDecimal("200.00"), taxClassUS, channelGlobal);
        priceRowRepository.save(priceRow);

        // WHEN: Requesting a price for US via global channel
        PublicPriceRestEntity result = publicPriceFacade.getBestPrice(
                "facade-test-global", "US", null, "FACADE-PROD-003",
                new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of());

        // THEN: Price is returned
        assertNotNull(result, "Should return a price for US via global channel");
    }

    @Test
    void getBestPrice_deCurrencyThroughDachChannel_deNotAllowedInWrongChannel_throwsNotFound() {
        // GIVEN: A DE price row assigned to global channel
        // WHEN: Requesting for DE via DACH channel — this should still work because DE IS in DACH
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-004", new BigDecimal("80.00"), taxClassDE, channelDACH);
        priceRowRepository.save(priceRow);

        // Confirming US is blocked from DACH
        assertThrows(NotFoundException.class,
                () -> publicPriceFacade.getBestPrice(
                        "facade-test-dach", "US", null, "FACADE-PROD-004",
                        new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of()),
                "US country should be rejected by DACH channel which only allows DE");
    }

    // ---- getAllPrices channel-country validation ----

    @Test
    void getAllPrices_countryAllowedInChannel_returnsNonEmptyList() throws Exception {
        // GIVEN: A DE price row assigned to DACH channel
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-005", new BigDecimal("100.00"), taxClassDE, channelDACH);
        priceRowRepository.save(priceRow);

        // WHEN: Requesting all prices for DE via DACH channel
        PublicPriceListRestEntity result = publicPriceFacade.getAllPrices(
                "facade-test-dach", "DE", null, "FACADE-PROD-005",
                new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of());

        // THEN: Results are returned
        assertNotNull(result);
        assertFalse(result.getItems().isEmpty(), "Should return prices when country is allowed in channel");
    }

    @Test
    void getAllPrices_countryNotAllowedInChannel_throwsNotFoundException() throws Exception {
        // GIVEN: A DE price row assigned to DACH channel
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-006", new BigDecimal("100.00"), taxClassDE, channelDACH);
        priceRowRepository.save(priceRow);

        // WHEN: Requesting all prices for US via DACH channel (US not allowed in DACH)
        assertThrows(NotFoundException.class,
                () ->  publicPriceFacade.getAllPrices(
                "facade-test-dach", "US", null, "FACADE-PROD-006",
                new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of()),
                "A selected country not allowed in channel must throw NotFoundException");
    }

    // ---- getBestPrice with group context, channel-country validation ----

    @Test
    void getBestPriceWithGroup_countryNotAllowedInChannel_throwsNotFoundException() {
        // GIVEN: A DE price row assigned to DACH channel
        PriceRowEntity priceRow = buildPriceRow("FACADE-PROD-007", new BigDecimal("100.00"), taxClassDE, channelDACH);
        priceRowRepository.save(priceRow);

        // WHEN: Requesting for US via DACH channel (with group context)
        assertThrows(NotFoundException.class,
                () -> publicPriceFacade.getBestPrice(
                        "facade-test-dach", "US", "some-group", "FACADE-PROD-007",
                        new BigDecimal("1"), "piece", "EUR", PriceType.SALES_PRICE, Set.of()),
                "Group-scoped price request with disallowed country must throw NotFoundException");
    }

    // ---- Helper ----

    private PriceRowEntity buildPriceRow(String pricedResourceId, BigDecimal value,
                                          TaxClassEntity taxClass, ChannelEntity channel) {
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId(pricedResourceId);
        priceRow.setPriceValue(value);
        priceRow.setCurrency(currency);
        priceRow.setUnit(unit);
        priceRow.setPriceType(PriceType.SALES_PRICE);
        priceRow.setMinQuantity(new BigDecimal("1.00"));
        priceRow.setTaxIncluded(false);
        priceRow.setTaxClass(taxClass);
        priceRow.setValidFrom(OffsetDateTime.now().minusDays(365));
        priceRow.setValidTo(null);
        priceRow.setGroups(new HashSet<>());
        priceRow.setChannels(new HashSet<>(Set.of(channel)));
        priceRow.setCreatedAt(OffsetDateTime.now());
        priceRow.setLastModifiedAt(OffsetDateTime.now());
        return priceRow;
    }
}
