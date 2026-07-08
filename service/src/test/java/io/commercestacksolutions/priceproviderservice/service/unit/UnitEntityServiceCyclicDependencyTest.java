package io.commercestacksolutions.priceproviderservice.service.unit;

import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for UnitService cyclic dependency validation.
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class UnitEntityServiceCyclicDependencyTest {

    @Autowired
    private UnitService unitEntityService;

    @BeforeEach
    public void setUp() {
        // Set up authentication context
        var authorities = AuthorityUtils.createAuthorityList(
            "priceprovider.admin:Unit:write",
            "priceprovider.admin:Unit:read"
        );
        var auth = new UsernamePasswordAuthenticationToken("test-admin", "test", authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testSaveUnitWithNoCycle_ShouldSucceed() throws EntityValidationException {
        // Create base unit
        UnitEntity baseUnit = createUnit("m", "meter", "length");
        unitEntityService.save(baseUnit);
        
        // Create derived unit
        UnitEntity derivedUnit = createUnit("cm", "centimeter", "length");
        derivedUnit.setBaseUnit(baseUnit);
        derivedUnit.setFactor(new BigDecimal("0.01"));
        
        UnitEntity saved = unitEntityService.save(derivedUnit);
        
        assertNotNull(saved);
        assertEquals("cm", saved.getSymbol());
        assertEquals("m", saved.getBaseUnit().getSymbol());
    }

    @Test
    public void testSaveUnitWithDirectCycle_ShouldThrowException() throws EntityValidationException {
        // Create a unit
        UnitEntity unitA = createUnit("A", "Unit A", "test");
        unitA = unitEntityService.save(unitA);
        
        // Try to make it reference itself
        final UnitEntity finalUnitA = unitA;
        finalUnitA.setBaseUnit(finalUnitA);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(finalUnitA),
            "Should throw exception when unit references itself"
        );
        
        assertTrue(exception.getMessage().contains("cyclic"));
    }

    @Test
    public void testSaveUnitWithTwoNodeCycle_ShouldThrowException() throws EntityValidationException {
        // Create unit A and B
        UnitEntity unitA = createUnit("testA", "Unit A", "test");
        UnitEntity unitB = createUnit("testB", "Unit B", "test");
        
        unitA = unitEntityService.save(unitA);
        unitB = unitEntityService.save(unitB);
        
        // Set B → A
        unitB.setBaseUnit(unitA);
        unitB = unitEntityService.save(unitB);
        
        // Try to set A → B (creating cycle)
        UnitEntity finalUnitA = unitA;
        UnitEntity finalUnitB = unitB;
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> {
                finalUnitA.setBaseUnit(finalUnitB);
                unitEntityService.save(finalUnitA);
            },
            "Should throw exception when A → B and B → A"
        );
        
        assertTrue(exception.getMessage().contains("cyclic"));
    }

    @Test
    public void testSaveUnitWithThreeNodeCycle_ShouldThrowException() throws EntityValidationException {
        // Create units A, B, and C
        UnitEntity unitA = createUnit("cycleA", "Unit A", "test");
        UnitEntity unitB = createUnit("cycleB", "Unit B", "test");
        UnitEntity unitC = createUnit("cycleC", "Unit C", "test");
        
        unitA = unitEntityService.save(unitA);
        unitB = unitEntityService.save(unitB);
        unitC = unitEntityService.save(unitC);
        
        // Set B → C → A
        unitC.setBaseUnit(unitA);
        unitC = unitEntityService.save(unitC);
        
        unitB.setBaseUnit(unitC);
        unitB = unitEntityService.save(unitB);
        
        // Try to set A → B (creating cycle A → B → C → A)
        UnitEntity finalUnitA = unitA;
        UnitEntity finalUnitB = unitB;
        EntityValidationException exception = assertThrows(
                EntityValidationException.class,
            () -> {
                finalUnitA.setBaseUnit(finalUnitB);
                unitEntityService.save(finalUnitA);
            },
            "Should throw exception when A → B → C → A"
        );
        
        assertTrue(exception.getMessage().contains("cyclic"));
    }

    @Test
    public void testSaveLongChainNoCycle_ShouldSucceed() throws EntityValidationException {
        // Create a chain: km → m → dm → cm
        UnitEntity cm = createUnit("chain_cm", "centimeter", "length");
        cm = unitEntityService.save(cm);
        
        UnitEntity dm = createUnit("chain_dm", "decimeter", "length");
        dm.setBaseUnit(cm);
        dm.setFactor(new BigDecimal("10"));
        dm = unitEntityService.save(dm);
        
        UnitEntity m = createUnit("chain_m", "meter", "length");
        m.setBaseUnit(dm);
        m.setFactor(new BigDecimal("10"));
        m = unitEntityService.save(m);
        
        UnitEntity km = createUnit("chain_km", "kilometer", "length");
        km.setBaseUnit(m);
        km.setFactor(new BigDecimal("1000"));
        
        UnitEntity saved = unitEntityService.save(km);
        
        assertNotNull(saved);
        assertEquals("chain_km", saved.getSymbol());
        assertEquals("chain_m", saved.getBaseUnit().getSymbol());
    }

    @Test
    public void testUpdateUnitToCreateCycle_ShouldThrowException() throws EntityValidationException {
        // Create units A and B with no cycle
        UnitEntity unitA = createUnit("updateA", "Unit A", "test");
        UnitEntity unitB = createUnit("updateB", "Unit B", "test");
        
        unitA = unitEntityService.save(unitA);
        
        unitB.setBaseUnit(unitA);
        unitB = unitEntityService.save(unitB);
        
        // Verify no cycle exists
        assertNotNull(unitB.getBaseUnit());
        
        // Now try to update A to point to B (creating cycle)
        UnitEntity fetchedA = unitEntityService.getUnit("updateA");
        fetchedA.setBaseUnit(unitB);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(fetchedA),
            "Should throw exception when updating creates a cycle"
        );
        
        assertTrue(exception.getMessage().contains("cyclic"));
    }

    private UnitEntity createUnit(String symbol, String name, String measure) {
        UnitEntity unit = new UnitEntity();
        unit.setSymbol(symbol);
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("en", name);
        nameMap.put("de", name); // Add German for mandatory language validation
        unit.setName(nameMap);
        unit.setMeasure(measure);
        unit.setFactor(BigDecimal.ONE);
        return unit;
    }
}
