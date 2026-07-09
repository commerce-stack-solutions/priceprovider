package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.facade.currency.CurrencyFacade;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
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
@RequestMapping("/admin/api/currencies")
@Tag(name = "Currencies", description = "Currency management API - handles currency definitions")
public class CurrencyController {

    private final CurrencyFacade currencyFacade;

    @Autowired
    public CurrencyController(CurrencyFacade currencyFacade) {
        this.currencyFacade = currencyFacade;
    }

    @Operation(
            summary = "Get list of currencies",
            description = "Retrieves a paginated and sortable list with optional filtering of currencies. Supports sorting by: currencyKey, symbol",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved currencies",
                            content = @Content(schema = @Schema(implementation = CurrencyListRestEntity.class)))
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Currency', 'read')") 

    @GetMapping
    public CurrencyListRestEntity getCurrencies(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: currencyKey, symbol. Can be specified multiple times for multi-field sorting",
                    example = "currencyKey")
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,

            @Parameter(description = "Sort direction: asc (ascending) or desc (descending)", example = "asc",
                    schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(value = "sort-direction", required = false) String sortDirection,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return currencyFacade.getCurrencies(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get currency by key",
            description = "Retrieves a single currency by its unique currency key identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved currency",
                            content = @Content(schema = @Schema(implementation = CurrencyRestEntity.class)))
            }
    )
    @GetMapping("/{currencyKey}")
    public CurrencyRestEntity getCurrency(
            @Parameter(description = "Currency key (e.g., 'USD', 'EUR')", example = "USD")
            @PathVariable("currencyKey") String currencyKey,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> epxand
    ) throws NotFoundException, DataMappingException {
        return currencyFacade.getCurrency(currencyKey, epxand);
    }

    @Operation(
            summary = "Get meta information for currencies",
            description = "Returns identity fields, mandatory fields and enum values for the Currency entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return currencyFacade.getMeta();
    }

    @Operation(
            summary = "Partially update currency",
            description = "Applies JSON Patch operations to partially update a currency. Supports operations: add, remove, replace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched currency",
                            content = @Content(schema = @Schema(implementation = CurrencyRestEntity.class)))
            }
    )
    @PatchMapping("/{currencyKey}")
    public CurrencyRestEntity patch(
            @Parameter(description = "Currency key (e.g., 'USD', 'EUR')", example = "USD")
            @PathVariable("currencyKey") String currencyKey,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return currencyFacade.patch(currencyKey, patch);
    }

    @Operation(
            summary = "Create or update currency",
            description = "Creates a new currency or fully replaces an existing currency with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated currency",
                            content = @Content(schema = @Schema(implementation = CurrencyRestEntity.class)))
            }
    )
    @PutMapping("/{currencyKey}")
    public CurrencyRestEntity createOrRecreate(
            @Parameter(description = "Currency key (e.g., 'USD', 'EUR')", example = "USD")
            @PathVariable("currencyKey") String currencyKey,

            @Parameter(description = "Currency data")
            @RequestBody CurrencyRestEntity currencyRestEntity
    ) throws DataMappingException, EntityValidationException {
        return currencyFacade.createOrRecreate(currencyKey, currencyRestEntity);
    }

    @Operation(
            summary = "Create new currency",
            description = "Creates a new currency with a server-assigned or provided currency key. The currency key must be included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created currency",
                            content = @Content(schema = @Schema(implementation = CurrencyRestEntity.class))),
                    @ApiResponse(responseCode = "409", description = "Currency with the same key already exists")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Currency', 'write')") 

    @PostMapping("/create")
    public CurrencyRestEntity create(
            @Parameter(description = "Currency data including currency key")
            @RequestBody CurrencyRestEntity currencyRestEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return currencyFacade.create(currencyRestEntity);
    }

    @Operation(
            summary = "Delete currency",
            description = "Deletes a currency. Will fail if there are price rows referencing this currency.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted currency"),
                    @ApiResponse(responseCode = "404", description = "Currency not found"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete currency - it is referenced by price rows")
            }
    )
    @DeleteMapping("/{currencyKey}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Currency key (e.g., 'USD', 'EUR')", example = "USD")
            @PathVariable("currencyKey") String currencyKey
    ) {
        try {
            currencyFacade.delete(currencyKey);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Bulk delete currencies",
            description = "Deletes multiple currencies by their currency keys. Will fail if any currency is referenced by price rows.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted currencies"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete one or more currencies - they are referenced by price rows")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Currency', 'delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteCurrencies(
            @Parameter(description = "List of currency keys to delete")
            @RequestBody List<String> currencyKeys
    ) throws DataIntegrityException {
        currencyFacade.bulkDeleteCurrencies(currencyKeys);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple currencies",
            description = "Creates new currencies or updates existing ones in a single batch operation. Maximum 100 currencies per request. " +
                    "Entities with existing currency keys will be updated. Entities with new keys will be created.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed currencies",
                            content = @Content(schema = @Schema(implementation = CurrencyListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size",
                            content = @Content(schema = @Schema(implementation = CurrencyListRestEntity.class)))
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Currency', 'write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<CurrencyListRestEntity> createOrUpdateAllCurrencies(
            @Parameter(description = "List of currency data (max 100 items). Currency key is required for all entities.")
            @RequestBody List<CurrencyRestEntity> currencyRestEntities
    ) throws DataMappingException, InvalidParameterException {
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencyRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}



