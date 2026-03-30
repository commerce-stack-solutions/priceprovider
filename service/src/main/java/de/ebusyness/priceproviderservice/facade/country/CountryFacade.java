package de.ebusyness.priceproviderservice.facade.country;

import com.fasterxml.jackson.databind.JsonNode;
import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.exception.NotFoundException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.MetaInfo;
import de.ebusyness.priceproviderservice.facade.country.restentity.CountryListRestEntity;
import de.ebusyness.priceproviderservice.facade.country.restentity.CountryRestEntity;

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
