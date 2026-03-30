package de.ebusyness.priceproviderservice.service.unit;

import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to demonstrate potential cyclic dependency issues
 * when working with database-persisted entities.
 * 
 * This test explores scenarios where the validator might miss cycles
 * due to lazy loading or entity detachment.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class UnitCyclicDependencyDatabaseTest {

    @Autowired
    private UnitService unitEntityService;
    
    @Autowired
    private UnitEntityRepository unitEntityRepository;

    /**
     * This test demonstrates a potential issue where a cycle could be created
     * if the validator doesn't properly handle the full hierarchy check.
     */
    @Test
    public void testComplexHierarchyCycle_ShouldBeDetected() throws EntityValidationException {
        // Create a hierarchy: mm → cm → m
        UnitEntity m = createUnit("m_test", "meter", "length");
        m = unitEntityService.save(m);
        
        UnitEntity cm = createUnit("cm_test", "centimeter", "length");
        cm.setBaseUnit(m);
        cm.setFactor(new BigDecimal("0.01"));
        cm = unitEntityService.save(cm);
        
        UnitEntity mm = createUnit("mm_test", "millimeter", "length");
        mm.setBaseUnit(cm);
        mm.setFactor(new BigDecimal("0.1"));
        mm = unitEntityService.save(mm);
        
        // Now try to make m point to mm (creating cycle: m → mm → cm → m)
        UnitEntity fetchedM = unitEntityRepository.findById("m_test").orElseThrow();
        fetchedM.setBaseUnit(mm);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(fetchedM),
            "Should detect cycle when m → mm → cm → m"
        );
        
        assertTrue(exception.getMessage().contains("cyclic"));
    }
    
    /**
     * Test the specific scenario mentioned in the issue:
     * m with baseUnit cm, then cm with baseUnit m
     */
    @Test
    public void testIssueScenario_MutualReferenceBetweenPersistedUnits() throws EntityValidationException {
        // Step 1: Create m with baseUnit cm
        UnitEntity cm = createUnit("cm_issue", "centimeter", "length");
        cm = unitEntityService.save(cm);
        
        UnitEntity m = createUnit("m_issue", "meter", "length");
        m.setBaseUnit(cm);
        m.setFactor(new BigDecimal("100"));
        m = unitEntityService.save(m);
        
        // Verify the relationship was saved
        UnitEntity fetchedM = unitEntityRepository.findById("m_issue").orElseThrow();
        assertNotNull(fetchedM.getBaseUnit());
        assertEquals("cm_issue", fetchedM.getBaseUnit().getSymbol());
        
        // Step 2: Try to update cm to have baseUnit m (creating cycle)
        UnitEntity fetchedCm = unitEntityRepository.findById("cm_issue").orElseThrow();
        
        // Refresh m to get the latest state
        UnitEntity latestM = unitEntityRepository.findById("m_issue").orElseThrow();
        fetchedCm.setBaseUnit(latestM);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(fetchedCm),
            "Should detect cycle when cm → m and m → cm (both persisted in DB)"
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
