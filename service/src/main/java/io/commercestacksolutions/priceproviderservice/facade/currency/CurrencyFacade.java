package io.commercestacksolutions.priceproviderservice.facade.currency;

import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
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
