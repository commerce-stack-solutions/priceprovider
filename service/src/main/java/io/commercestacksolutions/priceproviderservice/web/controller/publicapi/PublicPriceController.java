package io.commercestacksolutions.priceproviderservice.web.controller.publicapi;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.domain.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.domain.pricetype.PriceTypeRegistry;
import io.commercestacksolutions.priceproviderservice.config.security.JwtClaimsExtractor;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.PublicPriceFacade;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

/**
 * Public API controller for price queries.
 *
 * All endpoints are scoped by channel and country:
 * - GET /public/api/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}
 * - GET /public/api/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}/candidates
 *
 * The net/gross price representation is determined by the channel's configured
 * price representation mode — there is no per-request taxation parameter.
 *
 * Organization/Group context is derived from the authenticated user's JWT when available.
 */
@RestController
@RequestMapping("/public/api")
@Tag(name = "Public Price API", description = "Public API for third-party price queries")
public class PublicPriceController {

    private final PublicPriceFacade publicPriceFacade;
    private final JwtClaimsExtractor jwtClaimsExtractor;
    private final PriceTypeRegistry priceTypeRegistry;

    @Autowired
    public PublicPriceController(PublicPriceFacade publicPriceFacade, JwtClaimsExtractor jwtClaimsExtractor, PriceTypeRegistry priceTypeRegistry) {
        this.publicPriceFacade = publicPriceFacade;
        this.jwtClaimsExtractor = jwtClaimsExtractor;
        this.priceTypeRegistry = priceTypeRegistry;
    }

