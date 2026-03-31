package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.facade.unit.UnitFacadeService;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitRestEntity;
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
@RequestMapping("/admin/api/units")
@Tag(name = "Units", description = "Unit management API - handles measurement units like meters, kilograms, etc.")
public class UnitController {

    private final UnitFacadeService unitFacade;

    @Autowired
    public UnitController(UnitFacadeService unitFacade) {
        this.unitFacade = unitFacade;
    }

    @Operation(
            summary = "Get list of units",
            description = "Retrieves a paginated and sortable list of measurement units. " +
                         "Supports advanced filtering using query parameter 'q' with Lucene-like syntax. " +
                         "Supports sorting by: symbol, name, measure, baseUnit, factor",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved units",
                            content = @Content(schema = @Schema(implementation = UnitListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Unit:read')") 

    @GetMapping
    public UnitListRestEntity getUnits(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: symbol, name, measure, baseUnit, factor. Can be specified multiple times for multi-field sorting",
                    example = "symbol")
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,

            @Parameter(description = "Sort direction: asc (ascending) or desc (descending)", example = "asc",
                    schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(value = "sort-direction", required = false) String sortDirection,

            @Parameter(description = "Fields to expand in response", example = "$includes.baseUnit")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'measure:length', 'symbol:m OR symbol:kg', 'factor:[1 TO 1000]'",
                    example = "measure:length AND factor.exists:true")
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        // QueryFilterRuntimeException is handled by ExceptionHandlerAdvice global handler
        return unitFacade.getUnits(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get unit by symbol",
            description = "Retrieves a single unit by its unique symbol identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved unit",
                            content = @Content(schema = @Schema(implementation = UnitRestEntity.class)))
            }
    )
    @GetMapping("/{symbol}")
    public UnitRestEntity getUnit(
            @Parameter(description = "Unit symbol (e.g., 'm' for meter, 'kg' for kilogram)", example = "m")
            @PathVariable("symbol") String symbol,

            @Parameter(description = "Fields to expand in response", example = "$includes.baseUnit")
            @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        return unitFacade.getUnit(symbol, expand);
    }

    @Operation(
            summary = "Get meta information for units",
            description = "Returns identity fields, mandatory fields and enum values for the Unit entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return unitFacade.getMeta();
    }

    @Operation(
            summary = "Partially update unit",
            description = "Applies JSON Patch operations to partially update a unit. Supports operations: add, remove, replace. Can update nested Map fields like /name/en",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched unit",
                            content = @Content(schema = @Schema(implementation = UnitRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PatchMapping("/{symbol}")
    public UnitRestEntity patch(
            @Parameter(description = "Unit symbol (e.g., 'm' for meter, 'kg' for kilogram)", example = "m")
            @PathVariable("symbol") String symbol,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws NotFoundException, DataMappingException, EntityValidationException {
        return unitFacade.patch(symbol, patch);
    }

    @Operation(
            summary = "Create or update unit",
            description = "Creates a new unit or fully replaces an existing unit with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated unit",
                            content = @Content(schema = @Schema(implementation = UnitRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PutMapping("/{symbol}")
    public UnitRestEntity createOrRecreate(
            @Parameter(description = "Unit symbol (e.g., 'm' for meter, 'kg' for kilogram)", example = "m")
            @PathVariable("symbol") String symbol,

            @Parameter(description = "Unit data")
            @RequestBody UnitRestEntity unitRestEntity
    ) throws DataMappingException, EntityValidationException {
        return unitFacade.createOrRecreate(symbol, unitRestEntity);
    }

    @Operation(
            summary = "Create new unit",
            description = "Creates a new unit with a server-assigned or provided symbol. The symbol must be included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created unit",
                            content = @Content(schema = @Schema(implementation = UnitRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or unit with the same symbol already exists")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Unit:write')") 

    @PostMapping("/create")
    public UnitRestEntity create(
            @Parameter(description = "Unit data including symbol")
            @RequestBody UnitRestEntity unitRestEntity
    ) throws DataMappingException, EntityAlreadyExistsException, InvalidParameterException, EntityValidationException {
        return unitFacade.create(unitRestEntity);
    }

    @Operation(
            summary = "Delete unit",
            description = "Deletes a unit. Will fail if there are price rows referencing this unit.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted unit"),
                    @ApiResponse(responseCode = "404", description = "Unit not found"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete unit - it is referenced by price rows")
            }
    )
    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Unit symbol (e.g., 'm' for meter, 'kg' for kilogram)", example = "m")
            @PathVariable("symbol") String symbol
    ) {
        try {
            unitFacade.delete(symbol);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Bulk delete units",
            description = "Deletes multiple units by their symbols",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted units"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete unit - it is referenced by other entities")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Unit:delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteUnits(
            @Parameter(description = "List of unit symbols to delete")
            @RequestBody List<String> symbols
    ) throws DataIntegrityException {
        unitFacade.bulkDeleteUnits(symbols);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple units",
            description = "Creates new units or updates existing ones in a single batch operation. Maximum 100 units per request. " +
                    "Entities with existing symbols will be updated. Entities with new symbols will be created.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed units",
                            content = @Content(schema = @Schema(implementation = UnitListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size",
                            content = @Content(schema = @Schema(implementation = UnitListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Unit:write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<UnitListRestEntity> createOrUpdateAllUnits(
            @Parameter(description = "List of unit data (max 100 items). Symbol is required for all entities.")
            @RequestBody List<UnitRestEntity> unitRestEntities
    ) throws DataMappingException, InvalidParameterException {
        UnitListRestEntity result = unitFacade.createOrUpdateAllUnits(unitRestEntities);

        // Check if result contains error messages
        List<Message> msgs = result.getMessages();
        if (msgs != null && !msgs.isEmpty()) {
            for (Message msg : msgs) {
                if (msg.getType() == Message.MessageType.ERROR) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
                }
            }
        }

        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}


