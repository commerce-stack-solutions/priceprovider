package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryParser focusing on comparison operator validation.
 */
public class QueryParserTest {

    private final QueryParser parser = new QueryParser();

    @Test
    public void parse_invalidDuplicateGreaterThan_throws() {
        QueryParseException ex = assertThrows(QueryParseException.class, () -> parser.parse("priceValue:>>100"));
        assertNotNull(ex.getMessage());
    }

    @Test
    public void parse_invalidDoubleColon_throws() {
        QueryParseException ex = assertThrows(QueryParseException.class, () -> parser.parse("measure::invalid"));
        assertNotNull(ex.getMessage());
    }

    @Test
    public void parse_invalidEmbeddedComparison_throws() {
        QueryParseException ex = assertThrows(QueryParseException.class, () -> parser.parse("priceValue:x>50"));
        assertNotNull(ex.getMessage());
    }

    @Test
    public void parse_invalidEmbeddedComparisonWithEqual_throws() {
        QueryParseException ex = assertThrows(QueryParseException.class, () -> parser.parse("priceValue:val>=100"));
        assertNotNull(ex.getMessage());
    }

    @Test
    public void parse_validGreaterThan_parsesOperator() throws Exception {
        QueryExpression expr = parser.parse("priceValue:>100");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("priceValue", filter.getField());
        assertEquals(QueryFilter.FilterOperator.GREATER_THAN, filter.getOperator());
        assertNotNull(filter.getValue());
    }

    @Test
    public void parse_validGreaterThanOrEqual_parsesOperator() throws Exception {
        QueryExpression expr = parser.parse("priceValue:>=100");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals(QueryFilter.FilterOperator.GREATER_THAN_OR_EQUAL, filter.getOperator());
    }

}
