package io.commercestacksolutions.corebusinessentities.web.controller.adminapi;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.corebusinessentities.facade.language.LanguageFacade;
import io.commercestacksolutions.corebusinessentities.facade.language.restentity.LanguageListRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.language.restentity.LanguageRestEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/api/languages")
@Tag(name = "Languages", description = "Language management API - handles available languages configuration")
public class LanguageController {

    private final LanguageFacade languageFacade;

    @Autowired
    public LanguageController(LanguageFacade languageFacade) {
        this.languageFacade = languageFacade;
    }

    @Operation(summary = "Get list of languages", description = "Retrieves a paginated and sortable list of languages.",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully retrieved languages",
                    content = @Content(schema = @Schema(implementation = LanguageListRestEntity.class)))})
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Language', 'read')")
    @GetMapping
    public LanguageListRestEntity getLanguages(
            @Parameter(description = "Page number (0-based)") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(value = "page-size", defaultValue = "10") int pageSize,
            @Parameter(description = "Field(s) to sort by") @RequestParam(value = "sort-by", required = false) List<String> sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(value = "sort-direction", required = false) String sortDirection,
            @Parameter(description = "Optional related data to include") @RequestParam(value = "$expand", required = false) Set<String> expand,
            @Parameter(description = "Query string for filtering") @RequestParam(value = "q", required = false) String query
    ) throws DataMappingException, InvalidParameterException, QueryParseException {
        return languageFacade.getLanguages(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(summary = "Get language by ISO key",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully retrieved language",
                    content = @Content(schema = @Schema(implementation = LanguageRestEntity.class)))})
    @GetMapping("/{isoKey}")
    public LanguageRestEntity getLanguage(
            @Parameter(description = "Language ISO key") @PathVariable("isoKey") String isoKey,
            @Parameter(description = "Optional related data to include") @RequestParam(value = "$expand", required = false) Set<String> expand
    ) throws NotFoundException, DataMappingException {
        return languageFacade.getLanguage(isoKey, expand);
    }

    @Operation(summary = "Get meta information for languages",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                    content = @Content(schema = @Schema(implementation = MetaInfo.class)))})
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return languageFacade.getMeta();
    }

    @Operation(summary = "Partially update language",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully patched language",
                    content = @Content(schema = @Schema(implementation = LanguageRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")})
    @PatchMapping("/{isoKey}")
    public LanguageRestEntity patch(
            @Parameter(description = "Language ISO key") @PathVariable("isoKey") String isoKey,
            @Parameter(description = "JSON Patch operations") @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return languageFacade.patch(isoKey, patch);
    }

    @Operation(summary = "Create or update language",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully created or updated language",
                    content = @Content(schema = @Schema(implementation = LanguageRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")})
    @PutMapping("/{isoKey}")
    public LanguageRestEntity createOrRecreate(
            @Parameter(description = "Language ISO key") @PathVariable("isoKey") String isoKey,
            @Parameter(description = "Language data") @RequestBody LanguageRestEntity languageRestEntity
    ) throws DataMappingException, EntityValidationException {
        return languageFacade.createOrReCreate(isoKey, languageRestEntity);
    }

    @Operation(summary = "Create new language",
            responses = {@ApiResponse(responseCode = "200", description = "Successfully created language",
                    content = @Content(schema = @Schema(implementation = LanguageRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Language already exists")})
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Language', 'write')")
    @PostMapping("/create")
    public LanguageRestEntity create(
            @Parameter(description = "Language data including ISO key") @RequestBody LanguageRestEntity languageRestEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return languageFacade.create(languageRestEntity);
    }

    @Operation(summary = "Create or update multiple languages",
            responses = {@ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed languages",
                    content = @Content(schema = @Schema(implementation = LanguageListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")})
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Language', 'write')")
    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<LanguageListRestEntity> createOrUpdateAllLanguages(
            @Parameter(description = "List of language data (max 100 items)") @RequestBody List<LanguageRestEntity> languageRestEntities
    ) throws DataMappingException, InvalidParameterException {
        LanguageListRestEntity result = languageFacade.createOrUpdateAllLanguages(languageRestEntities);

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

    @Operation(summary = "Delete language",
            responses = {@ApiResponse(responseCode = "204", description = "Successfully deleted language"),
                    @ApiResponse(responseCode = "404", description = "Language not found")})
    @DeleteMapping("/{isoKey}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Language ISO key") @PathVariable("isoKey") String isoKey
    ) throws NotFoundException {
        try {
            languageFacade.deleteLanguage(isoKey);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Bulk delete languages",
            responses = {@ApiResponse(responseCode = "204", description = "Successfully deleted languages")})
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Language', 'delete')")
    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteLanguages(
            @Parameter(description = "List of language ISO keys to delete") @RequestBody List<String> isoKeys
    ) throws DataIntegrityException {
        languageFacade.bulkDeleteLanguages(isoKeys);
        return ResponseEntity.noContent().build();
    }
}
