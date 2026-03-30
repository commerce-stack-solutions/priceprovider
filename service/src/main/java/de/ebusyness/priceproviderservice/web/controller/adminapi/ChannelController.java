package de.ebusyness.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.channel.ChannelFacade;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelListRestEntity;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelRestEntity;
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
@RequestMapping("/admin/api/channels")
@Tag(name = "Channels", description = "Channel management API - handles sales channels and their country assignments")
public class ChannelController {

    private final ChannelFacade channelFacade;

    @Autowired
    public ChannelController(ChannelFacade channelFacade) {
        this.channelFacade = channelFacade;
    }
    @Operation(
            summary = "Get list of channels",
            description = "Retrieves a paginated and sortable list with optional filtering of channels. Supports sorting by: id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved channels",
                            content = @Content(schema = @Schema(implementation = ChannelListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:read')") 

    @GetMapping
    public ChannelListRestEntity getChannels(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(value = "page-size", defaultValue = "10") int pageSize,

            @Parameter(description = "Field(s) to sort by. Valid values: id. Can be specified multiple times for multi-field sorting",
                    example = "id")
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
        return channelFacade.getChannels(page, pageSize, sortBy, sortDirection, expand, query);
    }

    @Operation(
            summary = "Get channel by ID",
            description = "Retrieves a single channel by its unique identifier",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved channel",
                            content = @Content(schema = @Schema(implementation = ChannelRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:read')") 

    @GetMapping("/{id}")
    public ChannelRestEntity getChannel(
            @Parameter(description = "Channel ID (e.g., 'WEB-DE', 'APP-US')", example = "WEB-DE")
            @PathVariable("id") String id,

            @Parameter(description = "Optional related data to include in response")
            @RequestParam(value = "$expand", required = false) Set<String> expand,

            @Parameter(description = "Query string for advanced filtering using Lucene-like syntax",
                    example = "field:value")
            @RequestParam(value = "q", required = false) String query
    ) throws de.ebusyness.commons.exception.NotFoundException, DataMappingException {
        return channelFacade.getChannel(id, expand);
    }

    @Operation(
            summary = "Get meta information for channels",
            description = "Returns identity fields, mandatory fields and enum values for the Channel entity",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved meta information",
                            content = @Content(schema = @Schema(implementation = MetaInfo.class)))
            }
    )
    @PreAuthorize("isAuthenticated()")

    @GetMapping("/$meta")
    public MetaInfo getMeta() {
        return channelFacade.getMeta();
    }

    @Operation(
            summary = "Partially update channel",
            description = "Applies JSON Patch operations to partially update a channel. Supports operations: add, remove, replace.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully patched channel",
                            content = @Content(schema = @Schema(implementation = ChannelRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:write')") 

    @PatchMapping("/{id}")
    public ChannelRestEntity patch(
            @Parameter(description = "Channel ID (e.g., 'WEB-DE', 'APP-US')", example = "WEB-DE")
            @PathVariable("id") String id,

            @Parameter(description = "JSON Patch operations")
            @RequestBody JsonNode patch
    ) throws DataMappingException, de.ebusyness.commons.exception.NotFoundException, EntityValidationException {
        return channelFacade.patch(id, patch);
    }

    @Operation(
            summary = "Create or update channel",
            description = "Creates a new channel or fully replaces an existing channel with the provided data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created or updated channel",
                            content = @Content(schema = @Schema(implementation = ChannelRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:write')") 

    @PutMapping("/{id}")
    public ChannelRestEntity createOrRecreate(
            @Parameter(description = "Channel ID (e.g., 'WEB-DE', 'APP-US')", example = "WEB-DE")
            @PathVariable("id") String id,

            @Parameter(description = "Channel data")
            @RequestBody ChannelRestEntity channelRestEntity
    ) throws DataMappingException, EntityValidationException {
        return channelFacade.createOrRecreate(id, channelRestEntity);
    }

    @Operation(
            summary = "Create new channel",
            description = "Creates a new channel with the ID included in the request body.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created channel",
                            content = @Content(schema = @Schema(implementation = ChannelRestEntity.class))),
                    @ApiResponse(responseCode = "409", description = "Channel with the same ID already exists")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:write')") 

    @PostMapping("/create")
    public ResponseEntity<ChannelRestEntity> create(
            @Parameter(description = "Channel data including channel ID")
            @RequestBody ChannelRestEntity channelRestEntity
    ) throws DataMappingException, EntityValidationException {
        ChannelRestEntity result = channelFacade.create(channelRestEntity);

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
            summary = "Delete channel",
            description = "Deletes a channel. Will fail if there are price rows referencing this channel.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted channel"),
                    @ApiResponse(responseCode = "404", description = "Channel not found"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete channel - it is referenced by price rows")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:delete')") 

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Channel ID (e.g., 'WEB-DE', 'APP-US')", example = "WEB-DE")
            @PathVariable("id") String id
    ) {
        try {
            channelFacade.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Operation(
            summary = "Bulk delete channels",
            description = "Deletes multiple channels by their IDs. Will fail if any channel is referenced by price rows.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted channels"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete one or more channels - they are referenced by price rows")
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:delete')") 

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteChannels(
            @Parameter(description = "List of channel IDs to delete")
            @RequestBody List<String> ids
    ) throws de.ebusyness.commons.exception.DataIntegrityException {
        channelFacade.bulkDeleteChannels(ids);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update multiple channels",
            description = "Creates or updates multiple channels in a single request. Returns partial results with error messages for items that fail.",
            responses = {
                    @ApiResponse(responseCode = "207", description = "Multi-Status - Successfully processed channels (check individual items for errors)",
                            content = @Content(schema = @Schema(implementation = ChannelListRestEntity.class)))
            }
    )
    @PreAuthorize("hasAuthority('priceprovider.admin:Channel:write')") 

    @PostMapping("/bulk-create-or-update")
    public ResponseEntity<ChannelListRestEntity> createOrUpdateAllChannels(
            @Parameter(description = "List of channel data to create or update")
            @RequestBody List<ChannelRestEntity> channelRestEntities
    ) {
        ChannelListRestEntity result = channelFacade.createOrUpdateAllChannels(channelRestEntities);
        return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
    }
}




