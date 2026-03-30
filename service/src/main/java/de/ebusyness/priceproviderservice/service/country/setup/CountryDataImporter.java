package de.ebusyness.priceproviderservice.service.country.setup;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.service.country.CountryService;
import de.ebusyness.priceproviderservice.service.currency.CurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data importer for Country entities.
 *
 * <p>Overrides {@link #importFile(String)} to use a plain DTO for JSON deserialization.
 * The DTO uses String-typed currency references ({@code allowedCurrencyRefs} and
 * {@code primaryCurrencyRef}) to avoid Jackson identity-reference resolution issues
 * that arise when {@code CurrencyEntity} objects annotated with
 * {@code @JsonIdentityInfo} are referenced by bare key strings in the same document.
 * After parsing, each currency key is resolved to a managed {@link CurrencyEntity}
 * via {@link CurrencyService} before the country is saved.</p>
 */
@Component
public class CountryDataImporter extends AbstractSetupDataImporter<CountryEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountryDataImporter.class);

    private final CountryService countryService;
    private final CurrencyService currencyService;

    @Autowired
    public CountryDataImporter(CountryService countryService, CurrencyService currencyService) {
        super(countryService);
        this.countryService = countryService;
        this.currencyService = currencyService;
    }

    @Override
    public int getPriority() {
        return 65; // Load after currencies (60) but before tax classes (70)
    }

    @Override
    public String getEntityTypeName() {
        return "Country";
    }

    /**
     * Overrides the default import to parse the JSON file using {@link CountryImportDto}
     * (which uses plain String keys for currency references) and then converts to
     * {@link CountryEntity} with resolved {@link CurrencyEntity} objects.
     */
    @Override
    protected void importFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            LOGGER.debug("Lookup data file {} ", filePath);
            if (!Files.exists(path)) {
                LOGGER.debug("Data file {} not found. Skipped.", filePath);
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, CountryImportDto.class);
            List<CountryImportDto> dtos = objectMapper.readValue(Files.readString(path), type);

            for (CountryImportDto dto : dtos) {
                try {
                    CountryEntity entity = convertToEntity(dto);
                    countryService.save(entity);
                } catch (RuntimeException | EntityValidationException e) {
                    LOGGER.error("Unexpected exception in {} for country {}: {}", filePath, dto.getIsoKey(), e.getMessage(), e);
                }
            }
            LOGGER.info("Data file {} successfully imported.", filePath);

        } catch (IOException e) {
            LOGGER.error("Error occurred while loading data: " + e.getMessage());
        }
    }

    private CountryEntity convertToEntity(CountryImportDto dto) {
        CountryEntity entity = new CountryEntity(dto.getIsoKey());
        entity.setName(dto.getName());

        if (dto.getAllowedCurrencyRefs() != null && !dto.getAllowedCurrencyRefs().isEmpty()) {
            Set<CurrencyEntity> resolved = new HashSet<>();
            for (String key : dto.getAllowedCurrencyRefs()) {
                CurrencyEntity currency = currencyService.getCurrency(key);
                if (currency != null) {
                    resolved.add(currency);
                } else {
                    LOGGER.warn("Currency '{}' referenced in country '{}' not found – skipping.", key, dto.getIsoKey());
                }
            }
            entity.setAllowedCurrencyRefs(resolved);
        }

        if (dto.getPrimaryCurrencyRef() != null && !dto.getPrimaryCurrencyRef().isBlank()) {
            CurrencyEntity primary = currencyService.getCurrency(dto.getPrimaryCurrencyRef());
            if (primary != null) {
                entity.setPrimaryCurrencyRef(primary);
            } else {
                LOGGER.warn("Primary currency '{}' referenced in country '{}' not found – skipping.", dto.getPrimaryCurrencyRef(), dto.getIsoKey());
            }
        }

        return entity;
    }

    /**
     * Plain DTO used for JSON deserialization of Country data files.
     * Uses String keys for currency references to avoid {@code @JsonIdentityInfo}
     * forward-reference issues with {@link CurrencyEntity}.
     */
    static class CountryImportDto {
        private String isoKey;
        private Map<String, String> name;
        private List<String> allowedCurrencyRefs;
        private String primaryCurrencyRef;

        public String getIsoKey() { return isoKey; }
        public void setIsoKey(String isoKey) { this.isoKey = isoKey; }

        public Map<String, String> getName() { return name; }
        public void setName(Map<String, String> name) { this.name = name; }

        public List<String> getAllowedCurrencyRefs() { return allowedCurrencyRefs; }
        public void setAllowedCurrencyRefs(List<String> allowedCurrencyRefs) { this.allowedCurrencyRefs = allowedCurrencyRefs; }

        public String getPrimaryCurrencyRef() { return primaryCurrencyRef; }
        public void setPrimaryCurrencyRef(String primaryCurrencyRef) { this.primaryCurrencyRef = primaryCurrencyRef; }
    }
}
