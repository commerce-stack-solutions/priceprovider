package io.commercestacksolutions.priceproviderservice.facade.publicprice;

import io.commercestacksolutions.commons.exception.NotFoundException;

/**
 * Guard interface that determines whether a given country is permitted to be served
 * through a specific sales channel.
 *
 * <p>Implementations of this interface are called by the Public Price API before any
 * price lookup is performed. If the country is not allowed in the requested channel,
 * the guard raises a {@link NotFoundException} (for single-price endpoints) or
 * signals an empty response (for multi-price endpoints via the facade).</p>
 *
 * <h2>Extensibility</h2>
 * <p>The default implementation ({@code DefaultChannelCountryGuard}) uses the
 * channel's {@code allowedCountryRefs} list. Custom implementations can be provided
 * to override this behavior — for example, to allow all countries in development mode,
 * or to delegate to an external entitlement service.</p>
 *
 * <p>To swap the guard, provide a Spring bean of this type in the application context.
 * The {@link PublicPriceFacadeImpl} will auto-wire whichever implementation is present.</p>
 */
public interface ChannelCountryGuard {

    /**
     * Validates that the given {@code countryKey} is permitted in the specified channel.
     *
     * @param channelId  the channel to validate against (e.g., {@code "dach-sales-channel"})
     * @param countryKey the ISO Alpha-2 country code to check (e.g., {@code "DE"})
     * @throws NotFoundException if the channel does not exist or the country is not
     *                           in the channel's allowed country list
     */
    void assertCountryAllowedInChannel(String channelId, String countryKey) throws NotFoundException;
}
