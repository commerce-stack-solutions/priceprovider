package io.commercestacksolutions.priceproviderservice.facade.taxclass;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
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
