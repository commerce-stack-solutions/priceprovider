package de.ebusyness.priceproviderservice.service.unit;

import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
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
 * Integration test to verify that cyclic dependency validation works during data import.
 * The validation in UnitService.save() should prevent importing data with cycles.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "service-config.initialize.essential-data-on=false",
    "service-config.initialize.sample-data-on=false"
})
@Transactional
public class UnitDataLoaderCyclicDependencyTest {

    @Autowired
    private UnitService unitEntityService;

    @Test
    public void testImportUnitsWithCycle_ShouldFailValidation() throws EntityValidationException {
        // This test simulates what would happen if a data file had cyclic dependencies
        // The data loader uses entityService.save() which has our validation
        
        // Create unit A
        UnitEntity unitA = createUnit("import_test_A", "Unit A", "test");
        UnitEntity savedUnitA = unitEntityService.save(unitA);
        
        // Create unit B pointing to A
        UnitEntity unitB = createUnit("import_test_B", "Unit B", "test");
        unitB.setBaseUnit(savedUnitA);
        UnitEntity savedUnitB = unitEntityService.save(unitB);
        
        // Try to import/save unit A now pointing to B (would create cycle)
        savedUnitA.setBaseUnit(savedUnitB);
        
        // This should throw an exception, just like during import
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(savedUnitA),
            "Data loader should fail when trying to save units with cyclic dependencies"
        );
        
        assertTrue(exception.getMessage().contains("cyclic"));
    }

    @Test
    public void testImportValidUnitChain_ShouldSucceed() throws EntityValidationException {
        // This test verifies that valid data can still be imported
        
        // Create a valid chain: base -> derived1 -> derived2
        UnitEntity base = createUnit("import_base", "Base Unit", "test");
        base = unitEntityService.save(base);
        
        UnitEntity derived1 = createUnit("import_derived1", "Derived 1", "test");
        derived1.setBaseUnit(base);
        derived1.setFactor(new BigDecimal("10"));
        derived1 = unitEntityService.save(derived1);
        
        UnitEntity derived2 = createUnit("import_derived2", "Derived 2", "test");
        derived2.setBaseUnit(derived1);
        derived2.setFactor(new BigDecimal("100"));
        UnitEntity saved = unitEntityService.save(derived2);
        
        assertNotNull(saved);
        assertEquals("import_derived2", saved.getSymbol());
        assertEquals("import_derived1", saved.getBaseUnit().getSymbol());
    }

    @Test
    public void testImportWithSelfReference_ShouldFailValidation() throws EntityValidationException {
        // Simulate importing a unit that references itself
        UnitEntity unit = createUnit("import_self_ref", "Self Reference", "test");
        UnitEntity savedUnit = unitEntityService.save(unit);
        
        // Try to update it to reference itself
        savedUnit.setBaseUnit(savedUnit);
        
        EntityValidationException exception = assertThrows(
            EntityValidationException.class,
            () -> unitEntityService.save(savedUnit),
            "Data loader should fail when unit references itself"
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
