package de.ebusyness.priceproviderservice.facade.channel;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.PatchMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.mapper.validation.PatchValidator;
import de.ebusyness.commons.mapper.validation.rules.ImmutableFieldsRule;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.*;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.commons.dataaccess.meta.EntityMetaInfoRegistry;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.facade.channel.mapper.ChannelEntityMapper;
import de.ebusyness.priceproviderservice.facade.channel.mapper.ChannelRestEntityMapper;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelListRestEntity;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelRestEntity;
import de.ebusyness.priceproviderservice.service.channel.ChannelService;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChannelFacadeImpl implements ChannelFacade {

    private static final Logger logger = LoggerFactory.getLogger(ChannelFacadeImpl.class);

    private final ChannelService channelService;
    private final ChannelRestEntityMapper channelRestEntityMapper;
    private final PatchMapper<ChannelRestEntity> channelRestEntityPatchMapper;
    private final ChannelEntityMapper channelEntityMapper;
    private final PatchValidator patchValidator;
    private final EntityMetaInfoRegistry entityMetaInfoRegistry;

    @Autowired
    public ChannelFacadeImpl(ChannelService channelService,
                             ChannelRestEntityMapper channelRestEntityMapper,
                             PatchMapper<ChannelRestEntity> channelRestEntityPatchMapper,
                             ChannelEntityMapper channelEntityMapper,
                              EntityMetaInfoRegistry entityMetaInfoRegistry) {
        this.channelService = channelService;
        this.channelRestEntityMapper = channelRestEntityMapper;
        this.channelRestEntityPatchMapper = channelRestEntityPatchMapper;
        this.channelEntityMapper = channelEntityMapper;

        // Initialize patch validator with immutable field rule for id
        this.patchValidator = new PatchValidator(List.of(
                new ImmutableFieldsRule(Set.of("id"))
        ));
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
    }

    @Transactional
    @Override
    public ChannelListRestEntity getChannels(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> includes, String query) throws DataMappingException, InvalidParameterException, QueryParseException {
        Page<ChannelEntity> channelsPage = channelService.getChannels(page, pageSize, sortBy, sortDirection, query);
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(includes);
        PagingInfo pagingInfo = new PagingInfo(channelsPage.getNumber(), channelsPage.getSize(), channelsPage.getTotalElements(), channelsPage.getTotalPages());
        SortingInfo sortingInfo = sortBy != null && !sortBy.isEmpty() ? new SortingInfo(sortBy, sortDirection != null ? sortDirection : "asc") : null;
        Collection<ChannelRestEntity> channelRestEntities = channelRestEntityMapper.convertAll(channelsPage.getContent(), context);
        ChannelListRestEntity result = new ChannelListRestEntity(pagingInfo, sortingInfo, channelRestEntities);

        // Add metadata if requested
        if (includes != null && includes.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(ChannelEntity.class));
        }

        return result;
    }

    @Transactional
    @Override
    public ChannelRestEntity getChannel(String id, Set<String> expand) throws NotFoundException, DataMappingException {
        ChannelEntity channel = channelService.getChannel(id);
        if (channel == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_CHANNEL_NOT_FOUND, params);
        }
        RestResponseMappingContext context = new RestResponseMappingContext();
        context.addExpandPaths(expand);

        ChannelRestEntity result = channelRestEntityMapper.convert(channel, context);

        // Add metadata if requested
        if (expand != null && expand.contains("$meta")) {
            result.setMeta(entityMetaInfoRegistry.getMetaInfo(ChannelEntity.class));
        }

        return result;
    }

    @Override
    public MetaInfo getMeta() {
        return entityMetaInfoRegistry.getMetaInfo(ChannelEntity.class);
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public ChannelRestEntity patch(String id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException {
        // Validate that patch doesn't try to modify the id field
        List<Message> patchValidationErrors = patchValidator.validate(patch, id);
        if (!patchValidationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse(patchValidationErrors);
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_PATCH_OPERATION, errorResponse);
        }

        ChannelRestEntity channel = getChannel(id, Collections.emptySet());
        if (channel == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_CHANNEL_NOT_FOUND, params);
        }
        channel = channelRestEntityPatchMapper.applyPatch(patch, channel);

        // Fetch existing entity to preserve timestamps and update in place
        ChannelEntity existingChannel = channelService.getChannel(id);
        if (existingChannel == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_CHANNEL_NOT_FOUND, params);
        }
        channelEntityMapper.convert(channel, existingChannel, new RestRequestMappingContext<>(id));
        ChannelEntity saved = channelService.save(existingChannel);
        return channelRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public ChannelRestEntity createOrRecreate(String id, ChannelRestEntity channelRestEntity) throws DataMappingException, EntityValidationException {
        ChannelEntity channel = channelService.getChannel(id);
        if (channel != null) {
            // Update existing channel
            channelEntityMapper.convert(channelRestEntity, channel, new RestRequestMappingContext<>(id));
            ChannelEntity saved = channelService.save(channel);
            return channelRestEntityMapper.convert(saved, new RestResponseMappingContext());
        } else {
            // Create new channel with id from path
            ChannelEntity newChannel = channelEntityMapper.convert(channelRestEntity, new RestRequestMappingContext<>(id));
            ChannelEntity saved = channelService.save(newChannel);
            return channelRestEntityMapper.convert(saved, new RestResponseMappingContext());
        }
    }

    @Transactional(rollbackFor = {EntityValidationException.class, DataMappingException.class})
    @Override
    public ChannelRestEntity create(ChannelRestEntity channelRestEntity) throws DataMappingException, EntityValidationException {
        if (channelRestEntity.getId() == null || channelRestEntity.getId().isEmpty()) {
            ChannelRestEntity restEntity = new ChannelRestEntity();
            restEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_VALIDATION_ID_REQUIRED, "field", "id", List.of("id")));
            return restEntity;
        }

        // Check if channel already exists
        ChannelEntity existingChannel = channelService.getChannel(channelRestEntity.getId());
        if (existingChannel != null) {
            ChannelRestEntity restEntity = new ChannelRestEntity();
            Map<String, String> params = new HashMap<>();
            params.put("id", channelRestEntity.getId());
            restEntity.addMessage(MessageBuilder.create(Message.MessageType.ERROR, MessageKeys.ERROR_CHANNEL_ALREADY_EXISTS, params, List.of("id")));
            return restEntity;
        }

        ChannelEntity newChannel = channelEntityMapper.convert(channelRestEntity, new RestRequestMappingContext<>(channelRestEntity.getId()));
        ChannelEntity saved = channelService.save(newChannel);
        return channelRestEntityMapper.convert(saved, new RestResponseMappingContext());
    }

    @Transactional
    @Override
    public void delete(String id) throws NotFoundException {
        ChannelEntity channel = channelService.getChannel(id);
        if (channel == null) {
            Map<String, String> params = new HashMap<>();
            params.put("id", id);
            throw new NotFoundException(MessageKeys.ERROR_CHANNEL_NOT_FOUND, params);
        }
        channelService.deleteChannel(id);
    }

    @Override
    public void bulkDeleteChannels(List<String> ids) throws DataIntegrityException {
        List<String> failedDeletes = new java.util.ArrayList<>();

        for (String id : ids) {
            ChannelEntity channel = channelService.getChannel(id);
            if (channel != null) {
                try {
                    channelService.deleteChannel(id);
                } catch (DataIntegrityViolationException ex) {
                    failedDeletes.add(id);
                } catch (ConstraintViolationException ex) {
                    failedDeletes.add(id);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    while (cause != null) {
                        if (cause instanceof DataIntegrityViolationException || cause instanceof ConstraintViolationException) {
                            failedDeletes.add(id);
                            break;
                        }
                        cause = cause.getCause();
                    }
                    if (cause == null) {
                        throw ex;
                    }
                }
            }
        }

        if (!failedDeletes.isEmpty()) {
            Map<String, String> params = new HashMap<>();
            params.put("ids", String.join(", ", failedDeletes));
            throw new DataIntegrityException(MessageKeys.ERROR_DATA_INTEGRITY_REFERENCED, params);
        }
    }

    @Transactional
    @Override
    public ChannelListRestEntity createOrUpdateAllChannels(List<ChannelRestEntity> channelRestEntities) {
        if (channelRestEntities == null || channelRestEntities.isEmpty()) {
            ChannelListRestEntity result = new ChannelListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_EMPTY_REQUEST,
                    "entity", "channel"
            ));
            return result;
        }

        if (channelRestEntities.size() > 100) {
            ChannelListRestEntity result = new ChannelListRestEntity(null, null, List.of());
            result.addMessage(MessageBuilder.create(
                    Message.MessageType.ERROR,
                    MessageKeys.ERROR_VALIDATION_MAX_ITEMS_EXCEEDED,
                    "maxItems", "100",
                    "entity", "channels"
            ));
            return result;
        }

        List<ChannelRestEntity> results = new java.util.ArrayList<>();

        for (ChannelRestEntity restEntity : channelRestEntities) {
            try {
                if (restEntity.getId() == null || restEntity.getId().isEmpty()) {
                    ChannelRestEntity errorEntity = new ChannelRestEntity();
                    errorEntity.addMessage(MessageBuilder.create(
                            Message.MessageType.ERROR,
                            MessageKeys.ERROR_VALIDATION_MANDATORY_FIELD,
                            "field", "id",
                            List.of("id")
                    ));
                    results.add(errorEntity);
                    continue;
                }

                ChannelEntity existingChannel = channelService.getChannel(restEntity.getId());
                if (existingChannel != null) {
                    // Update existing
                    channelEntityMapper.convert(restEntity, existingChannel, new RestRequestMappingContext<>(restEntity.getId()));
                    ChannelEntity saved = channelService.save(existingChannel);
                    results.add(channelRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                } else {
                    // Create new
                    ChannelEntity newChannel = channelEntityMapper.convert(restEntity, new RestRequestMappingContext<>(restEntity.getId()));
                    ChannelEntity saved = channelService.save(newChannel);
                    results.add(channelRestEntityMapper.convert(saved, new RestResponseMappingContext()));
                }
            } catch (EntityValidationException e) {
                ChannelRestEntity errorEntity = new ChannelRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (DataMappingException e) {
                ChannelRestEntity errorEntity = new ChannelRestEntity();
                errorEntity.setId(restEntity.getId());
                for (Message message : e.getMessages()) {
                    errorEntity.addMessage(message);
                }
                results.add(errorEntity);
            } catch (Exception e) {
                logger.debug("Error processing channel with id {}: {}", restEntity.getId(), e.getMessage(), e);
                ChannelRestEntity errorEntity = new ChannelRestEntity();
                errorEntity.setId(restEntity.getId());
                errorEntity.addMessage(MessageBuilder.create(
                        Message.MessageType.ERROR,
                        MessageKeys.ERROR_PROCESSING,
                        "entity", "channel"
                ));
                results.add(errorEntity);
            }
        }

        return new ChannelListRestEntity(null, null, results);
    }
}
