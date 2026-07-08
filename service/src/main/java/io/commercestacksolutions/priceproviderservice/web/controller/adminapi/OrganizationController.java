package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.organization.OrganizationFacade;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
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
@RequestMapping("/admin/api/organizations")
@Tag(name = "Organizations", description = "Organization management API - handles organizational entities with types like companies, business units, etc.")
public class OrganizationController {

    private final OrganizationFacade organizationFacade;

    @Autowired
    public OrganizationController(OrganizationFacade organizationFacade) {
        this.organizationFacade = organizationFacade;
    }

    @Operation(
            summary = "Get list of organizations",
            description = "Retrieves a paginated and sortable list with optional filtering of organizations. Supports sorting by: id, name, organizationType",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved organizations",
                            content = @Content(schema = @Schema(implementation = OrganizationListRestEntity.class)))
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'read')") 

    @GetMapping
    public OrganizationListRestEntity getOrganizations(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: id, name, organizationType. Can be specified multiple times for multi-field sorting",
                    example = "name")
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
        return organizationFacade.getOrganizations(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get organization by id",
            description = "Retrieves a single organization by its unique identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved organization",
                            content = @Content(schema = @Schema(implementation = OrganizationRestEntity.class))),
                    @ApiResponse(responseCode = "404", description = "Organization not found")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'read')") 

    @GetMapping("/{id}")
    public OrganizationRestEntity getOrganization(
            @Parameter(description = "Organization ID", example = "ORG-001")
            @PathVariable("id") String id,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax. " +
                                    "Examples: 'field:value', 'field1:value1 AND field2:value2'",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws NotFoundException, DataMappingException {
        return organizationFacade.getOrganization(id, expand);
    }

    @Operation(
            summary = "Get meta information for organizations",
            description = "Returns identity fields, mandatory fields and enum values for the Organization entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return organizationFacade.getMeta();
    }

    @Operation(
            summary = "Partially update organization",
            description = "Applies JSON Patch operations to partially update an organization. Supports operations: add, remove, replace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched organization",
                            content = @Content(schema = @Schema(implementation = OrganizationRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Organization not found")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'write')") 

    @PatchMapping("/{id}")
    public OrganizationRestEntity patch(
            @Parameter(description = "Organization ID", example = "ORG-001")
            @PathVariable("id") String id,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, NotFoundException, EntityValidationException {
        return organizationFacade.patch(id, patch);
    }

    @Operation(
            summary = "Create or update organization",
            description = "Creates a new organization or fully replaces an existing organization with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated organization",
                            content = @Content(schema = @Schema(implementation = OrganizationRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'write')") 

    @PutMapping("/{id}")
    public OrganizationRestEntity createOrRecreate(
            @Parameter(description = "Organization ID", example = "ORG-001")
            @PathVariable("id") String id,

            @Parameter(description = "Organization data")
            @RequestBody OrganizationRestEntity organizationRestEntity
    ) throws DataMappingException, EntityValidationException {
        return organizationFacade.createOrRecreate(id, organizationRestEntity);
    }

    @Operation(
            summary = "Create new organization",
            description = "Creates a new organization with a provided ID. The ID must be included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created organization",
                            content = @Content(schema = @Schema(implementation = OrganizationRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Organization with the same ID already exists")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'write')") 

    @PostMapping("/create")
    public OrganizationRestEntity create(
            @Parameter(description = "Organization data including ID")
            @RequestBody OrganizationRestEntity organizationRestEntity
    ) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException {
        return organizationFacade.create(organizationRestEntity);
    }

    @Operation(
            summary = "Delete organization",
            description = "Deletes an organization by its ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted organization"),
                    @ApiResponse(responseCode = "404", description = "Organization not found"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete organization - it may be referenced by other entities")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'delete')") 

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Organization ID", example = "ORG-001")
            @PathVariable("id") String id
    ) {
        try {
            organizationFacade.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Bulk delete organizations",
            description = "Deletes multiple organizations by their IDs. Maximum 100 organizations per request.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted organizations"),
                    @ApiResponse(responseCode = "400", description = "Cannot delete - referential integrity constraint violation")
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteOrganizations(
            @Parameter(description = "List of organization IDs to delete")
            @RequestBody List<String> ids
    ) throws DataIntegrityException {
        organizationFacade.bulkDeleteOrganizations(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple organizations",
            description = "Creates new organizations or updates existing ones in a single batch operation. Maximum 100 organizations per request. " +
                    "Entities with existing IDs will be updated. Entities with new IDs will be created.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed organizations",
                            content = @Content(schema = @Schema(implementation = OrganizationListRestEntity.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error or request exceeds maximum batch size",
                            content = @Content(schema = @Schema(implementation = OrganizationListRestEntity.class)))
            }
    )
    @PreAuthorize("@permissionSecurityService.hasPermissionForAction('Organization', 'write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<OrganizationListRestEntity> createOrUpdateAllOrganizations(
            @Parameter(description = "List of organization data (max 100 items). ID is required for all entities.")
            @RequestBody List<OrganizationRestEntity> organizationRestEntities
    ) throws DataMappingException, InvalidParameterException {
        OrganizationListRestEntity result = organizationFacade.createOrUpdateAllOrganizations(organizationRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}



