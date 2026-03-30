# Testing

This document describes the testing strategies and examples for the Price Provider Service.

## Overview

The service uses **JUnit 5** and **Spring Boot Test** for all tests. Tests are organized into three categories:

| Category | Annotation | Purpose | Speed |
|----------|-----------|---------|-------|
| Unit Tests | Plain JUnit 5 | Test individual classes in isolation | Fast |
| Service Integration Tests | `@SpringBootTest` | Test service layer with a real in-memory H2 database | Medium |
| Controller Tests | `@WebMvcTest` | Test REST endpoints with mocked facades | Fast |

## Running Tests

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "de.ebusyness.priceproviderservice.service.unit.UnitEntityServiceCyclicDependencyTest"
```

## Unit Tests

Use plain JUnit 5 for classes that have no Spring dependencies:

- **Validation rules** (`ValidationRule<T>` implementations)
- **Mappers** (`AbstractMapper` subclasses)
- **Strategy implementations** (e.g., `DefaultPriceDeterminationStrategy`)
- **Utility classes** (e.g., `QueryParser`, `SpecificationBuilder`)

### Example: Validation Rule Unit Test

```java
class LanguageInactiveMandatoryRuleTest {

    private final LanguageInactiveMandatoryRule rule = new LanguageInactiveMandatoryRule();

    @Test
    void inactiveMandatoryLanguage_ShouldFail() {
        LanguageEntity language = new LanguageEntity();
        language.setActive(false);
        language.setMandatory(true);

        List<Message> errors = rule.validate(language);

        assertEquals(1, errors.size());
        assertEquals(Message.MessageType.ERROR, errors.get(0).getType());
        assertTrue(errors.get(0).getFields().contains("active"));
        assertTrue(errors.get(0).getFields().contains("mandatory"));
    }

    @Test
    void activeLanguage_ShouldPass() {
        LanguageEntity language = new LanguageEntity();
        language.setActive(true);
        language.setMandatory(true);

        assertTrue(rule.validate(language).isEmpty());
    }
}
```

## Service Integration Tests

Use `@SpringBootTest` with `@ActiveProfiles("test")` to test complete service flows against the in-memory H2 database:

```java
@SpringBootTest
@ActiveProfiles("test")
public class PublicPriceServiceIntegrationTest {

    @Autowired
    private PublicPriceService publicPriceService;

    @Autowired
    private PriceRowEntityRepository priceRowEntityRepository;

    @BeforeEach
    void setUp() {
        // Set up test data directly via repositories
        PriceRowEntity priceRow = new PriceRowEntity();
        priceRow.setPricedResourceId("TEST-PRODUCT");
        // ... configure other fields
        priceRowEntityRepository.save(priceRow);
    }

    @AfterEach
    void tearDown() {
        priceRowEntityRepository.deleteAll();
    }

    @Test
    void findBestPrice_ReturnsCorrectPrice() {
        PriceMatchingCriteria criteria = PriceMatchingCriteria.builder()
            .pricedResourceId("TEST-PRODUCT")
            .quantity(BigDecimal.ONE)
            .unit("piece")
            .currency("EUR")
            .priceType(PriceType.SALES_PRICE)
            .build();

        Optional<PriceRowEntity> result = publicPriceService.findBestPrice(criteria);

        assertTrue(result.isPresent());
        assertEquals("TEST-PRODUCT", result.get().getPricedResourceId());
    }
}
```

### Test Profile

The test profile (`application-test.yaml`) uses H2 in-memory database with:
- `spring.jpa.hibernate.ddl-auto: create-drop` – schema created fresh for each test class
- Sample data initialization disabled

## Controller Tests

Use `@WebMvcTest` with mocked facades for testing REST endpoints:

```java
@WebMvcTest(UnitController.class)
public class UnitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UnitFacadeService unitFacade;

    @Test
    void getUnit_WhenExists_Returns200() throws Exception {
        UnitRestEntity unit = new UnitRestEntity();
        unit.setSymbol("piece");
        when(unitFacade.getUnit(eq("piece"), any())).thenReturn(unit);

        mockMvc.perform(get("/api/admin/units/piece"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("piece"));
    }

    @Test
    void getUnit_WhenNotExists_Returns404() throws Exception {
        when(unitFacade.getUnit(eq("missing"), any()))
            .thenThrow(new NotFoundException("Unit not found"));

        mockMvc.perform(get("/api/admin/units/missing"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createUnit_WithValidData_Returns201() throws Exception {
        UnitRestEntity unit = new UnitRestEntity();
        unit.setSymbol("kg");
        when(unitFacade.create(any())).thenReturn(unit);

        mockMvc.perform(post("/api/admin/units/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\": \"kg\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.symbol").value("kg"));
    }
}
```

## Testing Strategies

### Strategy Pattern Tests

Business strategy implementations (e.g., `DefaultPriceDeterminationStrategy`) are tested with carefully constructed input data to verify ranking/sorting logic:

```java
class DefaultPriceDeterminationStrategyTest {

    private final DefaultPriceDeterminationStrategy strategy = new DefaultPriceDeterminationStrategy();

    @Test
    void rankPrices_PrefersMostRecentValidFrom() {
        PriceRowEntity older = buildPrice(/* validFrom 2023-01-01 */);
        PriceRowEntity newer = buildPrice(/* validFrom 2024-01-01 */);

        Optional<PriceRowEntity> best = strategy.determineBestPrice(List.of(older, newer), criteria);

        assertEquals(newer, best.orElse(null));
    }
}
```

### IDD Testing with Mocks

Thanks to IDD, every layer can be tested with Mockito mocks of its dependencies:

```java
@ExtendWith(MockitoExtension.class)
class UnitFacadeImplTest {

    @Mock
    private UnitService unitService; // Mock the interface

    @InjectMocks
    private UnitFacadeImpl unitFacade;

    @Test
    void getUnit_WhenFound_ReturnsMappedRestEntity() throws Exception {
        UnitEntity unitEntity = new UnitEntity();
        unitEntity.setSymbol("piece");
        when(unitService.getUnit("piece")).thenReturn(unitEntity);

        UnitRestEntity result = unitFacade.getUnit("piece", Set.of());

        assertNotNull(result);
        assertEquals("piece", result.getSymbol());
    }
}
```

## Test File Conventions

- Test classes are in the same package structure as the production code, under `src/test/java/`
- Test class names end with `Test` (unit tests), `IntegrationTest` (service integration tests), or `ControllerTest` (controller tests)
- Each test class has `@BeforeEach` and `@AfterEach` for setup/teardown as needed
- Use descriptive test method names: `methodUnderTest_StateUnderTest_ExpectedBehavior`
