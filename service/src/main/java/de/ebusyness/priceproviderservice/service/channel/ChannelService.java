package de.ebusyness.priceproviderservice.service.channel;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for Channel entity operations.
 * This interface defines the contract for channel management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface ChannelService extends EntityService<ChannelEntity> {

    /**
     * Deletes a channel by its ID.
     *
     * @param id the channel ID
     */
    void deleteChannel(String id);

    /**
     * Retrieves a paginated list of channels with optional sorting and filtering.
     *
     * @param page          the page number (0-based)
     * @param pageSize      the number of items per page
     * @param sortBy        list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query         optional query string for filtering (Lucene-like syntax)
     * @return page of channel entities
     */
    Page<ChannelEntity> getChannels(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;

    /**
     * Retrieves a channel by its ID.
     *
     * @param id the channel ID
     * @return the channel entity, or null if not found
     */
    ChannelEntity getChannel(String id);
}
