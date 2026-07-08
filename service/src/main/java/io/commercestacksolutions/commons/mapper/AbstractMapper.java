package io.commercestacksolutions.commons.mapper;

import io.commercestacksolutions.commons.mapper.exception.DataMappingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractMapper<SOURCE, TARGET, MAPPING_CONTEXT> implements Mapper<SOURCE, TARGET, MAPPING_CONTEXT> {

    public abstract TARGET createTarget();

    @Override
    public TARGET convert(SOURCE source, MAPPING_CONTEXT context) throws DataMappingException {
        TARGET target = createTarget();
        convert(source, target, context);
        return target;
    }

    public abstract void convert(SOURCE source, TARGET target, MAPPING_CONTEXT context) throws DataMappingException;

    @Override
    public Collection<TARGET> convertAll(Collection<SOURCE> sources, MAPPING_CONTEXT context) throws DataMappingException {
        List<TARGET> list = new ArrayList<>();
        if (sources == null) {
            return list;
        }
        for (SOURCE sourceItem : sources) {
            TARGET target = convert(sourceItem, context);
            list.add(target);
        }
        return list;
    }
}