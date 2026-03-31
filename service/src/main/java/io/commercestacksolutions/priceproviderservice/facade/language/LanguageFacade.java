package io.commercestacksolutions.priceproviderservice.facade.language;

import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.language.restentity.LanguageListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.language.restentity.LanguageRestEntity;
import java.util.List;
import java.util.Set;

public interface LanguageFacade {
    LanguageListRestEntity getLanguages(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    LanguageRestEntity getLanguage(String isoKey, Set<String> includes) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    LanguageRestEntity patch(String isoKey, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    LanguageRestEntity createOrReCreate(String isoKey, LanguageRestEntity languageRestEntity) throws DataMappingException, EntityValidationException;
    LanguageRestEntity create(LanguageRestEntity languageRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    LanguageListRestEntity createOrUpdateAllLanguages(List<LanguageRestEntity> languageRestEntities);
    void deleteLanguage(String isoKey) throws NotFoundException;
    void bulkDeleteLanguages(List<String> isoKeys) throws DataIntegrityException;
}
