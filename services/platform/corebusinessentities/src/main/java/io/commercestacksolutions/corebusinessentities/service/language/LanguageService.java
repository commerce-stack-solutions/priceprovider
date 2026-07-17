package io.commercestacksolutions.corebusinessentities.service.language;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.EntityService;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface LanguageService extends EntityService<LanguageEntity> {
    List<LanguageEntity> getAllLanguages();
    List<LanguageEntity> getActiveLanguages();
    List<LanguageEntity> getMandatoryLanguages();
    Optional<LanguageEntity> getLanguageByIsoKey(String isoKey);
    LanguageEntity updateLanguage(LanguageEntity updatedLanguage) throws EntityValidationException;
    void deleteLanguage(String isoKey);
    LanguageEntity findByIsoKey(String isoKey);
    Page<LanguageEntity> getLanguages(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    LanguageEntity getLanguage(String isoKey);
}
