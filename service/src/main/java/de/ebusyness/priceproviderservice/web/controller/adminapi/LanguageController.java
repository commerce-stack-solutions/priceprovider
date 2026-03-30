package de.ebusyness.priceproviderservice.web.controller.adminapi;

import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.facade.language.LanguageFacade;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageListRestEntity;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageRestEntity;
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
@RequestMapping("/admin/api/languages")
@Tag(name = "Languages", description = "Language management API - handles available languages configuration")
public class LanguageController {

    private final LanguageFacade languageFacade;

    @Autowired
    public LanguageController(LanguageFacade languageFacade) {
        this.languageFacade = languageFacade;
    }

    @Operation(
            summary = "Get list of languages",
            description = "Retrieves a paginated and sortable list of languages. " +
                         "Supports advanced filtering using query parameter 'q' with Lucene-like syntax (e.g., 'active:true'). " +
                         "Supports sorting by: isoKey, active, mandatory",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved languages",
                            content = @Content(schema = @Schema(implementation = LanguageListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Language:read')") 

    @GetMapping
    public LanguageListRestEntity getLanguages(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: isoKey, active, mandatory. Can be specified multiple times for multi-field sorting",
                    example = "isoKey")
            @RequestParam(value = "sort-by", required = false) List<String> sortBy,

            @Parameter(description = "Sort direction: asc (ascending) or desc (descending)", example = "asc",
                    schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(value = "sort-direction", required = false) String sortDirection,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'active:true', 'mandatory:true AND active:true', 'isoKey:en OR isoKey:de'",
                    example = "active:true AND mandatory:true")
            @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return languageFacade.getLanguages(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get language by ISO key",
            description = "Retrieves a single language by its unique ISO key identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved language",
                            content = @Content(schema = @Schema(implementation = LanguageRestEntity.class)))
            }
    )
    @GetMapping("/{isoKey}")
    public LanguageRestEntity getLanguage(
            @Parameter(description = "Language ISO key (e.g., 'en' for English, 'de' for German)", example = "en")
            @PathVariable("isoKey") String isoKey,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        return languageFacade.getLanguage(isoKey, expand);
    }

    @Operation(
            summary = "Get meta information for languages",
            description = "Returns identity fields, mandatory fields and enum values for the Language entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return languageFacade.getMeta();
    }

    @Operation(
            summary = "Partially update language",
            description = "Applies JSON Patch operations to partially update a language. Supports operations: add, remove, replace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched language",
                            content = @Content(schema = @Schema(implementation = LanguageRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PatchMapping("/{isoKey}")
    public LanguageRestEntity patch(
            @Parameter(description = "Language ISO key (e.g., 'en' for English, 'de' for German)", example = "en")
            @PathVariable("isoKey") String isoKey,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return languageFacade.patch(isoKey, patch);
    }

    @Operation(
            summary = "Create or update language",
            description = "Creates a new language or fully replaces an existing language with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated language",
                            content = @Content(schema = @Schema(implementation = LanguageRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PutMapping("/{isoKey}")
    public LanguageRestEntity createOrRecreate(
            @Parameter(description = "Language ISO key (e.g., 'en' for English, 'de' for German)", example = "en")
            @PathVariable("isoKey") String isoKey,

            @Parameter(description = "Language data")
            @RequestBody LanguageRestEntity languageRestEntity
    ) throws DataMappingException, EntityValidationException {
        return languageFacade.createOrReCreate(isoKey, languageRestEntity);
    }

    @Operation(
            summary = "Create new language",
            description = "Creates a new language with a server-assigned or provided ISO key. The ISO key must be included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created language",
                            content = @Content(schema = @Schema(implementation = LanguageRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Language with the same ISO key already exists")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Language:write')") 

    @PostMapping("/create")
    public LanguageRestEntity create(
            @Parameter(description = "Language data including ISO key")
            @RequestBody LanguageRestEntity languageRestEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return languageFacade.create(languageRestEntity);
    }

    @Operation(
            summary = "Create or update multiple languages",
            description = "Creates new languages or updates existing ones in a single batch operation. Maximum 100 languages per request. " +
                    "Entities with existing ISO keys will be updated. Entities with new keys will be created.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed languages",
                            content = @Content(schema = @Schema(implementation = LanguageListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size",
                            content = @Content(schema = @Schema(implementation = LanguageListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Language:write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<LanguageListRestEntity> createOrUpdateAllLanguages(
            @Parameter(description = "List of language data (max 100 items). ISO key is required for all entities.")
            @RequestBody List<LanguageRestEntity> languageRestEntities
    ) throws DataMappingException, InvalidParameterException {
        LanguageListRestEntity result = languageFacade.createOrUpdateAllLanguages(languageRestEntities);

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

    @Operation(
            summary = "Delete language",
            description = "Deletes a language by its ISO key",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted language"),
                    @ApiResponse(responseCode = "404", description = "Language not found")
            }
    )
    @DeleteMapping("/{isoKey}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Language ISO key (e.g., 'en' for English, 'de' for German)", example = "en")
            @PathVariable("isoKey") String isoKey
    ) throws NotFoundException {
        try {
            languageFacade.deleteLanguage(isoKey);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Bulk delete languages",
            description = "Deletes multiple languages by their ISO keys",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted languages")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Language:delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteLanguages(
            @Parameter(description = "List of language ISO keys to delete")
            @RequestBody List<String> isoKeys
    ) throws DataIntegrityException {
        languageFacade.bulkDeleteLanguages(isoKeys);
        return ResponseEntity.noContent().build();
    }
}



