package de.ebusyness.priceproviderservice.facade.currency.restentity;

import de.ebusyness.commons.web.rest.RestEntity;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;

import java.util.Map;

public class CurrencyRestEntity extends RestEntity<InfoAuditableRestEntity, IncludesCurrency> {
    private String currencyKey;
    private String symbol;
    private Map<String, String> name;

    public String getCurrencyKey() {
        return currencyKey;
    }

    public void setCurrencyKey(String currencyKey) {
        this.currencyKey = currencyKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }
}
