package de.ebusyness.priceproviderservice.service.pricerow.validation;

import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ChannelCountryConsistencyRule}.
 *
 * <p>Business rule: if a price row is assigned to channels, the tax class's country
 * must be included in every channel's allowedCountryRefs. This prevents creating price
 * rows that would never be reachable through the public price API's channel/country scope.</p>
 */
class ChannelCountryConsistencyRuleTest {

    private ChannelCountryConsistencyRule rule;

    private CountryEntity countryDE;
    private CountryEntity countryUS;
    private TaxClassEntity taxClassDE;
    private TaxClassEntity taxClassUS;
    private TaxClassEntity taxClassNoCountry;
    private ChannelEntity channelDACH;
    private ChannelEntity channelGlobal;
    private ChannelEntity channelNoCountries;

    @BeforeEach
    void setUp() {
        rule = new ChannelCountryConsistencyRule();

        countryDE = new CountryEntity();
        countryDE.setIsoKey("DE");

        countryUS = new CountryEntity();
        countryUS.setIsoKey("US");

        taxClassDE = new TaxClassEntity();
        taxClassDE.setTaxClassId("DE-STANDARD");
        taxClassDE.setCountry(countryDE);

        taxClassUS = new TaxClassEntity();
        taxClassUS.setTaxClassId("US-STANDARD");
        taxClassUS.setCountry(countryUS);

        taxClassNoCountry = new TaxClassEntity();
        taxClassNoCountry.setTaxClassId("GENERIC");
        // no country

        channelDACH = new ChannelEntity();
        channelDACH.setId("dach-channel");
        channelDACH.setAllowedCountryRefs(Set.of(countryDE));

        channelGlobal = new ChannelEntity();
        channelGlobal.setId("global-channel");
        channelGlobal.setAllowedCountryRefs(new HashSet<>(Set.of(countryDE, countryUS)));

        channelNoCountries = new ChannelEntity();
        channelNoCountries.setId("no-restriction-channel");
        channelNoCountries.setAllowedCountryRefs(new HashSet<>());
    }

    // ---- Passing cases ----

    @Test
    void validate_nullEntity_returnsNoErrors() {
        List<Message> errors = rule.validate(null);
        assertTrue(errors.isEmpty(), "Null entity should produce no validation errors");
    }

    @Test
    void validate_noChannelAssignment_genericPriceRow_alwaysValid() {
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassDE);
        priceRow.setChannels(new HashSet<>());

        List<Message> errors = rule.validate(priceRow);

        assertTrue(errors.isEmpty(),
                "Price row with no channel assignment (generic) should always be valid");
    }

    @Test
    void validate_channelAssigned_countryIsAllowed_noErrors() {
        // taxClassDE references DE; channelDACH allows DE
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassDE);
        priceRow.setChannels(Set.of(channelDACH));

        List<Message> errors = rule.validate(priceRow);

        assertTrue(errors.isEmpty(),
                "Price row with DE tax class assigned to DACH channel (allows DE) should be valid");
    }

    @Test
    void validate_multipleChannels_countryAllowedInAll_noErrors() {
        // taxClassDE references DE; both channelDACH and channelGlobal allow DE
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassDE);
        priceRow.setChannels(new HashSet<>(Set.of(channelDACH, channelGlobal)));

        List<Message> errors = rule.validate(priceRow);

        assertTrue(errors.isEmpty(),
                "Price row valid when country allowed in all assigned channels");
    }

    @Test
    void validate_channelWithNoCountryRestrictions_alwaysAllowed() {
        // channelNoCountries has no allowedCountryRefs — no restriction
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassDE);
        priceRow.setChannels(Set.of(channelNoCountries));

        List<Message> errors = rule.validate(priceRow);

        assertTrue(errors.isEmpty(),
                "Channel with no country restrictions should accept any tax class country");
    }

    @Test
    void validate_taxClassWithoutCountry_noErrors() {
        // No country reference on tax class → no restriction to enforce
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassNoCountry);
        priceRow.setChannels(Set.of(channelDACH));

        List<Message> errors = rule.validate(priceRow);

        assertTrue(errors.isEmpty(),
                "Tax class without a country should not trigger channel-country consistency check");
    }

    // ---- Failing cases ----

    @Test
    void validate_channelAssigned_countryNotAllowed_returnsError() {
        // taxClassUS references US; channelDACH only allows DE
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassUS);
        priceRow.setChannels(Set.of(channelDACH));

        List<Message> errors = rule.validate(priceRow);

        assertFalse(errors.isEmpty(),
                "Should fail when US tax class is assigned to DACH channel that only allows DE");
        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
        assertTrue(errors.get(0).getFields().contains("channelRefs"),
                "Error should reference the channelRefs field");
    }

    @Test
    void validate_multipleChannels_countryNotAllowedInOne_returnsError() {
        // taxClassUS references US; channelGlobal allows DE+US but channelDACH only allows DE
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassUS);
        priceRow.setChannels(new HashSet<>(Set.of(channelDACH, channelGlobal)));

        List<Message> errors = rule.validate(priceRow);

        assertEquals(1, errors.size(),
                "Exactly one error for the one channel that doesn't allow US");
        assertTrue(errors.get(0).getFields().contains("channelRefs"));
    }

    @Test
    void validate_multipleChannels_countryNotAllowedInAny_returnsMultipleErrors() {
        // Create two DACH-like channels that only allow DE
        ChannelEntity channelEURO = new ChannelEntity();
        channelEURO.setId("euro-channel");
        channelEURO.setAllowedCountryRefs(Set.of(countryDE));

        // taxClassUS references US; both channels only allow DE
        PriceRowEntity priceRow = priceRowWithTaxClass(taxClassUS);
        priceRow.setChannels(new HashSet<>(Set.of(channelDACH, channelEURO)));

        List<Message> errors = rule.validate(priceRow);

        assertEquals(2, errors.size(),
                "Should produce one error per channel that doesn't allow the country");
    }

    // ---- Helper ----

    private PriceRowEntity priceRowWithTaxClass(TaxClassEntity taxClass) {
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setTaxClass(taxClass);
        priceRow.setChannels(new HashSet<>());
        return priceRow;
    }
}
