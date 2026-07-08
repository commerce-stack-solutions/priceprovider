package io.commercestacksolutions.priceproviderservice.service.pricerow.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_VALIDATION_PRICEROW_CHANNEL_COUNTRY_MISMATCH;

/**
 * Validation rule: if a price row is assigned to channels, the tax class's country
 * must be included in the allowed countries of every referenced channel.
 *
 * <p>This enforces the business rule that a price row can only exist in a channel
 * if the country it belongs to (via its tax class) is permitted by that channel.</p>
 *
 * <p>Price rows without a channel assignment (generic prices) or tax classes without
 * a country reference are exempt from this check.</p>
 */
@Component
public class ChannelCountryConsistencyRule implements ValidationRule<PriceRowEntity> {

    @Override
    public List<Message> validate(PriceRowEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        Set<ChannelEntity> channels = entity.getChannels();
        if (channels == null || channels.isEmpty()) {
            // Generic price row (no channels) — no restriction applies
            return Collections.emptyList();
        }

        TaxClassEntity taxClass = entity.getTaxClass();
        if (taxClass == null) {
            return Collections.emptyList();
        }

        CountryEntity taxClassCountry = taxClass.getCountry();
        if (taxClassCountry == null || taxClassCountry.getIsoKey() == null) {
            // Tax class without a country — no country restriction to check
            return Collections.emptyList();
        }

        String countryIsoKey = taxClassCountry.getIsoKey();
        List<Message> errors = new ArrayList<>();

        for (ChannelEntity channel : channels) {
            if (channel == null || channel.getId() == null) {
                continue;
            }

            Set<CountryEntity> allowedCountries = channel.getAllowedCountryRefs();
            if (allowedCountries == null || allowedCountries.isEmpty()) {
                // Channel with no country restriction — skip
                continue;
            }

            Set<String> allowedIsoKeys = allowedCountries.stream()
                    .map(CountryEntity::getIsoKey)
                    .collect(Collectors.toSet());

            if (!allowedIsoKeys.contains(countryIsoKey)) {
                Map<String, String> params = new HashMap<>();
                params.put("channelId", channel.getId());
                params.put("countryIsoKey", countryIsoKey);
                params.put("allowedCountries", String.join(", ", allowedIsoKeys));

                errors.add(new Message(
                        Message.MessageType.ERROR,
                        ERROR_VALIDATION_PRICEROW_CHANNEL_COUNTRY_MISMATCH,
                        params,
                        List.of("channelRefs", "taxClassRef")
                ));
            }
        }

        return errors;
    }
}
