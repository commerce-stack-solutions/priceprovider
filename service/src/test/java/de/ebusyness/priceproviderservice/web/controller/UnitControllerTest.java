package de.ebusyness.priceproviderservice.web.controller.adminapi;

import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.priceproviderservice.web.controller.adminapi.UnitController;
import de.ebusyness.priceproviderservice.facade.unit.UnitFacadeService;
import de.ebusyness.priceproviderservice.facade.unit.restentity.UnitRestEntity;
import de.ebusyness.commons.web.rest.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import de.ebusyness.priceproviderservice.config.TestSecurityConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
@WebMvcTest(UnitController.class)
public class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnitFacadeService unitFacade;

    @Test
    public void testGetUnits() throws Exception {
        mockMvc.perform(get("/admin/api/units"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUnitsWithPaging() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("page", "0")
                        .param("page-size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUnitsWithSorting() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("page", "0")
                        .param("page-size", "10")
                        .param("sort-by", "symbol")
                        .param("sort-direction", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUnitsWithMultipleSortFields() throws Exception {
        mockMvc.perform(get("/admin/api/units")
                        .param("page", "0")
                        .param("page-size", "10")
                        .param("sort-by", "symbol", "factor")
                        .param("sort-direction", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetUnit() throws Exception {
        mockMvc.perform(get("/admin/api/units/kg"))
                .andExpect(status().isOk());
    }

    @Test
    public void testPatchReplace() throws Exception {
        UnitRestEntity unitRestEntity = new UnitRestEntity();
        unitRestEntity.setSymbol("m");
        unitRestEntity.setMeasure("length");

        when(unitFacade.patch(anyString(), any())).thenReturn(unitRestEntity);

        mockMvc.perform(patch("/admin/api/units/m")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/measure\",\"value\":\"length\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measure").value("length"));
    }

    @Test
    public void testPatchMapField() throws Exception {
        UnitRestEntity unitRestEntity = new UnitRestEntity();
        unitRestEntity.setSymbol("m");

        when(unitFacade.patch(anyString(), any())).thenReturn(unitRestEntity);

        mockMvc.perform(patch("/admin/api/units/m")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"add\",\"path\":\"/name/en\",\"value\":\"meter\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testPatchNotFound() throws Exception {

    }

    @Test
    public void testPatchValidationError() throws Exception {
    }

    @Test
    public void testDelete() throws Exception {
        mockMvc.perform(delete("/admin/api/units/kg"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteUnits() throws Exception {
        mockMvc.perform(post("/admin/api/units/bulk-delete")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("[\"kg\", \"m\", \"l\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteUnitsWithDataIntegrityViolation() throws Exception {
        org.mockito.Mockito.doThrow(new de.ebusyness.commons.exception.DataIntegrityException("Cannot delete unit - it is still referenced by other entities"))
                .when(unitFacade).bulkDeleteUnits(any());

        mockMvc.perform(post("/admin/api/units/bulk-delete")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("[\"m\"]"))
                .andExpect(status().isConflict());
    }

    @Test
    public void testCreateUnit() throws Exception {
        UnitRestEntity unitRestEntity = new UnitRestEntity();
        unitRestEntity.setSymbol("m");

        when(unitFacade.create(any())).thenReturn(unitRestEntity);

        mockMvc.perform(post("/admin/api/units/create")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"m\",\"name\":{\"de\":\"x\",\"en\":\"x\"},\"measure\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("m"));
    }

    @Test
    public void testCreateUnitAlreadyExists() throws Exception {
        when(unitFacade.create(any())).thenThrow(
                new EntityAlreadyExistsException("Unit with symbol m already exists", List.of("symbol"))
        );

        mockMvc.perform(post("/admin/api/units/create")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"m\",\"name\":{\"de\":\"x\",\"en\":\"x\"},\"measure\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Unit with symbol m already exists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("symbol"));
    }
}
