package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.TaxClassFacade;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import io.commercestacksolutions.commons.web.rest.Message;
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
@RequestMapping("/admin/api/taxclasses")
@Tag(name = "Tax Classes", description = "Tax class management API - handles tax rates and classifications")
public class TaxClassController {

    private final TaxClassFacade taxClassFacade;

    @Autowired
    public TaxClassController(TaxClassFacade taxClassFacade) {
        this.taxClassFacade = taxClassFacade;
    }

    @Operation(
        summary = "Get list of tax classes",
        description = "Retrieves a paginated and sortable list with optional filtering of tax classes. Supports sorting by: taxClassId, taxRate",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tax classes", 
                content = @Content(schema = @Schema(implementation = TaxClassListRestEntity.class)))
        }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:TaxClass:read')") 

    @GetMapping
    public TaxClassListRestEntity getTaxClasses(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,
            
            @Parameter(description = "Field(s) to sort by. Valid values: taxClassId, taxRate. Can be specified multiple times for multi-field sorting", 
                example = "taxClassId")
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
        return taxClassFacade.getTaxClasses(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
        summary = "Get tax class by ID",
        description = "Retrieves a single tax class by its unique identifier",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tax class",
                content = @Content(schema = @Schema(implementation = TaxClassRestEntity.class)))
        }
    )
    @GetMapping("/{taxClassId}")
    public TaxClassRestEntity getTaxClass(
            @Parameter(description = "Tax class ID (e.g., 'de-vat-full', 'de-vat-reduced')", example = "de-vat-full")
            @PathVariable("taxClassId") String taxClassId,
            
            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws io.commercestacksolutions.commons.exception.NotFoundException, DataMappingException {
        return taxClassFacade.getTaxClass(taxClassId, expand);
    }

    @Operation(
            summary = "Get meta information for tax classes",
            description = "Returns identity fields, mandatory fields and enum values for the TaxClass entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return taxClassFacade.getMeta();
    }

    @Operation(
        summary = "Partially update tax class",
        description = "Applies JSON Patch operations to partially update a tax class. Supports operations: add, remove, replace.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully patched tax class",
                content = @Content(schema = @Schema(implementation = TaxClassRestEntity.class)))
        }
    )
    @PatchMapping("/{taxClassId}")
    public TaxClassRestEntity patch(
            @Parameter(description = "Tax class ID (e.g., 'de-vat-full', 'de-vat-reduced')", example = "de-vat-full")
            @PathVariable("taxClassId") String taxClassId,
            
            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, io.commercestacksolutions.commons.exception.NotFoundException, EntityValidationException {
        return taxClassFacade.patch(taxClassId, patch);
    }

    @Operation(
        summary = "Create or update tax class",
        description = "Creates a new tax class or fully replaces an existing tax class with the provided data",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully created or updated tax class",
                content = @Content(schema = @Schema(implementation = TaxClassRestEntity.class)))
        }
    )
    @PutMapping("/{taxClassId}")
    public TaxClassRestEntity createOrRecreate(
            @Parameter(description = "Tax class ID (e.g., 'de-vat-full', 'de-vat-reduced')", example = "de-vat-full")
            @PathVariable("taxClassId") String taxClassId,
            
            @Parameter(description = "Tax class data")
            @RequestBody TaxClassRestEntity taxClassRestEntity
    ) throws DataMappingException, EntityValidationException {
        return taxClassFacade.createOrRecreate(taxClassId, taxClassRestEntity);
    }

    @Operation(
        summary = "Create new tax class",
        description = "Creates a new tax class with a server-assigned or provided tax class ID. The tax class ID must be included in the request body.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully created tax class",
                content = @Content(schema = @Schema(implementation = TaxClassRestEntity.class))),
            @ApiResponse(responseCode = "409", description = "Tax class with the same ID already exists")
        }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:TaxClass:write')") 

    @PostMapping("/create")
    public ResponseEntity<TaxClassRestEntity> create(
            @Parameter(description = "Tax class data including tax class ID")
            @RequestBody TaxClassRestEntity taxClassRestEntity
    ) throws DataMappingException, EntityValidationException {
        TaxClassRestEntity result = taxClassFacade.create(taxClassRestEntity);
        
        // Check if result contains error messages
        List<Message> msgs = result.getMessages();
        if (msgs != null && !msgs.isEmpty()) {
            for (Message msg : msgs) {
                if (msg.getType() == Message.MessageType.ERROR) {
                    Integer statusCode = msg.getStatusCode();
                    return ResponseEntity.status(statusCode != null ? statusCode : HttpStatus.BAD_REQUEST.value()).body(result);
                }
            }
        }
        
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Delete tax class",
        description = "Deletes a tax class. Will fail if there are price rows referencing this tax class.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted tax class"),
            @ApiResponse(responseCode = "404", description = "Tax class not found"),
            @ApiResponse(responseCode = "409", description = "Cannot delete tax class - it is referenced by price rows")
        }
    )
    @DeleteMapping("/{taxClassId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Tax class ID (e.g., 'de-vat-full', 'de-vat-reduced')", example = "de-vat-full")
            @PathVariable("taxClassId") String taxClassId
    ) {
        try {
            taxClassFacade.delete(taxClassId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
        summary = "Bulk delete tax classes",
        description = "Deletes multiple tax classes by their tax class IDs. Will fail if any tax class is referenced by price rows.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted tax classes"),
            @ApiResponse(responseCode = "409", description = "Cannot delete one or more tax classes - they are referenced by price rows")
        }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:TaxClass:delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteTaxClasses(
            @Parameter(description = "List of tax class IDs to delete")
            @RequestBody List<String> taxClassIds
    ) throws io.commercestacksolutions.commons.exception.DataIntegrityException {
        taxClassFacade.bulkDeleteTaxClasses(taxClassIds);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Create or update multiple tax classes",
        description = "Creates or updates multiple tax classes in a single request. Returns partial results with error messages for items that fail.",
        responses = {
            @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed tax classes (check individual items for errors)",
                content = @Content(schema = @Schema(implementation = TaxClassListRestEntity.class)))
        }
    )
    @PostMapping("/create-or-update-all")
    public ResponseEntity<TaxClassListRestEntity> createOrUpdateAllTaxClasses(
            @Parameter(description = "List of tax class data to create or update")
            @RequestBody List<TaxClassRestEntity> taxClassRestEntities
    ) {
        TaxClassListRestEntity result = taxClassFacade.createOrUpdateAllTaxClasses(taxClassRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}



