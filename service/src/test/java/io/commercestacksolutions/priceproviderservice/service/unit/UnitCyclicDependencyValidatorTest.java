package io.commercestacksolutions.priceproviderservice.service.unit;

import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.service.unit.validation.UnitCyclicDependencyValidator;
import io.commercestacksolutions.commons.web.rest.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for UnitCyclicDependencyValidator to ensure cyclic dependencies are detected correctly.
 * Tests the ValidationRule interface implementation.
 */
public class UnitCyclicDependencyValidatorTest {

    private UnitCyclicDependencyValidator validator;
    private UnitEntityRepository mockRepository;

    @BeforeEach
    public void setup() {
        mockRepository = Mockito.mock(UnitEntityRepository.class);
        validator = new UnitCyclicDependencyValidator(mockRepository);
    }

    @Test
    public void testNoBaseUnit_ShouldBeValid() {
        UnitEntity unit = new UnitEntity("m");
        unit.setBaseUnit(null);
        
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        
        List<Message> errors = validator.validate(unit);
        
        assertTrue(errors.isEmpty(), "Unit with no base unit should be valid");
    }

    @Test
    public void testValidBaseUnit_ShouldBeValid() {
        UnitEntity baseUnit = new UnitEntity("m");
        baseUnit.setBaseUnit(null);
        
        UnitEntity unit = new UnitEntity("cm");
        unit.setBaseUnit(baseUnit);
        
        // Mock repository to return only the base unit (no cycles)
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(baseUnit);
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unit);
        
        assertTrue(errors.isEmpty(), "Unit with valid base unit should be valid");
    }

    @Test
    public void testDirectCycle_ShouldBeInvalid() {
        // Create unit A that tries to reference itself
        UnitEntity unitA = new UnitEntity("A");
        unitA.setBaseUnit(unitA);
        
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        
        List<Message> errors = validator.validate(unitA);
        
        assertFalse(errors.isEmpty(), "Unit cannot have itself as base unit");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    public void testTwoNodeCycle_ShouldBeInvalid() {
        // A → B → A (cycle)
        UnitEntity unitA = new UnitEntity("A");
        UnitEntity unitB = new UnitEntity("B");
        
        // Set up the existing relationship: B → A
        unitB.setBaseUnit(unitA);
        
        // Now try to set A → B (which would create a cycle)
        unitA.setBaseUnit(unitB);
        
        // Mock repository to return the existing relationship B → A
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(unitB); // B → A exists in database
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unitA);
        
        assertFalse(errors.isEmpty(), "Should detect cycle when A → B and B → A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    public void testThreeNodeCycle_ShouldBeInvalid() {
        // A → B → C → A (cycle)
        UnitEntity unitA = new UnitEntity("A");
        UnitEntity unitB = new UnitEntity("B");
        UnitEntity unitC = new UnitEntity("C");
        
        // Set up existing relationships: B → C → A
        unitC.setBaseUnit(unitA);
        unitB.setBaseUnit(unitC);
        
        // Now try to set A → B (which would create a cycle)
        unitA.setBaseUnit(unitB);
        
        // Mock repository to return existing relationships
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(unitB); // B → C
        allUnits.add(unitC); // C → A
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unitA);
        
        assertFalse(errors.isEmpty(), "Should detect cycle when A → B → C → A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    public void testLongChainNoCycle_ShouldBeValid() {
        // A → B → C → D (no cycle)
        UnitEntity unitD = new UnitEntity("D");
        unitD.setBaseUnit(null);
        
        UnitEntity unitC = new UnitEntity("C");
        unitC.setBaseUnit(unitD);
        
        UnitEntity unitB = new UnitEntity("B");
        unitB.setBaseUnit(unitC);
        
        UnitEntity unitA = new UnitEntity("A");
        unitA.setBaseUnit(unitB);
        
        // Mock repository to return existing relationships
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(unitB); // B → C
        allUnits.add(unitC); // C → D
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unitA);
        
        assertTrue(errors.isEmpty(), "Long chain without cycle should be valid");
    }

    @Test
    public void testFourNodeCycle_ShouldBeInvalid() {
        // A → B → C → D → A (cycle)
        UnitEntity unitA = new UnitEntity("A");
        UnitEntity unitB = new UnitEntity("B");
        UnitEntity unitC = new UnitEntity("C");
        UnitEntity unitD = new UnitEntity("D");
        
        // Set up existing relationships: B → C → D → A
        unitD.setBaseUnit(unitA);
        unitC.setBaseUnit(unitD);
        unitB.setBaseUnit(unitC);
        
        // Now try to set A → B (which would create a cycle)
        unitA.setBaseUnit(unitB);
        
        // Mock repository to return existing relationships
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(unitB); // B → C
        allUnits.add(unitC); // C → D
        allUnits.add(unitD); // D → A
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unitA);
        
        assertFalse(errors.isEmpty(), "Should detect cycle when A → B → C → D → A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    public void testValidationErrorMessage() {
        UnitEntity unitA = new UnitEntity("cm");
        UnitEntity unitB = new UnitEntity("m");
        
        unitB.setBaseUnit(unitA);
        unitA.setBaseUnit(unitB);
        
        // Mock repository to return existing relationship
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(unitB); // m → cm
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unitA);
        
        assertFalse(errors.isEmpty());
        Message error = errors.get(0);
        assertNotNull(error.getMessageKey());
        assertTrue(error.getMessageKey().contains("cyclic"));
        // Check parameters contain the unit symbols
        assertNotNull(error.getParameters());
        assertTrue(error.getParameters().containsValue("cm") || error.getParameters().containsValue("m"));
        assertEquals(Message.MessageType.ERROR, error.getType());
        assertEquals(400, error.getStatusCode());
    }

    @Test
    public void testNullUnit_ShouldBeValid() {
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        
        List<Message> errors = validator.validate(null);
        
        assertTrue(errors.isEmpty(), "Null unit should be considered valid (no validation possible)");
    }

    @Test
    public void testUnitWithNullSymbol_ShouldBeValid() {
        UnitEntity unit = new UnitEntity();
        unit.setSymbol(null);
        UnitEntity baseUnit = new UnitEntity("m");
        unit.setBaseUnit(baseUnit);
        
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        
        List<Message> errors = validator.validate(unit);
        
        assertTrue(errors.isEmpty(), "Unit with null symbol should be considered valid (no validation possible)");
    }

    @Test
    public void testChangingBaseUnitInChain_ShouldNotCreateCycle() {
        // Existing chain: A → B → C
        UnitEntity unitC = new UnitEntity("C");
        unitC.setBaseUnit(null);
        
        UnitEntity unitB = new UnitEntity("B");
        unitB.setBaseUnit(unitC);
        
        UnitEntity unitA = new UnitEntity("A");
        unitA.setBaseUnit(unitB);
        
        // Now change C to point to a new unit D (C → D)
        UnitEntity unitD = new UnitEntity("D");
        unitD.setBaseUnit(null);
        unitC.setBaseUnit(unitD);
        
        // Mock repository to return existing relationships
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(unitA); // A → B
        allUnits.add(unitB); // B → C
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        List<Message> errors = validator.validate(unitC);
        
        assertTrue(errors.isEmpty(), "Changing base unit in chain to new unit should be valid");
    }
}
