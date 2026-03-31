package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.query.messagekeys.MessageKeys;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/**
 * Parses Lucene-like search query syntax into QueryExpression objects.
 * 
 * Supported syntax:
 * - field:value - Exact match or contains (for strings)
 * - field:[min TO max] - Range queries
 * - field.exists:true/false - Existence checks
 * - AND, OR - Logical operators
 * - NOT - Negation
 * - (expression) - Grouping with parentheses
 * 
 * Examples:
 * - name:john
 * - age:[18 TO 65]
 * - active:true AND role:admin
 * - (status:active OR status:pending) AND NOT deleted:true
 * - createdAt:[2024-01-01T00:00:00Z TO 2024-12-31T23:59:59Z]
 */
public class QueryParser {
    
    private static final int MAX_DEPTH = 10;
    private static final Pattern FIELD_VALUE_PATTERN = Pattern.compile("([a-zA-Z0-9_.]+):(.+)");
    // Fixed regex to prevent ReDoS - use possessive quantifier and limit space matching
    private static final Pattern RANGE_PATTERN = Pattern.compile("\\[(.+?)\\s++TO\\s++(.+?)\\]");

    // Instanz-spezifische Feldtyp-Map (konfigurierbar) - leer per Default
    private final Map<String, Class<?>> fieldTypes;

    private String query;
    private int position;
    private int depth;

    // No-arg constructor beibehaltet bisherigen Default (keine Typvalidierung)
    public QueryParser() {
        this.fieldTypes = Collections.emptyMap();
    }

    // Konstruktor zum Injizieren einer Feldtyp-Map
    public QueryParser(Map<String, Class<?>> fieldTypes) {
        this.fieldTypes = fieldTypes != null ? new HashMap<>(fieldTypes) : Collections.emptyMap();
    }

    // Convenience-Konstruktor: ermittelt Feldtypen per Reflection aus der Entity-Klasse
    public QueryParser(Class<?> entityClass) {
        this(QueryReflectionUtil.buildFieldTypeMap(entityClass));
    }

    /**
     * Parses a query string into a QueryExpression tree.
     * 
     * @param query the query string to parse
     * @return the parsed QueryExpression
     * @throws QueryParseException if the query syntax is invalid
     */
    public QueryExpression parse(String query) throws QueryParseException {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        
        this.query = sanitize(query.trim());
        this.position = 0;
        this.depth = 0;
        
        return parseExpression();
    }
    
    /**
     * Sanitizes input to prevent injection attacks.
     */
    private String sanitize(String input) {
        // Remove potentially dangerous characters while keeping query syntax
        // Allow: Unicode letters/numbers, spaces, :, *, [, ], (, ), -, _, ., T, Z (for ISO dates), <, >, = and currency symbols
        // \p{L} = any letter, \p{N} = any number, \p{Sc} = currency symbols
        return input.replaceAll("[^\\p{L}\\p{N}\\s:\\*\\[\\]\\(\\)\\-_.TZ<>=\\p{Sc}]", "");
    }
    
    /**
     * Parses a top-level expression (handles AND/OR operators).
     */
    private QueryExpression parseExpression() throws QueryParseException {
        if (depth++ > MAX_DEPTH) {
            throw new QueryParseException(MessageKeys.ERROR_QUERY_NESTING_DEPTH, Collections.singletonMap("max", String.valueOf(MAX_DEPTH)));
        }
        
        try {
            QueryExpression left = parseTerm();
            
            while (position < query.length()) {
                skipWhitespace();
                
                if (position >= query.length() || peek() == ')') {
                    break;
                }
                
                // Check for logical operators
                if (matchKeyword("AND")) {
                    skipWhitespace();
                    QueryExpression right = parseTerm();
                    left = new QueryExpression(QueryExpression.LogicalOperator.AND, List.of(left, right));
                } else if (matchKeyword("OR")) {
                    skipWhitespace();
                    QueryExpression right = parseTerm();
                    left = new QueryExpression(QueryExpression.LogicalOperator.OR, List.of(left, right));
                } else {
                    break;
                }
            }
            
            return left;
        } finally {
            depth--;
        }
    }
    
