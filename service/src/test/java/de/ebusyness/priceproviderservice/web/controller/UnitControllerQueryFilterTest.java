package de.ebusyness.priceproviderservice.web.controller;

import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import de.ebusyness.priceproviderservice.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive query filter tests for Unit entity.
 * Tests all query scenarios from the Postman collection including happy and angry paths.
 */
@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class UnitControllerQueryFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnitEntityRepository unitRepository;

    @BeforeEach
    public void setup() {
        unitRepository.deleteAll();

        // Create test data
        UnitEntity meter = new UnitEntity("m");
        Map<String, String> meterNames = new HashMap<>();
        meterNames.put("en", "Meter");
        meterNames.put("de", "Meter");
        meter.setName(meterNames);
        meter.setMeasure("length");
        unitRepository.save(meter);

        UnitEntity centimeter = new UnitEntity("cm");
        Map<String, String> cmNames = new HashMap<>();
        cmNames.put("en", "Centimeter");
        cmNames.put("de", "Zentimeter");
        centimeter.setName(cmNames);
        centimeter.setMeasure("length");
        centimeter.setBaseUnit(meter);
        centimeter.setFactor(new BigDecimal("0.01"));
        unitRepository.save(centimeter);

        UnitEntity millimeter = new UnitEntity("mm");
        Map<String, String> mmNames = new HashMap<>();
        mmNames.put("en", "Millimeter");
        mmNames.put("de", "Millimeter");
        millimeter.setName(mmNames);
        millimeter.setMeasure("length");
        millimeter.setBaseUnit(meter);
        millimeter.setFactor(new BigDecimal("0.001"));
        unitRepository.save(millimeter);

        UnitEntity kilogram = new UnitEntity("kg");
        Map<String, String> kgNames = new HashMap<>();
        kgNames.put("en", "Kilogram");
        kgNames.put("de", "Kilogramm");
        kilogram.setName(kgNames);
        kilogram.setMeasure("mass");
        unitRepository.save(kilogram);

        UnitEntity gram = new UnitEntity("g");
        Map<String, String> gNames = new HashMap<>();
        gNames.put("en", "Gram");
        gNames.put("de", "Gramm");
        gram.setName(gNames);
        gram.setMeasure("mass");
        gram.setBaseUnit(kilogram);
        gram.setFactor(new BigDecimal("0.001"));
        unitRepository.save(gram);

        UnitEntity liter = new UnitEntity("l");
        Map<String, String> lNames = new HashMap<>();
        lNames.put("en", "Liter");
        lNames.put("de", "Liter");
        liter.setName(lNames);
        liter.setMeasure("volume");
        unitRepository.save(liter);
    }

    // ====== HAPPY PATH TESTS ======

    @Test
    @Order(1)
    public void testFilterByMeasure_Length() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "measure:length"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("m")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("cm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("mm")));
    }

    @Test
    @Order(2)
    public void testFilterByMeasure_Mass() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "measure:mass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("kg")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("g")));
    }

    @Test
    @Order(3)
    public void testFilterByFactorRange() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "factor:[0.001 TO 1]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("cm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("mm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("g")));
    }

    @Test
    @Order(4)
    public void testFilterBaseUnitExists() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "baseUnitRef.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("cm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("mm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("g")));
    }

    @Test
    @Order(5)
    public void testFilterBaseUnitNotExists() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "baseUnitRef.exists:false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("m")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("kg")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("l")));
    }

    @Test
    @Order(6)
    public void testFilterBaseUnitWithMeasure() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "baseUnitRef.exists:true AND measure:length"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("cm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("mm")));
    }

    @Test
    @Order(7)
    public void testComplexFilter_MeasureOrWithFactor() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "(measure:length OR measure:mass) AND factor:>0.001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].symbol", is("cm")));
    }

    @Test
    @Order(8)
    public void testFilterByFactorGreaterThan() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "factor:>0.005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].symbol", is("cm")));
    }

    @Test
    @Order(9)
    public void testFilterByFactorLessThan() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "factor:<0.005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("mm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("g")));
    }

    @Test
    @Order(10)
    public void testFilterBySymbolExact() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "symbol:m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].symbol", is("m")));
    }

    @Test
    @Order(11)
    public void testFilterByBaseUnitSymbol() throws Exception {
        // Test filtering by referenced entity's ID field (the fix we implemented)
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "baseUnitRef.symbol:m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("cm")))
                .andExpect(jsonPath("$.items[*].symbol", hasItem("mm")));
    }

    // ====== ANGRY PATH TESTS (Error Scenarios) ======

    @Test
    @Order(100)
    public void testInvalidFieldName() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "invalidFieldName:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(101)
    public void testInvalidNestedField() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "name.invalidNested:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(102)
    public void testInvalidReferenceField() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "baseUnitRef.wrongField:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(103)
    public void testInvalidQuerySyntax() throws Exception {
        // Empty value after colon should return Bad Request
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "measure:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(104)
    public void testInvalidOperator() throws Exception {
        // Invalid operator used to be tolerated; now treated as client error
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "measure::invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(105)
    public void testMalformedRangeQuery() throws Exception {
        // BUG: Malformed range causes a 500 server error
        // This should ideally return 400 Bad Request
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "factor:[0.001 TO]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(106)
    public void testOpenEndedRange_Factor_UpperUnbounded() throws Exception {
        // factor:[0.001 TO *] should include all units with factor >= 0.001
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "factor:[0.001 TO *]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    @Order(107)
    public void testOpenEndedRange_Factor_LowerUnbounded() throws Exception {
        // factor:[* TO 0.01] should include units with factor <= 0.01
        mockMvc.perform(get("/admin/api/units")
                        .param("q", "factor:[* TO 0.01]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }
}
