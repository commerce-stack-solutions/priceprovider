package de.ebusyness.commons.query;

import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class QueryParserEntityValidationTest {

    @Test
    public void parse_invalidTaxRate_throwsQueryParseException() {
        QueryParser parser = new QueryParser(TaxClassEntity.class);
        assertThrows(QueryParseException.class, () -> parser.parse("taxRate:not-a-number"));
    }
}

