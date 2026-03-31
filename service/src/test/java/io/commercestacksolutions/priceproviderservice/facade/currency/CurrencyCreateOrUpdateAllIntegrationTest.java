package io.commercestacksolutions.priceproviderservice.facade.currency;

import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyListRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for createOrUpdateAllCurrencies functionality.
 * Tests error handling and partial success scenarios.
 */
@SpringBootTest
public class CurrencyCreateOrUpdateAllIntegrationTest {

    @Autowired
    private CurrencyFacade currencyFacade;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @BeforeEach
    public void setup() {
        // Clean up test currencies
        currencyRepository.findAll().stream()
            .filter(c -> c.getCurrencyKey().startsWith("TEST-"))
            .forEach(c -> {
                try {
                    currencyRepository.delete(c);
                } catch (Exception e) {
                    // Ignore
                }
            });
    }

    @Test
    public void testCreateOrUpdateAll_AllValid_ShouldSucceed() {
        List<CurrencyRestEntity> currencies = new ArrayList<>();
        
        // Create first currency
        CurrencyRestEntity usd = new CurrencyRestEntity();
        usd.setCurrencyKey("TEST-USD");
        usd.setSymbol("$");
        Map<String, String> usdName = new HashMap<>();
        usdName.put("en", "US Dollar");
        usdName.put("de", "US-Dollar");
        usd.setName(usdName);
        currencies.add(usd);
        
        // Create second currency
        CurrencyRestEntity eur = new CurrencyRestEntity();
        eur.setCurrencyKey("TEST-EUR");
        eur.setSymbol("€");
        Map<String, String> eurName = new HashMap<>();
        eurName.put("en", "Euro");
        eurName.put("de", "Euro");
        eur.setName(eurName);
        currencies.add(eur);

        // Execute
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencies);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(2, result.getItems().size());
        
        // Verify no errors
        for (CurrencyRestEntity entity : result.getItems()) {
            assertTrue(entity.getMessages() == null || entity.getMessages().isEmpty(), 
                "Expected no error messages but found: " + entity.getMessages());
        }
        
        // Verify currencies exist in DB
        assertTrue(currencyRepository.existsById("TEST-USD"));
        assertTrue(currencyRepository.existsById("TEST-EUR"));
    }

    @Test
    public void testCreateOrUpdateAll_WithMissingKey_ShouldReturnPartialSuccess() {
        List<CurrencyRestEntity> currencies = new ArrayList<>();
        
        // Valid currency
        CurrencyRestEntity usd = new CurrencyRestEntity();
        usd.setCurrencyKey("TEST-USD2");
        usd.setSymbol("$");
        currencies.add(usd);
        
        // Invalid currency - missing key
        CurrencyRestEntity invalid = new CurrencyRestEntity();
        invalid.setSymbol("€");
        currencies.add(invalid);
        
        // Valid currency
        CurrencyRestEntity gbp = new CurrencyRestEntity();
        gbp.setCurrencyKey("TEST-GBP2");
        gbp.setSymbol("£");
        currencies.add(gbp);

        // Execute
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencies);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(3, result.getItems().size());
        
        // First and third should succeed
        assertTrue(currencyRepository.existsById("TEST-USD2"));
        assertTrue(currencyRepository.existsById("TEST-GBP2"));
        
        // Second should have error - convert to List to access by index
        List<CurrencyRestEntity> resultList = new ArrayList<>(result.getItems());
        CurrencyRestEntity errorEntity = resultList.get(1);
        assertNotNull(errorEntity.getMessages());
        assertFalse(errorEntity.getMessages().isEmpty());
        assertEquals(Message.MessageType.ERROR, errorEntity.getMessages().get(0).getType());
    }

    @Test
    public void testCreateOrUpdateAll_Update_ShouldSucceed() {
        // Create initial currency
        CurrencyEntity existing = new CurrencyEntity();
        existing.setCurrencyKey("TEST-EUR3");
        existing.setSymbol("€");
        currencyRepository.save(existing);

        // Update it
        List<CurrencyRestEntity> currencies = new ArrayList<>();
        CurrencyRestEntity eur = new CurrencyRestEntity();
        eur.setCurrencyKey("TEST-EUR3");
        eur.setSymbol("€€"); // Changed symbol
        Map<String, String> eurName = new HashMap<>();
        eurName.put("en", "Euro Updated");
        eur.setName(eurName);
        currencies.add(eur);

        // Execute
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencies);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
        
        // Verify updated
        CurrencyEntity updated = currencyRepository.findById("TEST-EUR3").orElse(null);
        assertNotNull(updated);
        assertEquals("€€", updated.getSymbol());
    }

    @Test
    public void testCreateOrUpdateAll_EmptyList_ShouldReturnError() {
        List<CurrencyRestEntity> currencies = new ArrayList<>();

        // Execute
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencies);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getMessages());
        assertFalse(result.getMessages().isEmpty());
        assertEquals(Message.MessageType.ERROR, result.getMessages().get(0).getType());
    }

    @Test
    public void testCreateOrUpdateAll_TooManyItems_ShouldReturnError() {
        List<CurrencyRestEntity> currencies = new ArrayList<>();
        
        // Create more than 100 items
        for (int i = 0; i < 101; i++) {
            CurrencyRestEntity currency = new CurrencyRestEntity();
            currency.setCurrencyKey("TEST-CUR" + i);
            currency.setSymbol("$");
            currencies.add(currency);
        }

        // Execute
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencies);

        // Verify
        assertNotNull(result);
        assertNotNull(result.getMessages());
        assertFalse(result.getMessages().isEmpty());
        assertEquals(Message.MessageType.ERROR, result.getMessages().get(0).getType());
    }

    @Test
    public void testCreateOrUpdateAll_MixedCreateAndUpdate_ShouldSucceed() {
        // Create existing currency
        CurrencyEntity existing = new CurrencyEntity();
        existing.setCurrencyKey("TEST-EXIST");
        existing.setSymbol("E");
        currencyRepository.save(existing);

        List<CurrencyRestEntity> currencies = new ArrayList<>();
        
        // Update existing
        CurrencyRestEntity update = new CurrencyRestEntity();
        update.setCurrencyKey("TEST-EXIST");
        update.setSymbol("E2");
        currencies.add(update);
        
        // Create new
        CurrencyRestEntity create = new CurrencyRestEntity();
        create.setCurrencyKey("TEST-NEW");
        create.setSymbol("N");
        currencies.add(create);

        // Execute
        CurrencyListRestEntity result = currencyFacade.createOrUpdateAllCurrencies(currencies);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.getItems().size());
        
        // Both should succeed
        assertTrue(currencyRepository.existsById("TEST-EXIST"));
        assertTrue(currencyRepository.existsById("TEST-NEW"));
        
        // Verify update
        CurrencyEntity updated = currencyRepository.findById("TEST-EXIST").orElse(null);
        assertEquals("E2", updated.getSymbol());
    }
}
