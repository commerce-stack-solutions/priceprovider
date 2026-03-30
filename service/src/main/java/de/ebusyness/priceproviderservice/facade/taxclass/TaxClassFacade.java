package de.ebusyness.priceproviderservice.facade.taxclass;

import de.ebusyness.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.taxclass.restentity.TaxClassListRestEntity;
import de.ebusyness.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import java.util.List;
import java.util.Set;

public interface TaxClassFacade {
    TaxClassListRestEntity getTaxClasses(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> includes, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    TaxClassRestEntity getTaxClass(String taxClassId, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    TaxClassRestEntity patch(String taxClassId, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    TaxClassRestEntity createOrRecreate(String taxClassId, TaxClassRestEntity taxClassRestEntity) throws DataMappingException, EntityValidationException;
    TaxClassRestEntity create(TaxClassRestEntity taxClassRestEntity) throws DataMappingException, EntityValidationException;
    void delete(String taxClassId) throws NotFoundException;
    void bulkDeleteTaxClasses(List<String> taxClassIds) throws DataIntegrityException;
    TaxClassListRestEntity createOrUpdateAllTaxClasses(List<TaxClassRestEntity> taxClassRestEntities);
}
