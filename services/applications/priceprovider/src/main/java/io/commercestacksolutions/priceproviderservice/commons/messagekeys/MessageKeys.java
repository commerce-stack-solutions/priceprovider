package io.commercestacksolutions.priceproviderservice.commons.messagekeys;

/**
 * Constants for message keys used for i18n translations.
 */
public class MessageKeys {
    
    // Entity not found errors
    public static final String ERROR_ENTITY_NOT_FOUND = "common.errors.entity.notFound";
    public static final String ERROR_GROUP_NOT_FOUND = "common.errors.group.notFound";
    public static final String ERROR_ORGANIZATION_NOT_FOUND = "common.errors.organization.notFound";
    public static final String ERROR_LANGUAGE_NOT_FOUND = "common.errors.language.notFound";
    public static final String ERROR_CURRENCY_NOT_FOUND = "common.errors.currency.notFound";
    public static final String ERROR_UNIT_NOT_FOUND = "common.errors.unit.notFound";
    public static final String ERROR_PRICE_ROW_NOT_FOUND = "common.errors.priceRow.notFound";
    public static final String ERROR_TAX_CLASS_NOT_FOUND = "common.errors.taxClass.notFound";
    public static final String ERROR_COUNTRY_NOT_FOUND = "common.errors.country.notFound";
    public static final String ERROR_CHANNEL_NOT_FOUND = "common.errors.channel.notFound";
    
    // AppRole and AppPermission errors
    public static final String ERROR_APPROLE_NOT_FOUND = "common.errors.appRole.notFound";
    public static final String ERROR_APPROLE_ALREADY_EXISTS = "common.errors.appRole.alreadyExists";
    public static final String ERROR_APPPERMISSION_NOT_FOUND = "common.errors.appPermission.notFound";
    public static final String ERROR_APPPERMISSION_ALREADY_EXISTS = "common.errors.appPermission.alreadyExists";
    
    // Entity already exists errors
    public static final String ERROR_ENTITY_ALREADY_EXISTS = "common.errors.entity.alreadyExists";
    public static final String ERROR_GROUP_ALREADY_EXISTS = "common.errors.group.alreadyExists";
    public static final String ERROR_ORGANIZATION_ALREADY_EXISTS = "common.errors.organization.alreadyExists";
    public static final String ERROR_LANGUAGE_ALREADY_EXISTS = "common.errors.language.alreadyExists";
    public static final String ERROR_CURRENCY_ALREADY_EXISTS = "common.errors.currency.alreadyExists";
    public static final String ERROR_UNIT_ALREADY_EXISTS = "common.errors.unit.alreadyExists";
    public static final String ERROR_TAX_CLASS_ALREADY_EXISTS = "common.errors.taxClass.alreadyExists";
    public static final String ERROR_COUNTRY_ALREADY_EXISTS = "common.errors.country.alreadyExists";
    public static final String ERROR_CHANNEL_ALREADY_EXISTS = "common.errors.channel.alreadyExists";
    
    // Validation errors
    public static final String ERROR_VALIDATION_MANDATORY_FIELD = "common.errors.validation.mandatoryField";
    public static final String ERROR_VALIDATION_INVALID_REFERENCE = "common.errors.validation.invalidReference";
    public static final String ERROR_VALIDATION_ID_REQUIRED = "common.errors.validation.idRequired";
    public static final String ERROR_VALIDATION_PATH_REQUIRED = "common.errors.validation.pathRequired";
    public static final String ERROR_VALIDATION_REQUEST_BODY_EMPTY = "common.errors.validation.requestBodyEmpty";
    public static final String ERROR_VALIDATION_MAX_ITEMS_EXCEEDED = "common.errors.validation.maxItemsExceeded";
    public static final String ERROR_VALIDATION_EMPTY_REQUEST = "common.errors.validation.emptyRequest";
    public static final String ERROR_VALIDATION_LOCALIZED_FIELD_MISSING_LANGUAGE = "common.errors.validation.localizedFieldMissingLanguage";
    public static final String ERROR_LANGUAGE_MANDATORY_MUST_BE_ACTIVE = "common.errors.language.mandatoryMustBeActive";
    
    // Query errors
    public static final String ERROR_QUERY_SYNTAX = "common.errors.query.syntax";

    // Cyclic dependency errors
    public static final String ERROR_ORGANIZATION_CYCLIC_DEPENDENCY = "common.errors.organization.cyclicDependency";
    public static final String ERROR_GROUP_CYCLIC_DEPENDENCY = "common.errors.group.cyclicDependency";
    public static final String ERROR_UNIT_CYCLIC_DEPENDENCY = "common.errors.unit.cyclicDependency";
    
    // Data mapping errors
    public static final String ERROR_MAPPING_ENTITY_NOT_FOUND = "common.errors.mapping.entityNotFound";
    public static final String ERROR_MAPPING_INSTANTIATION = "common.errors.mapping.instantiation";
    public static final String ERROR_MAPPING_PATCH_OPERATION = "common.errors.mapping.patchOperation";
    public static final String ERROR_MAPPING_NO_TARGET_CLASS = "common.errors.mapping.noTargetClass";
    public static final String ERROR_MAPPING_TAX_CLASS_MANDATORY = "common.errors.mapping.taxClassMandatory";
    public static final String ERROR_MAPPING_GENERAL = "common.errors.mapping.general";
    
    // Data integrity errors
    public static final String ERROR_INTEGRITY_CANNOT_DELETE = "common.errors.integrity.cannotDelete";
    public static final String ERROR_DATA_INTEGRITY_REFERENCED = "common.errors.dataIntegrity.referenced";
    public static final String ERROR_DATA_INTEGRITY_REFERENCED_BY_ENTITY = "common.errors.dataIntegrity.referencedByEntity";

    // Channel-Country consistency errors
    public static final String ERROR_VALIDATION_COUNTRY_NOT_IN_CHANNEL = "common.errors.validation.countryNotInChannel";
    public static final String ERROR_VALIDATION_PRICEROW_CHANNEL_COUNTRY_MISMATCH = "common.errors.validation.priceRowChannelCountryMismatch";
    public static final String ERROR_VALIDATION_TAXCLASS_COUNTRY_MANDATORY = "common.errors.validation.taxClassCountryMandatory";

    // Country-Currency validation errors
    public static final String ERROR_VALIDATION_COUNTRY_MUST_HAVE_AT_LEAST_ONE_CURRENCY = "common.errors.validation.countryMustHaveAtLeastOneCurrency";
    public static final String ERROR_VALIDATION_COUNTRY_PRIMARY_CURRENCY_NOT_IN_ALLOWED = "common.errors.validation.countryPrimaryCurrencyNotInAllowed";

    // Processing errors
    public static final String ERROR_PROCESSING = "common.errors.processing";
    public static final String ERROR_APPLYING_PATCH = "common.errors.applyingPatch";
    
    private MessageKeys() {
        // Utility class
    }
}
