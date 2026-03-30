package de.ebusyness.commons.query;

import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import de.ebusyness.commons.exception.InvalidParameterException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpecificationBuilder, especially for collection handling and error cases.
 */
@DataJpaTest
@ActiveProfiles("test")
public class SpecificationBuilderTest {
    
    @Autowired
    private EntityManager entityManager;
    
    private final QueryParser parser = new QueryParser();
    
    @Test
    public void testExistsOnCollection() throws QueryParseException, InvalidParameterException {
        // Test that exists operator works on collections (subs is a Set<GroupEntity>)
        QueryExpression expr = parser.parse("subRefs.exists:true");
        Specification<GroupEntity> spec = SpecificationBuilder.build(expr);
        
        assertNotNull(spec);
        
        // Try to build a query with it - should not throw exception
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
        Root<GroupEntity> root = query.from(GroupEntity.class);
        query.where(spec.toPredicate(root, query, cb));
        
        // Should not throw exception
        TypedQuery<GroupEntity> typedQuery = entityManager.createQuery(query);
        assertNotNull(typedQuery);
    }
    
    @Test
    public void testNotExistsOnCollection() throws QueryParseException, InvalidParameterException {
        // Test that NOT EXISTS operator works on collections
        QueryExpression expr = parser.parse("subRefs.exists:false");
        Specification<GroupEntity> spec = SpecificationBuilder.build(expr);
        
        assertNotNull(spec);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
        Root<GroupEntity> root = query.from(GroupEntity.class);
        query.where(spec.toPredicate(root, query, cb));
        
        TypedQuery<GroupEntity> typedQuery = entityManager.createQuery(query);
        assertNotNull(typedQuery);
    }
    
    @Test
    public void testExistsOnSingleValuedReference() throws QueryParseException, InvalidParameterException {
        // Test that exists operator works on single-valued references
        QueryExpression expr = parser.parse("baseUnitRef.exists:true");
        Specification<UnitEntity> spec = SpecificationBuilder.build(expr);
        
        assertNotNull(spec);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UnitEntity> query = cb.createQuery(UnitEntity.class);
        Root<UnitEntity> root = query.from(UnitEntity.class);
        query.where(spec.toPredicate(root, query, cb));
        
        TypedQuery<UnitEntity> typedQuery = entityManager.createQuery(query);
        assertNotNull(typedQuery);
    }
    
    @Test
    public void testInvalidFieldThrowsQueryFilterRuntimeException() throws QueryParseException, InvalidParameterException {
        // Test that using an invalid field name throws QueryFilterRuntimeException
        QueryExpression expr = parser.parse("invalidFieldName:value");
        Specification<GroupEntity> spec = SpecificationBuilder.build(expr);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
        Root<GroupEntity> root = query.from(GroupEntity.class);
        
        // Should throw QueryFilterRuntimeException
        assertThrows(QueryFilterRuntimeException.class, () -> {
            spec.toPredicate(root, query, cb);
        });
    }
    
    @Test
    public void testInvalidNestedFieldThrowsQueryFilterRuntimeException() throws QueryParseException, InvalidParameterException {
        // Test that using an invalid nested field throws QueryFilterRuntimeException
        QueryExpression expr = parser.parse("baseUnitRef.invalidField:value");
        Specification<UnitEntity> spec = SpecificationBuilder.build(expr);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UnitEntity> query = cb.createQuery(UnitEntity.class);
        Root<UnitEntity> root = query.from(UnitEntity.class);
        
        assertThrows(QueryFilterRuntimeException.class, () -> {
            spec.toPredicate(root, query, cb);
        });
    }
    
    @Test
    public void testComplexQueryWithCollectionExists() throws QueryParseException, InvalidParameterException {
        // Test a complex query with collection exists
        QueryExpression expr = parser.parse("subRefs.exists:true AND name.exists:true");
        Specification<OrganizationEntity> spec = SpecificationBuilder.build(expr);
        
        assertNotNull(spec);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrganizationEntity> query = cb.createQuery(OrganizationEntity.class);
        Root<OrganizationEntity> root = query.from(OrganizationEntity.class);
        query.where(spec.toPredicate(root, query, cb));
        
        TypedQuery<OrganizationEntity> typedQuery = entityManager.createQuery(query);
        assertNotNull(typedQuery);
    }
    
    @Test
    public void testParentsCollectionExists() throws QueryParseException, InvalidParameterException {
        // Test exists on parents collection
        QueryExpression expr = parser.parse("parentRefs.exists:true");
        Specification<GroupEntity> spec = SpecificationBuilder.build(expr);
        
        assertNotNull(spec);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
        Root<GroupEntity> root = query.from(GroupEntity.class);
        query.where(spec.toPredicate(root, query, cb));
        
        TypedQuery<GroupEntity> typedQuery = entityManager.createQuery(query);
        assertNotNull(typedQuery);
    }
    
    @Test
    public void testInvalidFieldOnCollectionThrowsQueryFilterRuntimeException() throws QueryParseException, InvalidParameterException {
        // Test that using an invalid field on a collection throws QueryFilterRuntimeException
        // This tests the specific case: subs.invalidField:value
        QueryExpression expr = parser.parse("subRefs.invalidField:value");
        Specification<OrganizationEntity> spec = SpecificationBuilder.build(expr);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrganizationEntity> query = cb.createQuery(OrganizationEntity.class);
        Root<OrganizationEntity> root = query.from(OrganizationEntity.class);
        
        // Should throw QueryFilterRuntimeException which wraps InvalidParameterException
        QueryFilterRuntimeException exception = assertThrows(QueryFilterRuntimeException.class, () -> {
            spec.toPredicate(root, query, cb);
        });
        
        // Verify the wrapped exception is InvalidParameterException
        assertNotNull(exception.getInvalidParameterException());
        // Note: Trying to access a field on a collection results in fieldInvalid (not fieldUnknown)
        // because the collection path exists but the nested field access is invalid
        String messageKey = exception.getInvalidParameterException().getErrorResponse().getMessages().get(0).getMessageKey();
        assertTrue(messageKey.equals("common.errors.query.fieldUnknown") || messageKey.equals("common.errors.query.fieldInvalid"),
                   "Expected fieldUnknown or fieldInvalid, got: " + messageKey);
    }
    
    @Test
    public void testInvalidNestedFieldOnMapThrowsQueryFilterRuntimeException() throws QueryParseException, InvalidParameterException {
        // Test that using an invalid nested field on a Map throws QueryFilterRuntimeException
        // This tests the specific case from the bug report: name.invalidNested:value
        // where name is a Map<String, String>
        QueryExpression expr = parser.parse("name.invalidNested:value");
        Specification<LanguageEntity> spec = SpecificationBuilder.build(expr);
        
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LanguageEntity> query = cb.createQuery(LanguageEntity.class);
        Root<LanguageEntity> root = query.from(LanguageEntity.class);
        
        // Should throw QueryFilterRuntimeException which wraps InvalidParameterException
        QueryFilterRuntimeException exception = assertThrows(QueryFilterRuntimeException.class, () -> {
            spec.toPredicate(root, query, cb);
        });
        
        // Verify the wrapped exception is InvalidParameterException
        assertNotNull(exception.getInvalidParameterException());
        // TerminalPathException should result in fieldInvalid error
        String messageKey = exception.getInvalidParameterException().getErrorResponse().getMessages().get(0).getMessageKey();
        assertEquals("common.errors.query.fieldInvalid", messageKey,
                    "Expected fieldInvalid for TerminalPathException, got: " + messageKey);
    }
}
