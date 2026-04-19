package io.commercestacksolutions.priceproviderservice.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionHandlerAdviceTest {

    private final ExceptionHandlerAdvice advice = new ExceptionHandlerAdvice();

    @Test
    public void testHandleNoResourceFoundExceptionReturns404() throws Exception {
        NoResourceFoundException ex = new NoResourceFoundException(
                HttpMethod.GET, "public/api/dach-sales-channel/DE/pricerows/SALES_PRICE/of/DEMO-PRODUCT-001/candidates");

        ResponseEntity<Void> response = advice.handleNoResourceFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
