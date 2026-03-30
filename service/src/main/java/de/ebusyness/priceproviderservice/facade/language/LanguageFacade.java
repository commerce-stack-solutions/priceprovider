package de.ebusyness.priceproviderservice.facade.language;

import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageListRestEntity;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageRestEntity;
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
