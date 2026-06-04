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
 * Comprehensive integration tests for the specific issue mentioned:
 * "there are still cyclic dependencies possible. e.g. m with baseUnit cm
 * at the same time cm with baseUnit m"
 * 
 * These tests verify that the enhanced validator prevents ALL possible
 * cyclic dependency scenarios in the database.
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class UnitCyclicDependencyComprehensiveTest {

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

    /**
     * Test the EXACT scenario from the issue:
     * Step 1: Create m with baseUnit cm
     * Step 2: Try to create/update cm with baseUnit m
     * This should be prevented!
     */
    @Test
    public void testIssueScenario_MutualDependency_ShouldBePrevented() throws EntityValidationException {
        // Create cm (no baseUnit initially)
        UnitEntity cm = createUnit("cm_comprehensive", "centimeter", "length");
        cm = unitEntityService.save(cm);
        assertNotNull(cm);
        
        // Create m with baseUnit cm
        UnitEntity m = createUnit("m_comprehensive", "meter", "length");
        m.setBaseUnit(cm);
        m.setFactor(new BigDecimal("100"));
        m = unitEntityService.save(m);
        assertNotNull(m);
        
        // Verify m → cm relationship exists
        UnitEntity fetchedM = unitEntityService.getUnit("m_comprehensive");
        assertNotNull(fetchedM.getBaseUnit());
        assertEquals("cm_comprehensive", fetchedM.getBaseUnit().getSymbol());
        
        // Now try to update cm to have baseUnit m (this should FAIL)
        UnitEntity fetchedCm = unitEntityService.getUnit("cm_comprehensive");
        assertNotNull(fetchedCm);
        
        // Get fresh reference to m
        UnitEntity latestM = unitEntityService.getUnit("m_comprehensive");
        fetchedCm.setBaseUnit(latestM);
        
        // This should throw an exception due to cycle detection
        EntityValidationException exception = assertThrows(
                EntityValidationException.class,
            () -> unitEntityService.save(fetchedCm),
            "Should prevent creating mutual dependency: m→cm and cm→m"
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("cyclic") || 
                   exception.getMessage().toLowerCase().contains("cycle"),
                   "Exception message should mention cyclic dependency");
    }

    /**
     * Test using updateUnit() method specifically to ensure it also validates
     */
    @Test
    public void testUpdateUnit_WithCycle_ShouldBePrevented() throws EntityValidationException {
        // Create units
        UnitEntity unitA = createUnit("updateA_comp", "Unit A", "test");
        unitA = unitEntityService.save(unitA);
        
        UnitEntity unitB = createUnit("updateB_comp", "Unit B", "test");
        unitB.setBaseUnit(unitA);
        unitB = unitEntityService.save(unitB);
        
        // Try to use updateUnit() to create a cycle
        UnitEntity fetchedA = unitEntityService.getUnit("updateA_comp");
        UnitEntity fetchedB = unitEntityService.getUnit("updateB_comp");
        fetchedA.setBaseUnit(fetchedB);
        
        // save should now validate (previously it didn't)
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(fetchedA),
            "save() should validate and prevent cycles"
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("cyclic"));
    }

    /**
     * Test a more complex scenario: A → B → C, then try C → A
     */
    @Test
    public void testComplexChainCycle_ShouldBePrevented() throws EntityValidationException {
        // Create base unit C
        UnitEntity unitC = createUnit("C_chain", "Unit C", "test");
        unitC = unitEntityService.save(unitC);
        
        // Create B → C
        UnitEntity unitB = createUnit("B_chain", "Unit B", "test");
        unitB.setBaseUnit(unitC);
        unitB = unitEntityService.save(unitB);
        
        // Create A → B
        UnitEntity unitA = createUnit("A_chain", "Unit A", "test");
        unitA.setBaseUnit(unitB);
        unitA = unitEntityService.save(unitA);
        
        // Now try to make C → A (creating cycle A → B → C → A)
        UnitEntity fetchedC = unitEntityService.getUnit("C_chain");
        UnitEntity fetchedA = unitEntityService.getUnit("A_chain");
        fetchedC.setBaseUnit(fetchedA);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(fetchedC),
            "Should prevent cycle A → B → C → A"
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("cyclic"));
    }

    /**
     * Test that valid hierarchy changes are still allowed
     */
    @Test
    public void testValidHierarchyChange_ShouldBeAllowed() throws EntityValidationException {
        // Create units: mm → cm → m
        UnitEntity m = createUnit("m_valid", "meter", "length");
        m = unitEntityService.save(m);
        
        UnitEntity cm = createUnit("cm_valid", "centimeter", "length");
        cm.setBaseUnit(m);
        cm.setFactor(new BigDecimal("0.01"));
        cm = unitEntityService.save(cm);
        
        UnitEntity mm = createUnit("mm_valid", "millimeter", "length");
        mm.setBaseUnit(cm);
        mm.setFactor(new BigDecimal("0.1"));
        mm = unitEntityService.save(mm);
        
        // Now change mm to point directly to m (valid: mm → m)
        UnitEntity fetchedMm = unitEntityService.getUnit("mm_valid");
        UnitEntity fetchedM = unitEntityService.getUnit("m_valid");
        fetchedMm.setBaseUnit(fetchedM);
        fetchedMm.setFactor(new BigDecimal("0.001"));
        
        // This should succeed (no cycle)
        UnitEntity updated = unitEntityService.save(fetchedMm);
        assertNotNull(updated);
        assertEquals("m_valid", updated.getBaseUnit().getSymbol());
    }

    /**
     * Test that removing a baseUnit is allowed
     */
    @Test
    public void testRemoveBaseUnit_ShouldBeAllowed() throws EntityValidationException {
        // Create m → cm
        UnitEntity cm = createUnit("cm_remove", "centimeter", "length");
        cm = unitEntityService.save(cm);
        
        UnitEntity m = createUnit("m_remove", "meter", "length");
        m.setBaseUnit(cm);
        m = unitEntityService.save(m);
        
        // Remove the baseUnit from m
        UnitEntity fetchedM = unitEntityService.getUnit("m_remove");
        fetchedM.setBaseUnit(null);
        
        // This should succeed
        UnitEntity updated = unitEntityService.save(fetchedM);
        assertNotNull(updated);
        assertNull(updated.getBaseUnit());
    }

    /**
     * Test cycle detection with multiple independent chains
     */
    @Test
    public void testMultipleIndependentChains_OnlyTargetedCycleDetected() throws EntityValidationException {
        // Create chain 1: A1 → B1
        UnitEntity b1 = createUnit("B1", "Unit B1", "type1");
        b1 = unitEntityService.save(b1);
        
        UnitEntity a1 = createUnit("A1", "Unit A1", "type1");
        a1.setBaseUnit(b1);
        a1 = unitEntityService.save(a1);
        
        // Create chain 2: A2 → B2
        UnitEntity b2 = createUnit("B2", "Unit B2", "type2");
        b2 = unitEntityService.save(b2);
        
        UnitEntity a2 = createUnit("A2", "Unit A2", "type2");
        a2.setBaseUnit(b2);
        a2 = unitEntityService.save(a2);
        
        // Try to create cycle in chain 1 only: B1 → A1 (should fail)
        UnitEntity fetchedB1 = unitEntityService.getUnit("B1");
        UnitEntity fetchedA1 = unitEntityService.getUnit("A1");
        fetchedB1.setBaseUnit(fetchedA1);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(fetchedB1),
            "Should detect cycle in chain 1"
        );
        
        assertTrue(exception.getMessage().toLowerCase().contains("cyclic"));
        
        // Verify chain 2 is still valid
        UnitEntity fetchedA2 = unitEntityService.getUnit("A2");
        assertNotNull(fetchedA2);
        assertNotNull(fetchedA2.getBaseUnit());
        assertEquals("B2", fetchedA2.getBaseUnit().getSymbol());
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
