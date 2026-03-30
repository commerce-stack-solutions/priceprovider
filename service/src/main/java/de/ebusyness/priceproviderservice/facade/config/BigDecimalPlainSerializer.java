package de.ebusyness.priceproviderservice.facade.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.math.BigDecimal;

public class BigDecimalPlainSerializer extends StdSerializer<BigDecimal> {

    public BigDecimalPlainSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            // write as plain string so no scientific notation is used
            gen.writeNumber(value.toPlainString());
        }
    }
}

