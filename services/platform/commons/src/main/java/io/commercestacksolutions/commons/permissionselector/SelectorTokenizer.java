package io.commercestacksolutions.commons.permissionselector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes a permission selector string into a stream of tokens.
 *
 * <p>Supported tokens:
 * <ul>
 *   <li>Keywords: AND, OR, NOT, hasAny, hasAll, isEmpty</li>
 *   <li>Operators: ==, !=</li>
 *   <li>Delimiters: (, ), ,</li>
 *   <li>Identifiers: field names</li>
 *   <li>String literals: 'value'</li>
 * </ul>
 */
class SelectorTokenizer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            // String literals (single-quoted)
            "'([^']*)'" +
            // Operators
            "|==|!=" +
            // Keywords (case-sensitive)
            "|\\b(AND|OR|NOT|hasAny|hasAll|isEmpty)\\b" +
            // Delimiters
            "|[(),]" +
            // Identifiers (field names)
            "|\\b[a-zA-Z_][a-zA-Z0-9_]*\\b" +
            // Whitespace (to be skipped)
            "|\\s+"
    );

    public List<Token> tokenize(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new SelectorParseException("Selector string cannot be empty");
        }

        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(input);
        int lastEnd = 0;

        while (matcher.find()) {
            // Check for unrecognized characters between tokens
            if (matcher.start() > lastEnd) {
                String unrecognized = input.substring(lastEnd, matcher.start());
                if (!unrecognized.trim().isEmpty()) {
                    throw new SelectorParseException("Unrecognized character sequence: '" + unrecognized + "'");
                }
            }
            lastEnd = matcher.end();

            String tokenText = matcher.group();

            // Skip whitespace
            if (tokenText.trim().isEmpty()) {
                continue;
            }

            TokenType type = determineTokenType(tokenText, matcher);
            tokens.add(new Token(type, tokenText));
        }

        // Check for any remaining unrecognized characters
        if (lastEnd < input.length()) {
            String remaining = input.substring(lastEnd);
            if (!remaining.trim().isEmpty()) {
                throw new SelectorParseException("Unrecognized character sequence at end: '" + remaining + "'");
            }
        }

        return tokens;
    }

    private TokenType determineTokenType(String tokenText, Matcher matcher) {
        // String literal
        if (matcher.group(1) != null) {
            return TokenType.STRING_LITERAL;
        }

        // Keywords and operators
        switch (tokenText) {
            case "AND": return TokenType.AND;
            case "OR": return TokenType.OR;
            case "NOT": return TokenType.NOT;
            case "hasAny": return TokenType.HAS_ANY;
            case "hasAll": return TokenType.HAS_ALL;
            case "isEmpty": return TokenType.IS_EMPTY;
            case "==": return TokenType.EQUALS;
            case "!=": return TokenType.NOT_EQUALS;
            case "(": return TokenType.LEFT_PAREN;
            case ")": return TokenType.RIGHT_PAREN;
            case ",": return TokenType.COMMA;
        }

        // Must be an identifier (field name)
        if (tokenText.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            return TokenType.IDENTIFIER;
        }

        throw new SelectorParseException("Unexpected token: " + tokenText);
    }

    enum TokenType {
        // Keywords
        AND, OR, NOT,
        HAS_ANY, HAS_ALL, IS_EMPTY,

        // Operators
        EQUALS, NOT_EQUALS,

        // Delimiters
        LEFT_PAREN, RIGHT_PAREN, COMMA,

        // Values
        IDENTIFIER, STRING_LITERAL
    }

    static class Token {
        final TokenType type;
        final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }

        /**
         * Returns the string value from a STRING_LITERAL token (without quotes).
         */
        String getStringValue() {
            if (type != TokenType.STRING_LITERAL) {
                throw new IllegalStateException("Not a string literal: " + type);
            }
            // Remove surrounding quotes
            return text.substring(1, text.length() - 1);
        }

        @Override
        public String toString() {
            return type + "(" + text + ")";
        }
    }
}
