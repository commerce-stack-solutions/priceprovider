package io.commercestacksolutions.priceproviderservice.service.currency;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.commons.service.entity.EntityService;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for Currency entity operations.
 * This interface defines the contract for currency management operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface CurrencyService extends EntityService<CurrencyEntity> {
    
    /**
     * Deletes a currency by its key.
     * 
     * @param currencyKey the currency key
     */
    void deleteCurrency(String currencyKey);
    
    /**
     * Retrieves a paginated list of currencies with optional sorting and filtering.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return page of currency entities
     */
    Page<CurrencyEntity> getCurrencies(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException;
    
    /**
     * Retrieves a currency by its key.
     * 
     * @param currencyKey the currency key
     * @return the currency entity, or null if not found
     */
    CurrencyEntity getCurrency(String currencyKey);
}
