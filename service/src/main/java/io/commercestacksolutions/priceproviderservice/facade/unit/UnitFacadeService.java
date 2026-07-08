package io.commercestacksolutions.priceproviderservice.facade.unit;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitRestEntity;

import java.util.List;
import java.util.Set;

/**
 * Facade interface for Unit operations.
 * This interface defines the contract for unit facade operations,
 * following Interface Driven Design (IDD) principles.
 */
public interface UnitFacadeService {
    
    /**
     * Retrieves a paginated list of units with optional sorting, filtering and expansion.
     * 
     * @param page the page number (0-based)
     * @param pageSize the number of items per page
     * @param sortBy list of field names to sort by
     * @param sortDirection sort direction ("asc" or "desc")
     * @param expand set of paths to expand in the response
     * @param query optional query string for filtering (Lucene-like syntax)
     * @return list of unit REST entities with paging info
     * @throws DataMappingException if mapping fails
     */
    UnitListRestEntity getUnits(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    
    /**
     * Retrieves a single unit by its symbol.
     * 
     * @param symbol the unit symbol
     * @param expand set of paths to expand in the response
     * @return the unit REST entity
     * @throws NotFoundException if unit not found
     * @throws DataMappingException if mapping fails
     */
    UnitRestEntity getUnit(String symbol, Set<String> expand) throws NotFoundException, DataMappingException;
    
    /**
     * Returns the MetaInfo for Unit entities (identity fields, mandatory fields, enum values).
     *
     * @return MetaInfo for Unit
     */
    MetaInfo getMeta();
    
    /**
     * Applies a JSON Patch to an existing unit.
     * 
     * @param symbol the unit symbol
     * @param patch the JSON Patch document
     * @return the updated unit REST entity
     * @throws NotFoundException if unit not found
     * @throws DataMappingException if mapping or patch validation fails
     */
    UnitRestEntity patch(String symbol, JsonNode patch) throws NotFoundException, DataMappingException, EntityValidationException;
    
    /**
     * Creates a new unit or replaces an existing one (PUT operation).
     * 
     * @param symbol the unit symbol
     * @param unitRestEntity the unit data
     * @return the created or updated unit REST entity
     * @throws DataMappingException if mapping fails
     */
    UnitRestEntity createOrRecreate(String symbol, UnitRestEntity unitRestEntity) throws DataMappingException, EntityValidationException;
    
    /**
     * Creates a new unit (POST operation).
     * 
     * @param unitRestEntity the unit data
     * @return the created unit REST entity
     * @throws DataMappingException if mapping fails
     * @throws EntityAlreadyExistsException if unit already exists
     * @throws InvalidParameterException if required parameters are missing
     */
    UnitRestEntity create(UnitRestEntity unitRestEntity) throws DataMappingException, EntityAlreadyExistsException, InvalidParameterException, EntityValidationException;
    
    /**
     * Deletes a unit by its symbol.
     * 
     * @param symbol the unit symbol
     */
    void delete(String symbol);
    
    /**
     * Deletes multiple units in a single transaction.
     * 
     * @param symbols list of unit symbols to delete
     * @throws DataIntegrityException if deletion fails due to constraints
     */
    void bulkDeleteUnits(List<String> symbols) throws DataIntegrityException;
    
    /**
     * Creates or updates multiple units in a single transaction.
     * 
     * @param unitRestEntities list of unit entities to create or update
     * @return list of results with created/updated units
     * @throws DataMappingException if mapping fails
     */
    UnitListRestEntity createOrUpdateAllUnits(List<UnitRestEntity> unitRestEntities);
}
