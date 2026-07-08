package io.commercestacksolutions.commons.mapper;

public class RestRequestMappingContext<ID_TYPE> {
    private ID_TYPE id;

    public RestRequestMappingContext(ID_TYPE id) {
        this.id = id;
    }

    public ID_TYPE getId() {
        return id;
    }

    public void setId(ID_TYPE id) {
        this.id = id;
    }
}