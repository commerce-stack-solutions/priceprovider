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
 * Test to reproduce the issue where cyclic dependencies can exist:
 * m with baseUnit cm, and cm with baseUnit m
 * 
 * This tests the scenario where both relationships can exist "at the same time"
 * creating a mutual cycle that should be detected.
 */
public class UnitCyclicDependencyIssueTest {

    private UnitCyclicDependencyValidator validator;
    private UnitEntityRepository mockRepository;

    @BeforeEach
    public void setup() {
        mockRepository = Mockito.mock(UnitEntityRepository.class);
        validator = new UnitCyclicDependencyValidator(mockRepository);
    }

    @Test
    public void testMutualCycle_BothUnitsPointToEachOther_ShouldBeDetected() {
        // Create the scenario from the issue:
        // m with baseUnit cm
        UnitEntity cm = new UnitEntity("cm");
        UnitEntity m = new UnitEntity("m");
        m.setBaseUnit(cm);
        
        // Now try to set cm with baseUnit m (creating the cycle)
        cm.setBaseUnit(m);
        
        // Mock repository to return the existing relationship m → cm
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(m); // m → cm exists in database
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        // When validating cm, it should detect that:
        // cm → m → cm (cycle!)
        List<Message> errors = validator.validate(cm);
        
        assertFalse(errors.isEmpty(), "Should detect cycle when cm → m and m → cm");
        assertTrue(errors.get(0).getMessageKey().contains("cyclic") || 
                   errors.get(0).getMessageKey().contains("cycle"));
    }

    @Test
    public void testMutualCycle_ValidatingFirstUnit_ShouldAlsoBeDetected() {
        // Create the scenario in reverse order:
        // cm with baseUnit m
        UnitEntity m = new UnitEntity("m");
        UnitEntity cm = new UnitEntity("cm");
        cm.setBaseUnit(m);
        
        // Now try to set m with baseUnit cm (creating the cycle)
        m.setBaseUnit(cm);
        
        // Mock repository to return the existing relationship cm → m
        List<UnitEntity> allUnits = new ArrayList<>();
        allUnits.add(cm); // cm → m exists in database
        when(mockRepository.findAll()).thenReturn(allUnits);
        
        // When validating m, it should detect that:
        // m → cm → m (cycle!)
        List<Message> errors = validator.validate(m);
        
        assertFalse(errors.isEmpty(), "Should detect cycle when m → cm and cm → m");
        assertTrue(errors.get(0).getMessageKey().contains("cyclic") || 
                   errors.get(0).getMessageKey().contains("cycle"));
    }
}
