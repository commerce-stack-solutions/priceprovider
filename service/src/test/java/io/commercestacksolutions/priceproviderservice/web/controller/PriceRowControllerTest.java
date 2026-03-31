package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.PriceRowController;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.PriceRowFacade;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.commercestacksolutions.commons.web.rest.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestSecurityConfig.class)
@WebMvcTest(PriceRowController.class)
public class PriceRowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PriceRowFacade priceRowFacade;

    @Test
    public void testGetPriceRows() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetPriceRowsWithPaging() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("page", "0")
                        .param("page-size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetPriceRowsWithSorting() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("page", "0")
                        .param("page-size", "10")
                        .param("sort-by", "id")
                        .param("sort-direction", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetPriceRowsWithMultipleSortFields() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("page", "0")
                        .param("page-size", "10")
                        .param("sort-by", "priceValue", "id")
                        .param("sort-direction", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetPriceRow() throws Exception {
        PriceRowRestEntity priceRowRestEntity = new PriceRowRestEntity();
        priceRowRestEntity.setPricedResourceId("test");

        when(priceRowFacade.getPriceRow(anyLong(), any())).thenReturn(priceRowRestEntity);

        mockMvc.perform(get("/admin/api/pricerows/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricedResourceId").value("test"));
    }

    @Test
    public void testGetPriceRowNotFound() throws Exception {
        PriceRowRestEntity priceRowRestEntity = new PriceRowRestEntity();
        priceRowRestEntity.addMessage(new Message(Message.MessageType.ERROR, "errors.priceRow.notFound", List.of("id")));

        when(priceRowFacade.getPriceRow(anyLong(), any())).thenReturn(priceRowRestEntity);

        mockMvc.perform(get("/admin/api/pricerows/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.$messages[0].message-key").value("errors.priceRow.notFound"));
    }

    @Test
    public void testUpdatePriceRow() throws Exception {
        PriceRowRestEntity priceRowRestEntity = new PriceRowRestEntity();
        priceRowRestEntity.setPricedResourceId("updated");

        when(priceRowFacade.createOrRecreate(anyLong(), any())).thenReturn(priceRowRestEntity);

        mockMvc.perform(put("/admin/api/pricerows/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PriceRowRestEntity())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricedResourceId").value("updated"));
    }

    @Test
    public void testCreatePriceRowViaPut() throws Exception {
        PriceRowRestEntity priceRowRestEntity = new PriceRowRestEntity();
        priceRowRestEntity.setId(999L);
        priceRowRestEntity.setPricedResourceId("created-via-put");

        when(priceRowFacade.createOrRecreate(anyLong(), any())).thenReturn(priceRowRestEntity);

        mockMvc.perform(put("/admin/api/pricerows/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PriceRowRestEntity())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(999))
                .andExpect(jsonPath("$.pricedResourceId").value("created-via-put"));
    }

    @Test
    public void testPatchPriceRowAdd() throws Exception {
        PriceRowRestEntity priceRowRestEntity = new PriceRowRestEntity();
        priceRowRestEntity.setPricedResourceId("added");

        when(priceRowFacade.patch(anyLong(), any())).thenReturn(priceRowRestEntity);

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"add\",\"path\":\"/pricedResourceId\",\"value\":\"added\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricedResourceId").value("added"));
    }

    @Test
    public void testPatchPriceRowRemove() throws Exception {
        when(priceRowFacade.patch(anyLong(), any())).thenReturn(new PriceRowRestEntity());

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"remove\",\"path\":\"/groupRefs\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    public void testPatchPriceRowReplace() throws Exception {
        PriceRowRestEntity priceRowRestEntity = new PriceRowRestEntity();
        priceRowRestEntity.setPricedResourceId("replaced");

        when(priceRowFacade.patch(anyLong(), any())).thenReturn(priceRowRestEntity);

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/pricedResourceId\",\"value\":\"replaced\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricedResourceId").value("replaced"));
    }

    @Test
    public void testDeletePriceRow() throws Exception {
        mockMvc.perform(delete("/admin/api/pricerows/1"))
                .andExpect(status().isNoContent());
    }


    @Test
    public void testBulkDeletePriceRows() throws Exception {
        mockMvc.perform(post("/admin/api/pricerows/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isNoContent());
    }

}