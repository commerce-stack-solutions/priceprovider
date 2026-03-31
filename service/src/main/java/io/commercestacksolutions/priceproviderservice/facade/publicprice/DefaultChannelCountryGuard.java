package io.commercestacksolutions.priceproviderservice.facade.publicprice;

import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.service.channel.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Default implementation of {@link ChannelCountryGuard}.
 *
 * <p>Validates that the requested country is in the channel's {@code allowedCountryRefs} set.
 * A channel with an empty {@code allowedCountryRefs} set is treated as unrestricted and accepts
 * any country.</p>
 *
 * <p>This implementation can be replaced by providing a custom Spring bean of type
 * {@link ChannelCountryGuard}. This is useful for testing scenarios, development overrides,
 * or integration with external entitlement systems.</p>
 */
@Component
public class DefaultChannelCountryGuard implements ChannelCountryGuard {

    private final ChannelService channelService;

    @Autowired
    public DefaultChannelCountryGuard(ChannelService channelService) {
        this.channelService = channelService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Looks up the channel by {@code channelId}. If not found, throws {@link NotFoundException}.
     * If the channel has no {@code allowedCountryRefs} (empty or null), the country is considered
     * allowed (unrestricted channel). Otherwise checks whether {@code countryKey} is in the set.</p>
     */
    @Override
    public void assertCountryAllowedInChannel(String channelId, String countryKey) throws NotFoundException {
        ChannelEntity channel = channelService.getChannel(channelId);
        if (channel == null) {
            throw new NotFoundException("Channel not found: " + channelId);
        }
        Set<CountryEntity> allowedCountries = channel.getAllowedCountryRefs();
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            // Channel with no country restrictions — all countries are allowed
            return;
        }
        boolean countryAllowed = allowedCountries.stream()
                .anyMatch(c -> countryKey.equals(c.getIsoKey()));
        if (!countryAllowed) {
            throw new NotFoundException(
                    "Country '" + countryKey + "' is not in the allowed countries for channel '" + channelId + "'. " +
                    "No price can be returned.");
        }
    }
}
