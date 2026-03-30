package de.ebusyness.priceproviderservice.facade.channel;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelListRestEntity;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelRestEntity;

import java.util.List;
import java.util.Set;

public interface ChannelFacade {
    ChannelListRestEntity getChannels(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> includes, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    ChannelRestEntity getChannel(String id, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    ChannelRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    ChannelRestEntity createOrRecreate(String id, ChannelRestEntity channelRestEntity) throws DataMappingException, EntityValidationException;
    ChannelRestEntity create(ChannelRestEntity channelRestEntity) throws DataMappingException, EntityValidationException;
    void delete(String id) throws NotFoundException;
    void bulkDeleteChannels(List<String> ids) throws DataIntegrityException;
    ChannelListRestEntity createOrUpdateAllChannels(List<ChannelRestEntity> channelRestEntities);
}
