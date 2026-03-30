package de.ebusyness.priceproviderservice.facade.currency;

import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.currency.restentity.CurrencyListRestEntity;
import de.ebusyness.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import java.util.List;
import java.util.Set;

public interface CurrencyFacade {
    CurrencyListRestEntity getCurrencies(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> expand, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    CurrencyRestEntity getCurrency(String currencyKey, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    CurrencyRestEntity patch(String currencyKey, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    CurrencyRestEntity createOrRecreate(String currencyKey, CurrencyRestEntity currencyRestEntity) throws DataMappingException, EntityValidationException;
    CurrencyRestEntity create(CurrencyRestEntity currencyRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String currencyKey) throws NotFoundException;
    void bulkDeleteCurrencies(List<String> currencyKeys) throws DataIntegrityException;
    CurrencyListRestEntity createOrUpdateAllCurrencies(List<CurrencyRestEntity> currencyRestEntities);
}
