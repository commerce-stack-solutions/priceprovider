package de.ebusyness.priceproviderservice.service.country;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for Country entity operations.
 * This interface defines the contract for country management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface CountryService extends EntityService<CountryEntity> {

    /**
     * Deletes a country by its ISO key.
     *
     * @param isoKey the ISO Alpha-2 country key
     */
    void deleteCountry(String isoKey);

    /**
     * Retrieves a paginated list of countries with optional sorting and filtering.
     *
     * @param page          the page number (0-based)
     * @param pageSize      the number of items per page
     * @param sortBy        list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query         optional query string for filtering (Lucene-like syntax)
     * @return page of country entities
     */
    Page<CountryEntity> getCountries(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;

    /**
     * Retrieves a country by its ISO key.
     *
     * @param isoKey the ISO Alpha-2 country key
     * @return the country entity, or null if not found
     */
    CountryEntity getCountry(String isoKey);
}
