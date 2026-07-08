package io.commercestacksolutions.priceproviderservice.facade.country;

import com.fasterxml.jackson.databind.JsonNode;
import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.facade.country.restentity.CountryListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.country.restentity.CountryRestEntity;

import java.util.List;
import java.util.Set;

public interface CountryFacade {
    CountryListRestEntity getCountries(int page, int pageSize, List<String> sortBy, String sortDirection, Set<String> includes, String query) throws DataMappingException, InvalidParameterException, QueryParseException;
    CountryRestEntity getCountry(String isoKey, Set<String> expand) throws NotFoundException, DataMappingException;
    MetaInfo getMeta();
    CountryRestEntity patch(String isoKey, JsonNode patch) throws DataMappingException, NotFoundException, EntityValidationException;
    CountryRestEntity createOrRecreate(String isoKey, CountryRestEntity countryRestEntity) throws DataMappingException, EntityValidationException;
    CountryRestEntity create(CountryRestEntity countryRestEntity) throws DataMappingException, EntityValidationException, EntityAlreadyExistsException;
    void delete(String isoKey) throws NotFoundException;
    void bulkDeleteCountries(List<String> isoKeys) throws DataIntegrityException;
    CountryListRestEntity createOrUpdateAllCountries(List<CountryRestEntity> countryRestEntities);
}
