package de.ebusyness.priceproviderservice.facade.pricerow;

import de.ebusyness.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.pricerow.restentity.PriceRowListRestEntity;
import de.ebusyness.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
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
    void bulkDeletePriceRows(List<Long> ids) throws de.ebusyness.commons.exception.DataIntegrityException;
    PriceRowListRestEntity createOrUpdateAllPriceRows(List<PriceRowRestEntity> priceRowRestEntities);
}
