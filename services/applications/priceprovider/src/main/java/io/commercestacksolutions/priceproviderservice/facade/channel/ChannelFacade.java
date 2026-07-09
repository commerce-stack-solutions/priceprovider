package io.commercestacksolutions.priceproviderservice.facade.channel;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.channel.restentity.ChannelListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.channel.restentity.ChannelRestEntity;

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
