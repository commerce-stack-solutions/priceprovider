package io.commercestacksolutions.priceproviderservice.facade.pricerow;

import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import java.util.List;
import java.util.Set;

public interface PriceRowFacade {
    PriceRowListRestEntity getPriceRows(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    PriceRowRestEntity getPriceRow(Long id, Set<String> expand) throws DataMappingException, NotFoundException;
    MetaInfo getMeta();
    PriceRowRestEntity createOrRecreate(Long id, PriceRowRestEntity priceRowRestEntity) throws DataMappingException, EntityValidationException;
    PriceRowRestEntity patch(Long id, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    PriceRowRestEntity create(PriceRowRestEntity priceRowRestEntity) throws DataMappingException, InvalidParameterException;
    void delete(Long id) throws NotFoundException;
    void bulkDeletePriceRows(List<Long> ids) throws io.commercestacksolutions.commons.exception.DataIntegrityException;
    PriceRowListRestEntity createOrUpdateAllPriceRows(List<PriceRowRestEntity> priceRowRestEntities);
}
