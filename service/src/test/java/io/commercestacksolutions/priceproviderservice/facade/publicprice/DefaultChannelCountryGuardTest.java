package io.commercestacksolutions.priceproviderservice.facade.publicprice;

import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.service.channel.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultChannelCountryGuard}.
 *
 * <p>Verifies that the guard correctly enforces channel-country access control.
 * The guard is designed to be swappable — these tests document the contract
 * that custom implementations must follow.</p>
 */
class DefaultChannelCountryGuardTest {

    private ChannelService channelService;
    private DefaultChannelCountryGuard guard;

    @BeforeEach
    void setUp() {
        channelService = mock(ChannelService.class);
        guard = new DefaultChannelCountryGuard(channelService);
    }

    @Test
    void assertCountryAllowedInChannel_channelNotFound_throwsNotFoundException() {
        when(channelService.getChannel("unknown-channel")).thenReturn(null);

        assertThrows(NotFoundException.class,
                () -> guard.assertCountryAllowedInChannel("unknown-channel", "DE"),
                "Non-existent channel must throw NotFoundException");
    }

    @Test
    void assertCountryAllowedInChannel_countryAllowed_doesNotThrow() throws NotFoundException {
        CountryEntity de = countryEntity("DE");
        ChannelEntity channel = channelEntity("dach", Set.of(de));
        when(channelService.getChannel("dach")).thenReturn(channel);

        // Should not throw
        guard.assertCountryAllowedInChannel("dach", "DE");
    }

    @Test
    void assertCountryAllowedInChannel_countryNotAllowed_throwsNotFoundException() {
        CountryEntity de = countryEntity("DE");
        ChannelEntity channel = channelEntity("dach", Set.of(de));
        when(channelService.getChannel("dach")).thenReturn(channel);

        assertThrows(NotFoundException.class,
                () -> guard.assertCountryAllowedInChannel("dach", "US"),
                "Country not in channel's allowedCountryRefs must throw NotFoundException");
    }

    @Test
    void assertCountryAllowedInChannel_channelWithNoCountryRestrictions_alwaysAllowed() throws NotFoundException {
        // Channel with empty allowedCountryRefs is unrestricted
        ChannelEntity channel = channelEntity("unrestricted", new HashSet<>());
        when(channelService.getChannel("unrestricted")).thenReturn(channel);

        // Should not throw for any country
        guard.assertCountryAllowedInChannel("unrestricted", "US");
        guard.assertCountryAllowedInChannel("unrestricted", "JP");
    }

    @Test
    void assertCountryAllowedInChannel_channelWithNullAllowedCountries_alwaysAllowed() throws NotFoundException {
        ChannelEntity channel = channelEntity("null-restriction", null);
        when(channelService.getChannel("null-restriction")).thenReturn(channel);

        guard.assertCountryAllowedInChannel("null-restriction", "US");
    }

    @Test
    void assertCountryAllowedInChannel_multipleCountries_matchesCorrectOne() throws NotFoundException {
        CountryEntity de = countryEntity("DE");
        CountryEntity at = countryEntity("AT");
        CountryEntity ch = countryEntity("CH");
        ChannelEntity channel = channelEntity("dach", new HashSet<>(Set.of(de, at, ch)));
        when(channelService.getChannel("dach")).thenReturn(channel);

        // All allowed countries should pass
        guard.assertCountryAllowedInChannel("dach", "DE");
        guard.assertCountryAllowedInChannel("dach", "AT");
        guard.assertCountryAllowedInChannel("dach", "CH");
    }

    @Test
    void assertCountryAllowedInChannel_multipleCountries_rejectsUnknownOne() {
        CountryEntity de = countryEntity("DE");
        CountryEntity at = countryEntity("AT");
        ChannelEntity channel = channelEntity("dach", new HashSet<>(Set.of(de, at)));
        when(channelService.getChannel("dach")).thenReturn(channel);

        assertThrows(NotFoundException.class,
                () -> guard.assertCountryAllowedInChannel("dach", "US"),
                "Country not in allowedCountryRefs must be rejected");
    }

    // ---- Helpers ----

    private CountryEntity countryEntity(String isoKey) {
        CountryEntity c = new CountryEntity();
        c.setIsoKey(isoKey);
        return c;
    }

    private ChannelEntity channelEntity(String id, Set<CountryEntity> allowedCountries) {
        ChannelEntity c = new ChannelEntity();
        c.setId(id);
        c.setAllowedCountryRefs(allowedCountries);
        return c;
    }
}
