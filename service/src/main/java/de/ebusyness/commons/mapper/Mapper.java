package de.ebusyness.commons.mapper;

import de.ebusyness.commons.mapper.exception.DataMappingException;

import java.util.Collection;

public interface Mapper<SOURCE, TARGET, MAPPING_CONTEXT> {

    TARGET createTarget();

    TARGET convert(SOURCE source, MAPPING_CONTEXT context) throws DataMappingException;

    void convert(SOURCE source, TARGET target, MAPPING_CONTEXT context) throws DataMappingException;

    Collection<TARGET> convertAll(Collection<SOURCE> sourceItems, MAPPING_CONTEXT context) throws DataMappingException;
}
