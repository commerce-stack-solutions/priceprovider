package io.commercestacksolutions.priceproviderservice.web.controller.publicapi;

import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.config.security.JwtClaimsExtractor;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.PublicPriceFacade;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for PublicPriceController.
 * All endpoints are scoped by channel and country.
 */
@Import(TestSecurityConfig.class)
@WebMvcTest(PublicPriceController.class)
public class PublicPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicPriceFacade publicPriceFacade;

    @MockBean
    private JwtClaimsExtractor jwtClaimsExtractor;

    // ---- getBestPrice (channel + country) ----

    @Test
    public void testGetBestPrice_Success() throws Exception {
        PublicPriceRestEntity mockResponse = new PublicPriceRestEntity();
        mockResponse.setId(1L);
        mockResponse.setPricedResourceId("DEMO-PRODUCT-001");
        mockResponse.setPriceValue(new BigDecimal("100.00"));

        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(null);
        when(publicPriceFacade.getBestPrice(
                eq("dach-sales-channel"), eq("DE"), isNull(), eq("DEMO-PRODUCT-001"),
                any(BigDecimal.class), eq("piece"), eq("EUR"),
                eq(PriceType.SALES_PRICE), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE")
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pricedResourceId").value("DEMO-PRODUCT-001"))
                .andExpect(jsonPath("$.priceValue").value(100.00));
    }

    @Test
    public void testGetBestPrice_WithGroup_Success() throws Exception {
        PublicPriceRestEntity mockResponse = new PublicPriceRestEntity();
        mockResponse.setId(1L);
        mockResponse.setPricedResourceId("DEMO-PRODUCT-001");
        mockResponse.setPriceValue(new BigDecimal("90.00"));

        String groupPath = "DEMO-GROUP-STANDARD/DEMO-GROUP-PREMIUM";
        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(groupPath);
        when(publicPriceFacade.getBestPrice(
                eq("dach-sales-channel"), eq("DE"), eq(groupPath), eq("DEMO-PRODUCT-001"),
                any(BigDecimal.class), eq("piece"), eq("EUR"),
                eq(PriceType.SALES_PRICE), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE")
                        .with(jwt())
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.priceValue").value(90.00));
    }

    @Test
    public void testGetBestPrice_NotFound() throws Exception {
        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(null);
        when(publicPriceFacade.getBestPrice(
                anyString(), anyString(), any(), anyString(),
                any(BigDecimal.class), anyString(), anyString(),
                any(PriceType.class), any()
        )).thenThrow(new NotFoundException("No matching price found"));

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/PROD-999/SALES_PRICE")
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetBestPrice_InvalidPriceType() throws Exception {
        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/INVALID_TYPE")
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetBestPrice_MissingQuantity() throws Exception {
        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SALES_PRICE", "PURCHASE_PRICE", "MATERIAL_COST"})
    public void testGetBestPrice_AllPriceTypes(String priceType) throws Exception {
        PublicPriceRestEntity mockResponse = new PublicPriceRestEntity();
        mockResponse.setId(1L);
        mockResponse.setPriceValue(new BigDecimal("100.00"));

        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(null);
        when(publicPriceFacade.getBestPrice(
                anyString(), anyString(), any(), anyString(),
                any(BigDecimal.class), anyString(), anyString(),
                any(PriceType.class), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/" + priceType)
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk());
    }

    // ---- getAllPrices (channel + country) ----

    @Test
    public void testGetAllPrices_Success() throws Exception {
        PublicPriceListRestEntity mockResponse = new PublicPriceListRestEntity();
        mockResponse.setItems(new ArrayList<>());

        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(null);
        when(publicPriceFacade.getAllPrices(
                eq("dach-sales-channel"), eq("DE"), isNull(), eq("DEMO-PRODUCT-001"),
                any(BigDecimal.class), eq("piece"), eq("EUR"),
                eq(PriceType.SALES_PRICE), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE/all-candidates")
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    public void testGetAllPrices_WithGroup_Success() throws Exception {
        PublicPriceListRestEntity mockResponse = new PublicPriceListRestEntity();
        mockResponse.setItems(new ArrayList<>());

        String groupPath = "DEMO-GROUP-STANDARD/DEMO-GROUP-PREMIUM";
        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(groupPath);
        when(publicPriceFacade.getAllPrices(
                eq("dach-sales-channel"), eq("DE"), eq(groupPath), eq("DEMO-PRODUCT-001"),
                any(BigDecimal.class), eq("piece"), eq("EUR"),
                eq(PriceType.SALES_PRICE), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE/all-candidates")
                        .with(jwt())
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    public void testGetAllPrices_EmptyList() throws Exception {
        PublicPriceListRestEntity mockResponse = new PublicPriceListRestEntity();
        mockResponse.setItems(new ArrayList<>());

        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(null);
        when(publicPriceFacade.getAllPrices(
                anyString(), anyString(), any(), anyString(),
                any(BigDecimal.class), anyString(), anyString(),
                any(PriceType.class), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/PROD-999/SALES_PRICE/all-candidates")
                        .param("quantity", "10.00")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // ---- getAllQuantityBestPrices (channel + country) ----

    @Test
    public void testGetAllQuantityBestPrices_Success() throws Exception {
        PublicPriceListRestEntity mockResponse = new PublicPriceListRestEntity();
        mockResponse.setItems(new ArrayList<>());

        when(jwtClaimsExtractor.extractEffectiveOrganization(any())).thenReturn(null);
        when(publicPriceFacade.getAllQuantityBestPrices(
                eq("dach-sales-channel"), eq("DE"), isNull(), eq("DEMO-PRODUCT-001"),
                eq("piece"), eq("EUR"), eq(PriceType.SALES_PRICE), any()
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/public/api/channels/dach-sales-channel/countries/DE/pricedresource/DEMO-PRODUCT-001/SALES_PRICE/all-quantities")
                        .param("unit", "piece")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray());
    }
}
