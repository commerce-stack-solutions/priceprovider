package de.ebusyness.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.country.CountryFacade;
import de.ebusyness.priceproviderservice.facade.country.restentity.CountryListRestEntity;
import de.ebusyness.priceproviderservice.facade.country.restentity.CountryRestEntity;
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
@RequestMapping("/admin/api/countries")
@Tag(name = "Countries", description = "Country management API - handles country definitions and ISO keys")
public class CountryController {

    private final CountryFacade countryFacade;

    @Autowired
    public CountryController(CountryFacade countryFacade) {
        this.countryFacade = countryFacade;
    }

    @Operation(
            summary = "Get list of countries",
            description = "Retrieves a paginated and sortable list with optional filtering of countries. Supports sorting by: isoKey, name",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved countries",
                            content = @Content(schema = @Schema(implementation = CountryListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Country:read')") 

    @GetMapping
    public CountryListRestEntity getCountries(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: isoKey. Can be specified multiple times for multi-field sorting",
                    example = "isoKey")
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
        return countryFacade.getCountries(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get country by ISO key",
            description = "Retrieves a single country by its ISO Alpha-2 key",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved country",
                            content = @Content(schema = @Schema(implementation = CountryRestEntity.class)))
            }
    )
    @GetMapping("/{isoKey}")
    public CountryRestEntity getCountry(
            @Parameter(description = "ISO Alpha-2 country key (e.g., 'DE', 'US', 'FR')", example = "DE")
            @PathVariable("isoKey") String isoKey,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws de.ebusyness.commons.exception.NotFoundException, DataMappingException {
        return countryFacade.getCountry(isoKey, expand);
    }

    @Operation(
            summary = "Get meta information for countries",
            description = "Returns identity fields, mandatory fields and enum values for the Country entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return countryFacade.getMeta();
    }

    @Operation(
            summary = "Partially update country",
            description = "Applies JSON Patch operations to partially update a country. Supports operations: add, remove, replace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched country",
                            content = @Content(schema = @Schema(implementation = CountryRestEntity.class)))
            }
    )
    @PatchMapping("/{isoKey}")
    public CountryRestEntity patch(
            @Parameter(description = "ISO Alpha-2 country key (e.g., 'DE', 'US', 'FR')", example = "DE")
            @PathVariable("isoKey") String isoKey,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, de.ebusyness.commons.exception.NotFoundException, EntityValidationException {
        return countryFacade.patch(isoKey, patch);
    }

    @Operation(
            summary = "Create or update country",
            description = "Creates a new country or fully replaces an existing country with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated country",
                            content = @Content(schema = @Schema(implementation = CountryRestEntity.class)))
            }
    )
    @PutMapping("/{isoKey}")
    public CountryRestEntity createOrRecreate(
            @Parameter(description = "ISO Alpha-2 country key (e.g., 'DE', 'US', 'FR')", example = "DE")
            @PathVariable("isoKey") String isoKey,

            @Parameter(description = "Country data")
            @RequestBody CountryRestEntity countryRestEntity
    ) throws DataMappingException, EntityValidationException {
        return countryFacade.createOrRecreate(isoKey, countryRestEntity);
    }

    @Operation(
            summary = "Create new country",
            description = "Creates a new country with the ISO key included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created country",
                            content = @Content(schema = @Schema(implementation = CountryRestEntity.class))),
                    @ApiResponse(responseCode = "409", description = "Country with the same ISO key already exists")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Country:write')") 

    @PostMapping("/create")
    public ResponseEntity<CountryRestEntity> create(
            @Parameter(description = "Country data including ISO key")
            @RequestBody CountryRestEntity countryRestEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        CountryRestEntity result = countryFacade.create(countryRestEntity);

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
            summary = "Delete country",
            description = "Deletes a country. Will fail if there are tax classes or channels referencing this country.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted country"),
                    @ApiResponse(responseCode = "404", description = "Country not found"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete country - it is referenced by tax classes or channels")
            }
    )
    @DeleteMapping("/{isoKey}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ISO Alpha-2 country key (e.g., 'DE', 'US', 'FR')", example = "DE")
            @PathVariable("isoKey") String isoKey
    ) {
        try {
            countryFacade.delete(isoKey);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Bulk delete countries",
            description = "Deletes multiple countries by their ISO keys. Will fail if any country is referenced by tax classes or channels.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted countries"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete one or more countries - they are referenced by other entities")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Country:delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteCountries(
            @Parameter(description = "List of ISO keys to delete")
            @RequestBody List<String> isoKeys
    ) throws de.ebusyness.commons.exception.DataIntegrityException {
        countryFacade.bulkDeleteCountries(isoKeys);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple countries",
            description = "Creates or updates multiple countries in a single request. Returns partial results with error messages for items that fail.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed countries (check individual items for errors)",
                            content = @Content(schema = @Schema(implementation = CountryListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Country:write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<CountryListRestEntity> createOrUpdateAllCountries(
            @Parameter(description = "List of country data to create or update")
            @RequestBody List<CountryRestEntity> countryRestEntities
    ) {
        CountryListRestEntity result = countryFacade.createOrUpdateAllCountries(countryRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}


