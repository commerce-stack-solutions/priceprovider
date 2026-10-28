package io.commercestacksolutions.priceproviderservice.config.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;

/**
 * Non-deprecated HttpMessageConverter for Jackson 2.x JsonNode types.
 * Handles deserialization of request bodies typed as com.fasterxml.jackson.databind.JsonNode
 * (used by zjsonpatch for JSON Patch operations) without relying on the deprecated
 * MappingJackson2HttpMessageConverter.
 */
public class Jackson2JsonNodeConverter extends AbstractHttpMessageConverter<JsonNode> {

    private final ObjectMapper objectMapper;

    public Jackson2JsonNodeConverter(ObjectMapper objectMapper) {
        super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return JsonNode.class.isAssignableFrom(clazz);
    }

    @Override
    protected JsonNode readInternal(Class<? extends JsonNode> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return objectMapper.readTree(inputMessage.getBody());
    }

    @Override
    protected void writeInternal(JsonNode jsonNode, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        objectMapper.writeValue(outputMessage.getBody(), jsonNode);
    }
}
