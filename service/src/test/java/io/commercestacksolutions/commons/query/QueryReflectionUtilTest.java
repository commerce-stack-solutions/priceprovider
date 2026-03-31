package io.commercestacksolutions.commons.query;

import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class QueryReflectionUtilTest {

    @Test
    public void buildFieldTypeMap_containsTaxRateAsBigDecimal() {
        Map<String, Class<?>> map = QueryReflectionUtil.buildFieldTypeMap(TaxClassEntity.class);
        assertNotNull(map, "Field type map must not be null");
        assertTrue(map.containsKey("taxRate"), "taxRate should be present in field map");
        assertEquals(BigDecimal.class, map.get("taxRate"), "taxRate should be of type BigDecimal");
    }
}

