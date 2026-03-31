package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.PriceRowFacade;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/admin/api/pricerows")
@Tag(name = "Price Rows", description = "Price row management API - handles pricing information for products")
public class PriceRowController {

    private final PriceRowFacade priceRowFacade;

    @Autowired
    public PriceRowController(PriceRowFacade priceRowFacade) {
        this.priceRowFacade = priceRowFacade;
    }

    @Operation(
            summary = "Get list of price rows",
            description = "Retrieves a paginated and sortable list with optional filtering of price rows. Supports sorting by: id, pricedResourceId, priceValue, minQuantity, unit.symbol, currency, taxIncluded, validFrom, validTo, customerId",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved price rows",
                            content = @Content(schema = @Schema(implementation = PriceRowListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:read')") 

    @GetMapping
    public PriceRowListRestEntity getPriceRows(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: id, pricedResourceId, priceValue, minQuantity, unit.symbol, currency, taxIncluded, validFrom, validTo, customerId. Can be specified multiple times for multi-field sorting",
                    example = "pricedResourceId")
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,

            @Parameter(description = "Sort direction: asc (ascending) or desc (descending)", example = "asc",
                    schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(value = "sort-direction", required = false) String sortDirection,

            @Parameter(description = "Fields to expand in response. Use comma-separated paths like $info.taxation,$includes.unit,$includes.currency,$includes.taxClass",
                    example = "$info.taxation,$includes.unit")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return priceRowFacade.getPriceRows(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get price row by ID",
            description = "Retrieves a single price row by its unique identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved price row",
                            content = @Content(schema = @Schema(implementation = PriceRowRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:read')") 

    @GetMapping("/{id}")
    public ResponseEntity<PriceRowRestEntity> getPriceRow(
            @Parameter(description = "Price row ID", example = "1")
            @PathVariable("id") Long id,

            @Parameter(description = "Fields to expand in response. Use comma-separated paths like $info.taxation,$includes.unit,$includes.currency,$includes.taxClass",
                    example = "$info.taxation,$includes.unit")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, io.commercestacksolutions.commons.exception.NotFoundException {
        PriceRowRestEntity result = priceRowFacade.getPriceRow(id, expand);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get meta information for price rows",
            description = "Returns identity fields, mandatory fields and enum values for the PriceRow entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return priceRowFacade.getMeta();
    }

    @Operation(
            summary = "Update price row",
            description = "Fully replaces a price row with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated price row",
                            content = @Content(schema = @Schema(implementation = PriceRowRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(schema = @Schema(implementation = PriceRowRestEntity.class))),
                    @ApiResponse(responseCode = "404", description = "Price row not found",
                            content = @Content(schema = @Schema(implementation = PriceRowRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:write')") 

    @PutMapping("/{id}")
    public PriceRowRestEntity createOrRecreate(
            @Parameter(description = "Price row ID", example = "1")
            @PathVariable("id") Long id,

            @Parameter(description = "Price row data")
            @RequestBody PriceRowRestEntity priceRowRestEntity
    ) throws DataMappingException, EntityValidationException {
        return priceRowFacade.createOrRecreate(id, priceRowRestEntity);
    }

    @Operation(
            summary = "Partially update price row",
            description = "Applies JSON Patch operations to partially update a price row. Supports operations: add, remove, replace",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched price row",
                            content = @Content(schema = @Schema(implementation = PriceRowRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:write')") 

    @PatchMapping("/{id}")
    public PriceRowRestEntity patch(
            @Parameter(description = "Price row ID", example = "1")
            @PathVariable("id") Long id,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, io.commercestacksolutions.commons.exception.NotFoundException, EntityValidationException {
        return priceRowFacade.patch(id, patch);
    }

    @Operation(
            summary = "Create new price row",
            description = "Creates a new price row with a server-generated ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created price row",
                            content = @Content(schema = @Schema(implementation = PriceRowRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:write')") 

    @PostMapping("/create")
    public ResponseEntity<PriceRowRestEntity> create(
            @Parameter(description = "Price row data (ID will be generated by the server)")
            @RequestBody PriceRowRestEntity priceRowRestEntity
    ) throws DataMappingException, InvalidParameterException {
        PriceRowRestEntity result = priceRowFacade.create(priceRowRestEntity);

        // Check if result contains error messages
        java.util.List<Message> msgs = result.getMessages();
        if (msgs != null && !msgs.isEmpty()) {
            for (Message msg : msgs) {
                if (msg.getType() == Message.MessageType.ERROR) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
                }
            }
        }

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Delete price row",
            description = "Deletes a price row by its ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted price row"),
                    @ApiResponse(responseCode = "404", description = "Price row not found")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:delete')") 

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Price row ID", example = "1")
            @PathVariable("id") Long id
    ) throws NotFoundException {
        priceRowFacade.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Bulk delete price rows",
            description = "Deletes multiple price rows by their IDs",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted price rows")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeletePriceRows(
            @Parameter(description = "List of price row IDs to delete")
            @RequestBody List<Long> ids
    ) throws io.commercestacksolutions.commons.exception.DataIntegrityException {
        priceRowFacade.bulkDeletePriceRows(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple price rows",
            description = "Creates new price rows or updates existing ones in a single batch operation. Maximum 100 price rows per request. " +
                    "Entities with IDs will be updated if they exist, created if they don't. Entities without IDs will be created with server-generated IDs.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed price rows",
                            content = @Content(schema = @Schema(implementation = PriceRowListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size",
                            content = @Content(schema = @Schema(implementation = PriceRowListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:PriceRow:write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<PriceRowListRestEntity> createOrUpdateAllPriceRows(
            @Parameter(description = "List of price row data (max 100 items). Include ID for updates, omit ID for creates.")
            @RequestBody List<PriceRowRestEntity> priceRowRestEntities
    ) throws DataMappingException, EntityValidationException {
        PriceRowListRestEntity result = priceRowFacade.createOrUpdateAllPriceRows(priceRowRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}


