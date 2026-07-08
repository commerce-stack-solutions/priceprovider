package io.commercestacksolutions.commons.permissionselector;

import io.commercestacksolutions.commons.permissionselector.SelectorTokenizer.Token;
import io.commercestacksolutions.commons.permissionselector.SelectorTokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive descent parser for permission selector expressions.
 *
 * <p>Grammar (with precedence from highest to lowest):
 * <pre>
 * expression    ::= orExpression
 * orExpression  ::= andExpression ( OR andExpression )*
 * andExpression ::= notExpression ( AND notExpression )*
 * notExpression ::= NOT? primary
 * primary       ::= '(' expression ')' | condition
 * condition     ::= field ( operator value | functionCall )
 * functionCall  ::= hasAny '(' valueList ')' | hasAll '(' valueList ')' | isEmpty
 * operator      ::= '==' | '!='
 * valueList     ::= STRING_LITERAL ( ',' STRING_LITERAL )*
 * </pre>
 *
 * <p>Operator precedence (high to low):
 * <ol>
 *   <li>Parentheses: ( ... )</li>
 *   <li>Comparison/functions: ==, !=, hasAny, hasAll, isEmpty</li>
 *   <li>NOT</li>
 *   <li>AND</li>
 *   <li>OR</li>
 * </ol>
 */
public class SelectorParser {

    private final SelectorTokenizer tokenizer = new SelectorTokenizer();
    private List<Token> tokens;
    private int position;

    /**
     * Parses a selector string into an expression tree.
     *
     * @param selectorString the selector expression (e.g., "currencyRef == 'EUR'")
     * @return the parsed expression tree
     * @throws SelectorParseException if parsing fails
     */
    public SelectorExpression parse(String selectorString) {
        this.tokens = tokenizer.tokenize(selectorString);
        this.position = 0;

        if (tokens.isEmpty()) {
            throw new SelectorParseException("Empty selector expression");
        }

        SelectorExpression result = parseOrExpression();

        if (position < tokens.size()) {
            throw new SelectorParseException("Unexpected token after expression: " + currentToken());
        }

        return result;
    }

    // orExpression ::= andExpression ( OR andExpression )*
    private SelectorExpression parseOrExpression() {
        SelectorExpression left = parseAndExpression();

        while (match(TokenType.OR)) {
            List<SelectorExpression> children = new ArrayList<>();
            children.add(left);

            do {
                children.add(parseAndExpression());
            } while (match(TokenType.OR));

            return new SelectorExpression(SelectorExpression.LogicalOperator.OR, children);
        }

        return left;
    }

    // andExpression ::= notExpression ( AND notExpression )*
    private SelectorExpression parseAndExpression() {
        SelectorExpression left = parseNotExpression();

        while (match(TokenType.AND)) {
            List<SelectorExpression> children = new ArrayList<>();
            children.add(left);

            do {
                children.add(parseNotExpression());
            } while (match(TokenType.AND));

            return new SelectorExpression(SelectorExpression.LogicalOperator.AND, children);
        }

        return left;
    }

    // notExpression ::= NOT? primary
    private SelectorExpression parseNotExpression() {
        if (match(TokenType.NOT)) {
            SelectorExpression expr = parsePrimary();
            // Apply negation
            if (expr.isLeaf()) {
                return new SelectorExpression(expr.getCondition(), true);
            } else {
                return new SelectorExpression(expr.getLogicalOperator(), expr.getChildren(), true);
            }
        }

        return parsePrimary();
    }

    // primary ::= '(' expression ')' | condition
    private SelectorExpression parsePrimary() {
        if (match(TokenType.LEFT_PAREN)) {
            SelectorExpression expr = parseOrExpression();
            expect(TokenType.RIGHT_PAREN, "Expected ')' to close grouped expression");
            return expr;
        }

        return parseCondition();
    }

    // condition ::= field ( operator value | functionCall )
    private SelectorExpression parseCondition() {
        if (!check(TokenType.IDENTIFIER)) {
            throw new SelectorParseException("Expected field name, got: " + currentToken());
        }

        String field = advance().text;

        // Check for operators and function calls
        if (match(TokenType.EQUALS)) {
            String value = expectStringLiteral("Expected string value after '=='");
            return new SelectorExpression(new SelectorCondition(field, SelectorOperator.EQUALS, value));
        } else if (match(TokenType.NOT_EQUALS)) {
            String value = expectStringLiteral("Expected string value after '!='");
            return new SelectorExpression(new SelectorCondition(field, SelectorOperator.NOT_EQUALS, value));
        } else if (match(TokenType.HAS_ANY)) {
            List<String> values = parseValueList();
            return new SelectorExpression(new SelectorCondition(field, SelectorOperator.HAS_ANY, values));
        } else if (match(TokenType.HAS_ALL)) {
            List<String> values = parseValueList();
            return new SelectorExpression(new SelectorCondition(field, SelectorOperator.HAS_ALL, values));
        } else if (match(TokenType.IS_EMPTY)) {
            return new SelectorExpression(new SelectorCondition(field, SelectorOperator.IS_EMPTY));
        } else {
            throw new SelectorParseException("Expected operator or function after field '" + field + "', got: " + currentToken());
        }
    }

    // valueList ::= '(' STRING_LITERAL ( ',' STRING_LITERAL )* ')'
    private List<String> parseValueList() {
        expect(TokenType.LEFT_PAREN, "Expected '(' after function name");

        List<String> values = new ArrayList<>();
        values.add(expectStringLiteral("Expected at least one string value in list"));

        while (match(TokenType.COMMA)) {
            values.add(expectStringLiteral("Expected string value after ','"));
        }

        expect(TokenType.RIGHT_PAREN, "Expected ')' to close value list");
        return values;
    }

    // Helper methods

    private Token currentToken() {
        if (position >= tokens.size()) {
            return null;
        }
        return tokens.get(position);
    }

    private boolean check(TokenType type) {
        Token current = currentToken();
        return current != null && current.type == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token advance() {
        if (position >= tokens.size()) {
            throw new SelectorParseException("Unexpected end of expression");
        }
        return tokens.get(position++);
    }

    private void expect(TokenType type, String errorMessage) {
        if (!match(type)) {
            throw new SelectorParseException(errorMessage + ", got: " + currentToken());
        }
    }

    private String expectStringLiteral(String errorMessage) {
        if (!check(TokenType.STRING_LITERAL)) {
            throw new SelectorParseException(errorMessage + ", got: " + currentToken());
        }
        return advance().getStringValue();
    }
}