    @Operation(summary = "Get best matching price for channel and country",
            description = "Finds the best matching price filtered by channel and country. The net/gross representation is determined by the channel's price representation mode.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully found matching price",
                            content = @Content(schema = @Schema(implementation = PublicPriceRestEntity.class))),
                    @ApiResponse(responseCode = "404", description = "No matching price found", content = @Content)
            })
    @GetMapping("/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}")
    public ResponseEntity<PublicPriceRestEntity> getBestPrice(
            @Parameter(description = "Channel identifier", required = true, example = "dach-sales-channel")
            @PathVariable("channelId") String channelId,
            @Parameter(description = "Country ISO key (Alpha-2)", required = true, example = "DE")
            @PathVariable("countryIsoKey") String countryIsoKey,
            @Parameter(description = "Priced resource identifier", required = true, example = "DEMO-PRODUCT-001")
            @PathVariable("pricedResourceId") String pricedResourceId,
            @Parameter(description = "Price type", required = true, example = "SALES_PRICE",
                    schema = @Schema(allowableValues = {"SALES_PRICE", "PURCHASE_PRICE", "MATERIAL_COST", "RENTAL_BASE_PRICE", "RENTAL_DAILY_RATE"}))
            @PathVariable("priceType") String priceType,
            @Parameter(description = "Quantity", required = true, example = "10.00")
            @RequestParam("quantity") BigDecimal quantity,
            @Parameter(description = "Unit reference", required = true, example = "piece")
            @RequestParam("unit") String unit,
            @Parameter(description = "Currency reference (optional - uses country's primary currency if not specified)", required = false, example = "EUR")
            @RequestParam(value = "currency", required = false) String currency,
            @Parameter(description = "Fields to expand in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @AuthenticationPrincipal Jwt jwt
    ) throws NotFoundException, DataMappingException, InvalidParameterException {
        String groupId = jwtClaimsExtractor.extractEffectiveOrganization(jwt);
        PriceType priceTypeEnum = parsePriceType(priceType);
        PublicPriceRestEntity result = publicPriceFacade.getBestPrice(
                channelId, countryIsoKey, groupId, pricedResourceId, quantity, unit, currency, priceTypeEnum, expand);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get all matching prices for channel and country",
            description = "Returns all prices matching the criteria filtered by channel and country.",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully retrieved matching prices",
                    content = @Content(schema = @Schema(implementation = PublicPriceListRestEntity.class)))})
    @PreAuthorize("hasAuthority('priceprovider.public:PriceRow:inspect')")
    @GetMapping("/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}/candidates")
    public ResponseEntity<PublicPriceListRestEntity> getAllPrices(
            @Parameter(description = "Channel identifier", required = true, example = "dach-sales-channel")
            @PathVariable("channelId") String channelId,
            @Parameter(description = "Country ISO key (Alpha-2)", required = true, example = "DE")
            @PathVariable("countryIsoKey") String countryIsoKey,
            @Parameter(description = "Priced resource identifier", required = true, example = "DEMO-PRODUCT-001")
            @PathVariable("pricedResourceId") String pricedResourceId,
            @Parameter(description = "Price type", required = true, example = "SALES_PRICE",
                    schema = @Schema(allowableValues = {"SALES_PRICE", "PURCHASE_PRICE", "MATERIAL_COST"}))
            @PathVariable("priceType") String priceType,
            @Parameter(description = "Quantity", required = true, example = "10.00")
            @RequestParam("quantity") BigDecimal quantity,
            @Parameter(description = "Unit reference", required = true, example = "piece")
            @RequestParam("unit") String unit,
            @Parameter(description = "Currency reference (optional - uses country's primary currency if not specified)", required = false, example = "EUR")
            @RequestParam(value = "currency", required = false) String currency,
            @Parameter(description = "Fields to expand in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @AuthenticationPrincipal Jwt jwt
    ) throws DataMappingException, InvalidParameterException, NotFoundException {
        String groupId = jwtClaimsExtractor.extractEffectiveOrganization(jwt);
        PriceType priceTypeEnum = parsePriceType(priceType);
        PublicPriceListRestEntity result = publicPriceFacade.getAllPrices(
                channelId, countryIsoKey, groupId, pricedResourceId, quantity, unit, currency, priceTypeEnum, expand);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get all quantity-specific best prices for channel and country",
            description = "Returns the best matching prices for every unique quantity break point found for the resource.",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully retrieved matching prices",
                    content = @Content(schema = @Schema(implementation = PublicPriceListRestEntity.class)))})
    @GetMapping("/{channelId}/{countryIsoKey}/pricerows/{priceType}/of/{pricedResourceId}/all-quantity-breaks")
    public ResponseEntity<PublicPriceListRestEntity> getAllQuantityBestPrices(
            @Parameter(description = "Channel identifier", required = true, example = "dach-sales-channel")
            @PathVariable("channelId") String channelId,
            @Parameter(description = "Country ISO key (Alpha-2)", required = true, example = "DE")
            @PathVariable("countryIsoKey") String countryIsoKey,
            @Parameter(description = "Priced resource identifier", required = true, example = "DEMO-PRODUCT-001")
            @PathVariable("pricedResourceId") String pricedResourceId,
            @Parameter(description = "Price type", required = true, example = "SALES_PRICE",
                    schema = @Schema(allowableValues = {"SALES_PRICE", "PURCHASE_PRICE", "MATERIAL_COST", "RENTAL_BASE_PRICE", "RENTAL_DAILY_RATE"}))
            @PathVariable("priceType") String priceType,
            @Parameter(description = "Unit reference", required = true, example = "piece")
            @RequestParam("unit") String unit,
            @Parameter(description = "Currency reference (optional - uses country's primary currency if not specified)", required = false, example = "EUR")
            @RequestParam(value = "currency", required = false) String currency,
            @Parameter(description = "Fields to expand in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @AuthenticationPrincipal Jwt jwt
    ) throws DataMappingException, InvalidParameterException, NotFoundException {
        String groupId = jwtClaimsExtractor.extractEffectiveOrganization(jwt);
        PriceType priceTypeEnum = parsePriceType(priceType);
        PublicPriceListRestEntity result = publicPriceFacade.getAllQuantityBestPrices(
                channelId, countryIsoKey, groupId, pricedResourceId, unit, currency, priceTypeEnum, expand);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get multiple best prices for channel and country",
            description = "Finds the best matching prices for multiple resource IDs. The net/gross representation is determined by the channel's price representation mode.",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully retrieved best prices",
                    content = @Content(schema = @Schema(implementation = PublicPriceListRestEntity.class)))})
    @GetMapping("/{channelId}/{countryIsoKey}/pricerows/{priceType}")
    public ResponseEntity<PublicPriceListRestEntity> getBestPrices(
            @Parameter(description = "Channel identifier", required = true, example = "dach-sales-channel")
            @PathVariable("channelId") String channelId,
            @Parameter(description = "Country ISO key (Alpha-2)", required = true, example = "DE")
            @PathVariable("countryIsoKey") String countryIsoKey,
            @Parameter(description = "Price type", required = true, example = "SALES_PRICE",
                    schema = @Schema(allowableValues = {"SALES_PRICE", "PURCHASE_PRICE", "MATERIAL_COST", "RENTAL_BASE_PRICE", "RENTAL_DAILY_RATE"}))
            @PathVariable("priceType") String priceType,
            @Parameter(description = "Priced resource identifiers (comma separated)", required = true, example = "DEMO-PRODUCT-001,DEMO-PRODUCT-002")
            @RequestParam("pricedresourceIds") Set<String> pricedResourceIds,
            @Parameter(description = "Quantity", required = true, example = "1.00")
            @RequestParam("quantity") BigDecimal quantity,
            @Parameter(description = "Unit reference", required = true, example = "piece")
            @RequestParam("unit") String unit,
            @Parameter(description = "Currency reference (optional - uses country's primary currency if not specified)", required = false, example = "EUR")
            @RequestParam(value = "currency", required = false) String currency,
            @Parameter(description = "Fields to expand in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,
            @AuthenticationPrincipal Jwt jwt
    ) throws DataMappingException, InvalidParameterException, NotFoundException {
        String groupId = jwtClaimsExtractor.extractEffectiveOrganization(jwt);
        PriceType priceTypeEnum = parsePriceType(priceType);
        PublicPriceListRestEntity result = publicPriceFacade.getBestPrices(
                channelId, countryIsoKey, groupId, pricedResourceIds, quantity, unit, currency, priceTypeEnum, expand);
        return ResponseEntity.ok(result);
    }

    /**
     * Parses the priceType path parameter into a PriceType.
     */
    private PriceType parsePriceType(String priceType) throws InvalidParameterException {
        String code = priceType.trim().toUpperCase();
        if (priceTypeRegistry.exists(code)) {
            return new PriceType(code);
        } else {
            throw new InvalidParameterException(
                    "Invalid priceType value '" + priceType + "'. Valid values are: " +
                    priceTypeRegistry.getCodes());
        }
    }
}
