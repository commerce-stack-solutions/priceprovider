package io.commercestacksolutions.priceproviderservice.service.language;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Language entity operations.
 * This interface defines the contract for language management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface LanguageService extends EntityService<LanguageEntity> {
    
    /**
     * Retrieves all languages.
     * 
     * @return list of all language entities
     */
    List<LanguageEntity> getAllLanguages();
    
    /**
     * Retrieves all active languages.
     * 
     * @return list of active language entities
     */
    List<LanguageEntity> getActiveLanguages();
    
    /**
     * Retrieves all mandatory languages.
     * 
     * @return list of mandatory language entities
     */
    List<LanguageEntity> getMandatoryLanguages();
    
    /**
     * Retrieves a language by its ISO key.
     * 
     * @param isoKey the ISO language key
     * @return optional containing the language entity if found
     */
    Optional<LanguageEntity> getLanguageByIsoKey(String isoKey);
    
    /**
     * Updates a language entity.
     * 
     * @param updatedLanguage the language entity to update
     * @return the updated language entity
     */
    LanguageEntity updateLanguage(LanguageEntity updatedLanguage) throws EntityValidationException;
    
    /**
     * Deletes a language by its ISO key.
     * 
     * @param isoKey the ISO language key
     */
    void deleteLanguage(String isoKey);
    
    /**
     * Finds a language by its ISO key.
     * 
     * @param isoKey the ISO language key
     * @return the language entity, or null if not found
     */
    LanguageEntity findByIsoKey(String isoKey);
    
    /**
     * Retrieves a paginated list of languages with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of language entities
     */
    Page<LanguageEntity> getLanguages(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Retrieves a language by its ISO key.
     * 
     * @param isoKey the ISO language key
     * @return the language entity, or null if not found
     */
    LanguageEntity getLanguage(String isoKey);
}
