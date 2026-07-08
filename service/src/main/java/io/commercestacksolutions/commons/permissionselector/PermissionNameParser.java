package io.commercestacksolutions.commons.permissionselector;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses permission names to extract datatype, selector, and action components.
 *
 * <p>Permission name format:
 * <ul>
 *   <li>Without selector: {@code <prefix>:<DataType|Capability>:<Action>}</li>
 *   <li>With selector: {@code <prefix>:<DataType>[<selector>]:<Action>}</li>
 * </ul>
 *
 * <p>Supported prefixes: {@code priceprovider.admin}, {@code priceprovider.public}, or any custom prefix
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code priceprovider.admin:PriceRow:read} - Global permission</li>
 *   <li>{@code priceprovider.admin:PriceRow[currencyRef=='EUR']:read} - Selector-based permission</li>
 *   <li>{@code priceprovider.public:PriceRow:read} - Public API permission</li>
 *   <li>{@code priceprovider.admin:ServiceInitialization:write} - Capability permission</li>
 * </ul>
 */
public class PermissionNameParser {

    // Pattern: <prefix>:<DataType>(<OptionalSelector>):<Action>
    // The prefix is everything before the first ":" followed by the DataType
    // Example: priceprovider.admin:PriceRow[currencyRef=='EUR']:read
    // Example: priceprovider.public:PriceRow:read
    private static final Pattern PERMISSION_PATTERN = Pattern.compile(
            "^(.+?):" +                         // Prefix (anything before first :)
            "([A-Za-z][A-Za-z0-9]*)" +          // DataType or Capability
            "(?:\\[(.+?)\\])?" +                // Optional selector in [...]
            ":([a-z]+)" +                       // Action
            "$"
    );

    /**
     * Parses a permission name into its components.
     *
     * @param permissionName the full permission name
     * @return parsed permission components
     * @throws IllegalArgumentException if the permission name format is invalid
     */
    public ParsedPermission parse(String permissionName) {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name cannot be empty");
        }

        Matcher matcher = PERMISSION_PATTERN.matcher(permissionName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid permission name format: " + permissionName);
        }

        String prefix = matcher.group(1);
        String dataType = matcher.group(2);
        String selectorString = matcher.group(3); // May be null
        String action = matcher.group(4);

        SelectorExpression selector = null;
        if (selectorString != null && !selectorString.trim().isEmpty()) {
            try {
                SelectorParser parser = new SelectorParser();
                selector = parser.parse(selectorString);
            } catch (SelectorParseException e) {
                throw new IllegalArgumentException("Invalid selector in permission '" + permissionName + "': " + e.getMessage(), e);
            }
        }

        return new ParsedPermission(permissionName, prefix, dataType, selector, action);
    }

    /**
     * Validates that a permission name is well-formed.
     *
     * @param permissionName the permission name to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String permissionName) {
        try {
            parse(permissionName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a permission name has a selector.
     *
     * @param permissionName the permission name
     * @return true if the permission has a selector, false otherwise
     */
    public boolean hasSelector(String permissionName) {
        try {
            ParsedPermission parsed = parse(permissionName);
            return parsed.getSelector() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Represents a parsed permission name with its components.
     */
    public static class ParsedPermission {
        private final String fullName;
        private final String prefix;
        private final String dataType;
        private final SelectorExpression selector;
        private final String action;

        public ParsedPermission(String fullName, String prefix, String dataType, SelectorExpression selector, String action) {
            this.fullName = Objects.requireNonNull(fullName);
            this.prefix = Objects.requireNonNull(prefix);
            this.dataType = Objects.requireNonNull(dataType);
            this.selector = selector; // Nullable
            this.action = Objects.requireNonNull(action);
        }

        /**
         * Returns the full permission name.
         */
        public String getFullName() {
            return fullName;
        }

        /**
         * Returns the permission prefix (e.g., "priceprovider.admin", "priceprovider.public").
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Returns the data type or capability (e.g., "PriceRow", "ServiceInitialization").
         */
        public String getDataType() {
            return dataType;
        }

        /**
         * Returns the parsed selector expression, or null if no selector.
         */
        public SelectorExpression getSelector() {
            return selector;
        }

        /**
         * Returns true if this permission has a selector.
         */
        public boolean hasSelector() {
            return selector != null;
        }

        /**
         * Returns the action (e.g., "read", "write", "delete").
         */
        public String getAction() {
            return action;
        }

        /**
         * Checks if this permission matches a given datatype and action.
         * Does not consider the selector.
         */
        public boolean matchesTypeAndAction(String targetDataType, String targetAction) {
            return this.dataType.equals(targetDataType) && this.action.equals(targetAction);
        }

        @Override
        public String toString() {
            return fullName;
        }
    }
}
