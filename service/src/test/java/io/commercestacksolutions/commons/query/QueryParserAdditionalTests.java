package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    // ====== hasAny / hasAll parser tests ======

    @Test
    public void parse_hasAnySingleValue_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAny:(GROUP_A)");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("groupRefs", filter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ANY, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("GROUP_A"), values);
    }

    @Test
    public void parse_hasAnyMultipleValues_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAny:(GROUP_A,GROUP_B)");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("groupRefs", filter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ANY, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("GROUP_A", "GROUP_B"), values);
    }

    @Test
    public void parse_hasAllMultipleValues_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAll:(GROUP_A,GROUP_B)");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("groupRefs", filter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ALL, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("GROUP_A", "GROUP_B"), values);
    }

    @Test
    public void parse_hasAnyWithSpacesAroundValues_trimsValues() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAny:(GROUP_A, GROUP_B)");
        assertNotNull(expr);
        QueryFilter filter = expr.getFilter();
        assertEquals(QueryFilter.FilterOperator.HAS_ANY, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("GROUP_A", "GROUP_B"), values);
    }

    @Test
    public void parse_hasAnyInComplexExpression_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAny:(GROUP_A,GROUP_B) AND active:true");
        assertNotNull(expr);
        assertFalse(expr.isLeaf());
        assertEquals(QueryExpression.LogicalOperator.AND, expr.getLogicalOperator());
        assertEquals(2, expr.getChildren().size());

        QueryFilter leftFilter = expr.getChildren().get(0).getFilter();
        assertEquals("groupRefs", leftFilter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ANY, leftFilter.getOperator());
    }

    @Test
    public void parse_hasAnyMissingParens_throwsQueryParseException() {
        QueryParser parser = new QueryParser();
        assertThrows(QueryParseException.class, () -> parser.parse("groupRefs.hasAny:GROUP_A,GROUP_B"));
    }

    @Test
    public void parse_hasAnyEmptyList_throwsQueryParseException() {
        QueryParser parser = new QueryParser();
        assertThrows(QueryParseException.class, () -> parser.parse("groupRefs.hasAny:()"));
    }

    @Test
    public void parse_channelRefsHasAll_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("channelRefs.hasAll:(CH1,CH2,CH3)");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("channelRefs", filter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ALL, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("CH1", "CH2", "CH3"), values);
    }

    @Test
    public void parse_hasAnyWithSlashInId_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAny:(DEMO-GROUP-STANDARD/DEMO-GROUP-PREMIUM)");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("groupRefs", filter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ANY, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("DEMO-GROUP-STANDARD/DEMO-GROUP-PREMIUM"), values);
    }

    @Test
    public void parse_hasAnyWithSlashInMultipleIds_parsesCorrectly() throws Exception {
        QueryParser parser = new QueryParser();
        QueryExpression expr = parser.parse("groupRefs.hasAny:(ORG/GROUP-A,ORG/GROUP-B)");
        assertNotNull(expr);
        assertTrue(expr.isLeaf());
        QueryFilter filter = expr.getFilter();
        assertEquals("groupRefs", filter.getField());
        assertEquals(QueryFilter.FilterOperator.HAS_ANY, filter.getOperator());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) filter.getValue();
        assertEquals(List.of("ORG/GROUP-A", "ORG/GROUP-B"), values);
    }
}