    /**
     * Parses a term (handles NOT and parenthesized expressions).
     */
    private QueryExpression parseTerm() throws QueryParseException {
        skipWhitespace();
        
        // Handle NOT
        boolean negated = false;
        if (matchKeyword("NOT")) {
            negated = true;
            skipWhitespace();
        }
        
        // Handle parenthesized expression
        if (peek() == '(') {
            position++; // consume '('
            QueryExpression expr = parseExpression();
            skipWhitespace();
            if (position >= query.length() || peek() != ')') {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_MISSING_CLOSING_PAREN, Collections.singletonMap("pos", String.valueOf(position)));
            }
            position++; // consume ')'
            
            return negated ? new QueryExpression(QueryExpression.LogicalOperator.AND, List.of(expr), true) : expr;
        }
        
        // Parse field:value filter
        QueryFilter filter = parseFilter();
        return new QueryExpression(filter, negated);
    }
    
    /**
     * Parses a field:value filter.
     */
    private QueryFilter parseFilter() throws QueryParseException {
        skipWhitespace();
        
        // Extract the field:value portion
        int start = position;
        while (position < query.length()) {
            char c = query.charAt(position);
            if (c == ' ' && !isInsideRange()) {
                // Check if next word is AND or OR
                int lookahead = position + 1;
                while (lookahead < query.length() && query.charAt(lookahead) == ' ') {
                    lookahead++;
                }
                String nextWord = extractWord(lookahead);
                if ("AND".equals(nextWord) || "OR".equals(nextWord) || ")".equals(nextWord)) {
                    break;
                }
            }
            if (c == ')' && !isInsideRange()) {
                break;
            }
            position++;
        }
        
        String filterStr = query.substring(start, position).trim();
        return parseFilterString(filterStr);
    }
    
    /**
     * Checks if we're currently inside a range expression [... TO ...].
     */
    private boolean isInsideRange() {
        int bracketCount = 0;
        for (int i = position - 1; i >= 0; i--) {
            char c = query.charAt(i);
            if (c == ']') bracketCount++;
            if (c == '[') bracketCount--;
            if (bracketCount < 0) return true; // We're inside an unclosed '['
        }
        return false;
    }
    
    /**
     * Parses a filter string like "field:value" or "field:[min TO max]".
     */
    private QueryFilter parseFilterString(String filterStr) throws QueryParseException {
        Matcher matcher = FIELD_VALUE_PATTERN.matcher(filterStr);
        if (!matcher.matches()) {
            throw new QueryParseException(MessageKeys.ERROR_QUERY_INVALID_FILTER, Collections.singletonMap("filter", filterStr));
        }
        
        String field = matcher.group(1);
        String valueStr = matcher.group(2);

        // Reject embedded comparison operator characters that are not used as a leading operator
        // Examples to reject: "x>50", "10<5", "a>=1" (operator must be a prefix if present)
        if (valueStr.indexOf('>') != -1 || valueStr.indexOf('<') != -1) {
            if (!(valueStr.startsWith(">=") || valueStr.startsWith("<=") || valueStr.startsWith(">") || valueStr.startsWith("<"))) {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_INVALID_OPERATOR, Collections.singletonMap("filter", filterStr));
            }
        }

        // Detect invalid operator tokens like a second colon immediately after field (e.g. "measure::invalid")
        if (valueStr.startsWith(":")) {
            throw new QueryParseException(MessageKeys.ERROR_QUERY_INVALID_OPERATOR, Collections.singletonMap("filter", filterStr));
        }

        // Check for existence query (field.exists:true/false)
        if (field.endsWith(".exists")) {
            String actualField = field.substring(0, field.length() - 7); // Remove ".exists"
            boolean exists = "true".equalsIgnoreCase(valueStr);
            return new QueryFilter(actualField, 
                exists ? QueryFilter.FilterOperator.EXISTS : QueryFilter.FilterOperator.NOT_EXISTS, 
                null);
        }
        
        // Check for range query [min TO max]
        // Explicitly validate range syntax to ensure malformed ranges are rejected as client errors
        if (valueStr.startsWith("[")) {
            // Expect a closing bracket and the 'TO' keyword
            if (!valueStr.endsWith("]") || !valueStr.toUpperCase().contains(" TO ")) {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_SYNTAX, Collections.singletonMap("filter", filterStr));
            }

            // Extract inner content and split on 'TO' surrounded by whitespace (case-insensitive)
            String inner = valueStr.substring(1, valueStr.length() - 1);
            String[] parts = inner.split("(?i)\\s+TO\\s+");
            if (parts.length != 2) {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_SYNTAX, Collections.singletonMap("filter", filterStr));
            }

            String min = parts[0].trim();
            String max = parts[1].trim();

            // Allow '*' to indicate unbounded; reject truly empty bounds (e.g. [10 TO] or [ TO 100])
            if (min.isEmpty() || max.isEmpty()) {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_SYNTAX, Collections.singletonMap("filter", filterStr));
            }

            // Validate bounds against expected field type when applicable
            try {
                Class<?> expected = fieldTypes.get(field);
                if (expected != null) {
                    if (!"*".equals(min)) validateValueType(field, min, expected);
                    if (!"*".equals(max)) validateValueType(field, max, expected);
                }
            } catch (InvalidParameterException ipe) {
                // wrap into QueryParseException so callers expecting QueryParseException keep behavior
                throw new QueryParseException(ipe.getMessage(), Collections.singletonMap("filter", filterStr), ipe);
            }

            // Interpret '*' as unbounded (null)
            Object minVal = "*".equals(min) ? null : parseValue(min);
            Object maxVal = "*".equals(max) ? null : parseValue(max);

            Object[] range = new Object[2];
            range[0] = minVal;
            range[1] = maxVal;

             return new QueryFilter(field, QueryFilter.FilterOperator.RANGE, range);
        }

        // Check for comparison operators
        // Validate comparison operator usage: allowed prefixes are ">=", "<=", ">", "<" only once
        if (valueStr.startsWith(">=") || valueStr.startsWith("<=") || valueStr.startsWith(">") || valueStr.startsWith("<")) {
            // Detect duplicated operators like ">>100" or ">=>100" by checking if after removing a single
            // operator another operator symbol immediately follows (no digit/letter)
            String rest;
            String op;
            if (valueStr.startsWith(">=")) {
                op = ">=";
                rest = valueStr.substring(2).trim();
            } else if (valueStr.startsWith("<=")) {
                op = "<=";
                rest = valueStr.substring(2).trim();
            } else if (valueStr.startsWith(">")) {
                op = ">";
                rest = valueStr.substring(1).trim();
            } else { // startsWith("<")
                op = "<";
                rest = valueStr.substring(1).trim();
            }

            // If rest starts with any comparison char, it's invalid/duplicate (e.g. ">>100", ">= >100")
            if (rest.startsWith(">") || rest.startsWith("<") || rest.startsWith("=>") || rest.startsWith("=<")) {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_INVALID_OPERATOR, Collections.singletonMap("operator", op));
            }

            // Ensure rest is non-empty and not an invalid operator token (like ":" or "::")
            if (rest.isEmpty() || rest.startsWith(":")) {
                throw new QueryParseException(MessageKeys.ERROR_QUERY_INVALID_OPERATOR, Collections.singletonMap("operator", op));
            }

            // Validate type for comparison value when applicable
            try {
                Class<?> expected = fieldTypes.get(field);
                if (expected != null) {
                    validateValueType(field, rest, expected);
                }
            } catch (InvalidParameterException ipe) {
                throw new QueryParseException(ipe.getMessage(), Collections.singletonMap("filter", filterStr), ipe);
            }

            // Map to appropriate operator
            switch (op) {
                case ">=":
                    return new QueryFilter(field, QueryFilter.FilterOperator.GREATER_THAN_OR_EQUAL, parseValue(rest));
                case "<=":
                    return new QueryFilter(field, QueryFilter.FilterOperator.LESS_THAN_OR_EQUAL, parseValue(rest));
                case ">":
                    return new QueryFilter(field, QueryFilter.FilterOperator.GREATER_THAN, parseValue(rest));
                case "<":
                default:
                    return new QueryFilter(field, QueryFilter.FilterOperator.LESS_THAN, parseValue(rest));
            }
        }
        
        // Default to equals/contains
        // Validate single value type when applicable
        try {
            Class<?> expected = fieldTypes.get(field);
            if (expected != null) {
                // For wildcard strings (contains) we allow '*' and skip numeric validation
                if (!valueStr.contains("*") && !"*".equals(valueStr)) {
                    validateValueType(field, valueStr, expected);
                }
            }
        } catch (InvalidParameterException ipe) {
            throw new QueryParseException(ipe.getMessage(), Collections.singletonMap("filter", filterStr), ipe);
        }

        Object value = parseValue(valueStr);
        
        // Use CONTAINS for strings with wildcards, EQUALS otherwise
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.contains("*")) {
                // Remove wildcards and use CONTAINS
                value = strValue.replace("*", "");
                return new QueryFilter(field, QueryFilter.FilterOperator.CONTAINS, value);
            }
        }
        
        return new QueryFilter(field, QueryFilter.FilterOperator.EQUALS, value);
    }

    /**
     * Validates that the provided string can be converted to the expected target type.
     * Throws InvalidParameterException when conversion fails.
     */
    private void validateValueType(String field, String valueStr, Class<?> expectedType) throws InvalidParameterException {
        if (valueStr == null || valueStr.isEmpty() || "*".equals(valueStr)) return;

        try {
            if (expectedType == BigDecimal.class) {
                // BigDecimal accepts formats like 0.19, 1, .5 is accepted by BigDecimal but we keep default
                new BigDecimal(valueStr);
            } else if (expectedType == Long.class || expectedType == long.class) {
                Long.parseLong(valueStr);
            } else if (expectedType == Integer.class || expectedType == int.class) {
                Integer.parseInt(valueStr);
            } else if (expectedType == Boolean.class || expectedType == boolean.class) {
                if (!"true".equalsIgnoreCase(valueStr) && !"false".equalsIgnoreCase(valueStr)) {
                    throw new NumberFormatException("Not a boolean");
                }
            } else if (expectedType == java.time.OffsetDateTime.class) {
                OffsetDateTime.parse(valueStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
        } catch (Exception ex) {
            Map<String, String> params = Map.of(
                    "field", field,
                    "expectedType", expectedType == BigDecimal.class ? "decimal" : expectedType.getSimpleName().toLowerCase(),
                    "actualValue", valueStr
            );
            throw new InvalidParameterException(MessageKeys.ERROR_QUERY_INVALID_VALUE_TYPE, params, List.of(field));
        }
    }

    private Object parseValue(String valueStr) {
        if (valueStr == null || valueStr.isEmpty() || "*".equals(valueStr)) {
            return null;
        }
        
        // Try boolean
        if ("true".equalsIgnoreCase(valueStr)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(valueStr)) {
            return Boolean.FALSE;
        }
        
        // Try ISO date/time
        if (valueStr.contains("T") && (valueStr.contains("Z") || valueStr.contains("+") || valueStr.contains("-"))) {
            try {
                return OffsetDateTime.parse(valueStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                // Not a valid date, treat as string
            }
        }
        
        // Try number
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Long.parseLong(valueStr);
            }
        } catch (NumberFormatException e) {
            // Not a number, treat as string
        }
        
        return valueStr;
    }
    
    private void skipWhitespace() {
        while (position < query.length() && Character.isWhitespace(query.charAt(position))) {
            position++;
        }
    }
    
    private char peek() {
        return position < query.length() ? query.charAt(position) : '\0';
    }
    
    private boolean matchKeyword(String keyword) {
        skipWhitespace();
        if (position + keyword.length() > query.length()) {
            return false;
        }
        
        String substr = query.substring(position, position + keyword.length());
        if (substr.equals(keyword)) {
            // Make sure it's followed by whitespace or end of string
            int nextPos = position + keyword.length();
            if (nextPos >= query.length() || Character.isWhitespace(query.charAt(nextPos)) || query.charAt(nextPos) == '(') {
                position += keyword.length();
                return true;
            }
        }
        return false;
    }
    
    private String extractWord(int fromPos) {
        int start = fromPos;
        while (fromPos < query.length() && !Character.isWhitespace(query.charAt(fromPos)) && query.charAt(fromPos) != ')') {
            fromPos++;
        }
        return query.substring(start, fromPos);
    }

}
