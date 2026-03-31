package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QueryParserAdditionalTests {

    @Test
    public void parse_taxRateEquals_parsesSuccessfully() throws Exception {
        QueryParser parser = new QueryParser(TaxClassEntity.class);
        QueryExpression expr = parser.parse("taxRate:0.19");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("taxRate", filter.getField());
        assertEquals(QueryFilter.FilterOperator.EQUALS, filter.getOperator());
        assertNotNull(filter.getValue());
        assertTrue(filter.getValue() instanceof Number);
    }

    @Test
    public void parse_taxRateGreaterThan_parsesSuccessfully() throws Exception {
        QueryParser parser = new QueryParser(TaxClassEntity.class);
        QueryExpression expr = parser.parse("taxRate:>0.15");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("taxRate", filter.getField());
        assertEquals(QueryFilter.FilterOperator.GREATER_THAN, filter.getOperator());
        assertNotNull(filter.getValue());
    }

    @Test
    public void parse_taxRateRangeOpenUpper_unboundedParses() throws Exception {
        QueryParser parser = new QueryParser(TaxClassEntity.class);
        QueryExpression expr = parser.parse("taxRate:[0.1 TO *]");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals(QueryFilter.FilterOperator.RANGE, filter.getOperator());
        Object[] range = (Object[]) filter.getValue();
        assertNotNull(range);
        assertNotNull(range[0]);
        assertNull(range[1]);
    }

    @Test
    public void parse_taxClassIdWildcard_containsOperator() throws Exception {
        QueryParser parser = new QueryParser(TaxClassEntity.class);
        QueryExpression expr = parser.parse("taxClassId:TAX*");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals(QueryFilter.FilterOperator.CONTAINS, filter.getOperator());
        assertEquals("TAX", filter.getValue());
    }

    @Test
    public void parse_existsTrue_parsesExistsOperator() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("someField.exists:true");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals(QueryFilter.FilterOperator.EXISTS, filter.getOperator());
        assertEquals("someField", filter.getField());
    }

    @Test
    public void parse_malformedRange_throwsQueryParseException() {
        QueryParser parser = new QueryParser(TaxClassEntity.class);
        assertThrows(QueryParseException.class, () -> parser.parse("taxRate:[0.1 TO]"));
    }
}

