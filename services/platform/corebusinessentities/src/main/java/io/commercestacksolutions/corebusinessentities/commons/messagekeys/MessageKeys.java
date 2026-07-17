package io.commercestacksolutions.corebusinessentities.commons.messagekeys;

/**
 * Constants for message keys used for i18n translations in the corebusinessentities module.
 */
public class MessageKeys {

    // Language errors
    public static final String ERROR_LANGUAGE_NOT_FOUND = "common.errors.language.notFound";
    public static final String ERROR_LANGUAGE_ALREADY_EXISTS = "common.errors.language.alreadyExists";
    public static final String ERROR_LANGUAGE_MANDATORY_MUST_BE_ACTIVE = "common.errors.language.mandatoryMustBeActive";

    // Currency errors
    public static final String ERROR_CURRENCY_NOT_FOUND = "common.errors.currency.notFound";
    public static final String ERROR_CURRENCY_ALREADY_EXISTS = "common.errors.currency.alreadyExists";

    // Country errors
    public static final String ERROR_COUNTRY_NOT_FOUND = "common.errors.country.notFound";
    public static final String ERROR_COUNTRY_ALREADY_EXISTS = "common.errors.country.alreadyExists";

    // Channel errors
    public static final String ERROR_CHANNEL_NOT_FOUND = "common.errors.channel.notFound";
    public static final String ERROR_CHANNEL_ALREADY_EXISTS = "common.errors.channel.alreadyExists";

    // Group errors
    public static final String ERROR_GROUP_NOT_FOUND = "common.errors.group.notFound";
    public static final String ERROR_GROUP_ALREADY_EXISTS = "common.errors.group.alreadyExists";
    public static final String ERROR_GROUP_CYCLIC_DEPENDENCY = "common.errors.group.cyclicDependency";

    // Organization errors
    public static final String ERROR_ORGANIZATION_NOT_FOUND = "common.errors.organization.notFound";
    public static final String ERROR_ORGANIZATION_ALREADY_EXISTS = "common.errors.organization.alreadyExists";
    public static final String ERROR_ORGANIZATION_CYCLIC_DEPENDENCY = "common.errors.organization.cyclicDependency";

    // TaxClass errors
    public static final String ERROR_TAX_CLASS_NOT_FOUND = "common.errors.taxClass.notFound";
    public static final String ERROR_TAX_CLASS_ALREADY_EXISTS = "common.errors.taxClass.alreadyExists";

    // Unit errors
    public static final String ERROR_UNIT_NOT_FOUND = "common.errors.unit.notFound";
    public static final String ERROR_UNIT_ALREADY_EXISTS = "common.errors.unit.alreadyExists";
    public static final String ERROR_UNIT_CYCLIC_DEPENDENCY = "common.errors.unit.cyclicDependency";

    // Validation errors
    public static final String ERROR_VALIDATION_MANDATORY_FIELD = "common.errors.validation.mandatoryField";
    public static final String ERROR_VALIDATION_INVALID_REFERENCE = "common.errors.validation.invalidReference";
    public static final String ERROR_VALIDATION_ID_REQUIRED = "common.errors.validation.idRequired";
    public static final String ERROR_VALIDATION_PATH_REQUIRED = "common.errors.validation.pathRequired";
    public static final String ERROR_VALIDATION_REQUEST_BODY_EMPTY = "common.errors.validation.requestBodyEmpty";
    public static final String ERROR_VALIDATION_MAX_ITEMS_EXCEEDED = "common.errors.validation.maxItemsExceeded";
    public static final String ERROR_VALIDATION_EMPTY_REQUEST = "common.errors.validation.emptyRequest";
    public static final String ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE = "common.errors.validation.localizedFieldMissingLanguage";
    public static final String ERROR_VALIDATION_COUNTRY_MUST_HAVE_AT_LEAST_ONE_CURRENCY = "common.errors.validation.countryMustHaveAtLeastOneCurrency";
    public static final String ERROR_VALIDATION_COUNTRY_PRIMARY_CURRENCY_NOT_IN_ALLOWED = "common.errors.validation.countryPrimaryCurrencyNotInAllowed";

    // Data mapping errors
    public static final String ERROR_MAPPING_ENTITY_NOT_FOUND = "common.errors.mapping.entityNotFound";
    public static final String ERROR_MAPPING_PATCH_OPERATION = "common.errors.mapping.patchOperation";

    // Data integrity errors
    public static final String ERROR_DATA_INTEGRITY_REFERENCED = "common.errors.dataIntegrity.referenced";
    public static final String ERROR_DATA_INTEGRITY_REFERENCED_BY_ENTITY = "common.errors.dataIntegrity.referencedByEntity";

    // Processing errors
    public static final String ERROR_PROCESSING = "common.errors.processing";

    private MessageKeys() {
        // Utility class
    }
}
