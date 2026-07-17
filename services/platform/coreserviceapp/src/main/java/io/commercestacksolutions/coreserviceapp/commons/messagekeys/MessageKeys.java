package io.commercestacksolutions.coreserviceapp.commons.messagekeys;

/**
 * Constants for message keys used for i18n translations in the coreserviceapp module.
 */
public class MessageKeys {

    // AppRole errors
    public static final String ERROR_APPROLE_NOT_FOUND = "common.errors.appRole.notFound";
    public static final String ERROR_APPROLE_ALREADY_EXISTS = "common.errors.appRole.alreadyExists";

    // AppPermission errors
    public static final String ERROR_APPPERMISSION_NOT_FOUND = "common.errors.appPermission.notFound";
    public static final String ERROR_APPPERMISSION_ALREADY_EXISTS = "common.errors.appPermission.alreadyExists";

    // Validation errors
    public static final String ERROR_VALIDATION_ID_REQUIRED = "common.errors.validation.idRequired";
    public static final String ERROR_VALIDATION_REQUEST_BODY_EMPTY = "common.errors.validation.requestBodyEmpty";
    public static final String ERROR_VALIDATION_MAX_ITEMS_EXCEEDED = "common.errors.validation.maxItemsExceeded";

    // Data mapping errors
    public static final String ERROR_MAPPING_PATCH_OPERATION = "common.errors.mapping.patchOperation";

    // Data integrity errors
    public static final String ERROR_DATA_INTEGRITY_REFERENCED = "common.errors.dataIntegrity.referenced";

    // Processing errors
    public static final String ERROR_PROCESSING = "common.errors.processing";

    private MessageKeys() {
        // Utility class
    }
}
