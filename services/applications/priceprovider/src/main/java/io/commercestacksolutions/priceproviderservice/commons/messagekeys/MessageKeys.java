package io.commercestacksolutions.priceproviderservice.commons.messagekeys;

/**
 * Constants for message keys used for i18n translations in the priceprovider module.
 */
public class MessageKeys {

    // PriceRow errors
    public static final String ERROR_PRICE_ROW_NOT_FOUND = "common.errors.priceRow.notFound";

    // Validation errors
    public static final String ERROR_VALIDATION_EMPTY_REQUEST = "common.errors.validation.emptyRequest";
    public static final String ERROR_VALIDATION_MAX_ITEMS_EXCEEDED = "common.errors.validation.maxItemsExceeded";
    public static final String ERROR_VALIDATION_PRICEROW_CHANNEL_COUNTRY_MISMATCH = "common.errors.validation.priceRowChannelCountryMismatch";

    // Data mapping errors
    public static final String ERROR_MAPPING_ENTITY_NOT_FOUND = "common.errors.mapping.entityNotFound";
    public static final String ERROR_MAPPING_PATCH_OPERATION = "common.errors.mapping.patchOperation";
    public static final String ERROR_MAPPING_TAX_CLASS_MANDATORY = "common.errors.mapping.taxClassMandatory";

    // Data integrity errors
    public static final String ERROR_DATA_INTEGRITY_REFERENCED = "common.errors.dataIntegrity.referenced";

    // Processing errors
    public static final String ERROR_PROCESSING = "common.errors.processing";
    public static final String ERROR_APPLYING_PATCH = "common.errors.applyingPatch";

    private MessageKeys() {
        // Utility class
    }
}
