package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.TaxClassController;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.TaxClassFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

@Import(TestSecurityConfig.class)
@WebMvcTest(TaxClassController.class)
public class TaxClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxClassFacade taxClassFacade;

    @Test
    public void testBulkDeleteTaxClasses_Success() throws Exception {
        // Given: Facade will successfully delete tax classes
        doNothing().when(taxClassFacade).bulkDeleteTaxClasses(anyList());

        // When: Bulk delete is called
        mockMvc.perform(post("/admin/api/taxclasses/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"de-vat-full\", \"de-vat-reduced\", \"us-sales-tax\"]"))
                // Then: Expect 204 No Content
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteTaxClasses_DataIntegrityViolation() throws Exception {
        // Given: Facade will throw DataIntegrityException due to foreign key constraint
        doThrow(new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED))
                .when(taxClassFacade).bulkDeleteTaxClasses(anyList());

        // When: Bulk delete is called with tax classes that are referenced
        mockMvc.perform(post("/admin/api/taxclasses/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"de-vat-full\"]"))
                // Then: Expect 409 Conflict
                .andExpect(status().isConflict());
    }

    @Test
    public void testBulkDeleteTaxClasses_EmptyList() throws Exception {
        // Given: Facade will successfully handle empty list
        doNothing().when(taxClassFacade).bulkDeleteTaxClasses(anyList());

        // When: Bulk delete is called with empty list
        mockMvc.perform(post("/admin/api/taxclasses/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                // Then: Expect 204 No Content
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteTaxClasses_SingleItem() throws Exception {
        // Given: Facade will successfully delete single tax class
        doNothing().when(taxClassFacade).bulkDeleteTaxClasses(anyList());

        // When: Bulk delete is called with single item
        mockMvc.perform(post("/admin/api/taxclasses/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"de-vat-full\"]"))
                // Then: Expect 204 No Content
                .andExpect(status().isNoContent());
    }
}
