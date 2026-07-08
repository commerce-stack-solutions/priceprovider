package io.commercestacksolutions.priceproviderservice.service.country.validation;

import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_VALIDATION_COUNTRY_MUST_HAVE_AT_LEAST_ONE_CURRENCY;
import static io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys.ERROR_VALIDATION_COUNTRY_PRIMARY_CURRENCY_NOT_IN_ALLOWED;

/**
 * Validation rule: ensures that a country has at least one allowed currency,
 * and that the primary currency (if set) is one of the allowed currencies.
 *
 * <p>This enforces the business rule that:</p>
 * <ul>
 *   <li>Every country must have at least one allowed currency</li>
 *   <li>If a primary currency is specified, it must be in the allowed currencies list</li>
 * </ul>
 */
@Component
public class CountryCurrencyConsistencyRule implements ValidationRule<CountryEntity> {

    @Override
    public List<Message> validate(CountryEntity entity) {
        if (entity == null) {
            return Collections.emptyList();
        }

        List<Message> errors = new ArrayList<>();

        Set<CurrencyEntity> allowedCurrencies = entity.getAllowedCurrencyRefs();
        CurrencyEntity primaryCurrency = entity.getPrimaryCurrencyRef();

        // Rule 1: At least one currency must be defined
        if (allowedCurrencies == null || allowedCurrencies.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("countryIsoKey", entity.getIsoKey());

            errors.add(new Message(
                    Message.MessageType.ERROR,
                    ERROR_VALIDATION_COUNTRY_MUST_HAVE_AT_LEAST_ONE_CURRENCY,
                    params,
                    List.of("allowedCurrencyRefs")
            ));
        }

        // Rule 2: Primary currency must be in allowed currencies (if primary is set)
        if (primaryCurrency != null && primaryCurrency.getCurrencyKey() != null) {
            if (allowedCurrencies == null || allowedCurrencies.isEmpty()) {
                // Already reported in Rule 1, no need to duplicate
                return errors;
            }

            Set<String> allowedCurrencyKeys = allowedCurrencies.stream()
                    .map(CurrencyEntity::getCurrencyKey)
                    .collect(Collectors.toSet());

            if (!allowedCurrencyKeys.contains(primaryCurrency.getCurrencyKey())) {
                Map<String, String> params = new HashMap<>();
                params.put("countryIsoKey", entity.getIsoKey());
                params.put("primaryCurrency", primaryCurrency.getCurrencyKey());
                params.put("allowedCurrencies", String.join(", ", allowedCurrencyKeys));

                errors.add(new Message(
                        Message.MessageType.ERROR,
                        ERROR_VALIDATION_COUNTRY_PRIMARY_CURRENCY_NOT_IN_ALLOWED,
                        params,
                        List.of("primaryCurrencyRef", "allowedCurrencyRefs")
                ));
            }
        }

        return errors;
    }
}
