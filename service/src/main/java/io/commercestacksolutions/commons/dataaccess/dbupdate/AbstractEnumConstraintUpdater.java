package io.commercestacksolutions.commons.dataaccess.dbupdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Abstract base class for enum constraint updaters.
 * Handles updating CHECK constraints for enum fields in both H2 and PostgreSQL databases.
 */
public abstract class AbstractEnumConstraintUpdater implements DBUpdateTask {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnumConstraintUpdater.class);
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Returns the table name for the enum constraint.
     */
    protected abstract String getTableName();
    
    /**
     * Returns the column name for the enum constraint.
     */
    protected abstract String getColumnName();
    
    /**
     * Returns the enum class for this constraint.
     */
    protected abstract Class<? extends Enum<?>> getEnumClass();
    
    /**
     * Returns the constraint name.
     * Default implementation uses table_column_check format.
     */
    protected String getConstraintName() {
        return getTableName() + "_" + getColumnName() + "_check";
    }
    
    @Override
    public int getPriority() {
        return 100; // Default priority for enum constraints
    }
    
    @Override
    public String getDescription() {
        return "Update " + getColumnName() + " enum constraint for " + getTableName() + " table";
    }
    
    @Override
    public void updateSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            
            LOGGER.debug("Database: {}", databaseProductName);
            
            // Get all enum values
            String[] enumValues = Arrays.stream(getEnumClass().getEnumConstants())
                    .map(Enum::name)
                    .toArray(String[]::new);
            
            if (enumValues.length == 0) {
                LOGGER.warn("No enum values found for {}", getEnumClass().getSimpleName());
                return;
            }
            
            String tableName = getTableName();
            String columnName = getColumnName();
            String constraintName = getConstraintName();
            
            // Drop existing constraint if it exists
            dropConstraintIfExists(connection, tableName, constraintName, databaseProductName);
            
            // Create new constraint with all enum values
            createEnumConstraint(connection, tableName, columnName, constraintName, enumValues, databaseProductName);
            
            LOGGER.info("Successfully updated enum constraint {} with {} values: {}", 
                    constraintName, enumValues.length, String.join(", ", enumValues));
        }
    }
    
    private void dropConstraintIfExists(Connection connection, String tableName, String constraintName, String databaseProductName) throws Exception {
        String dropSql;
        
        if (databaseProductName.toLowerCase().contains("h2")) {
            // H2 syntax
            dropSql = String.format("ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s", tableName, constraintName);
        } else if (databaseProductName.toLowerCase().contains("postgresql")) {
            // PostgreSQL syntax
            dropSql = String.format("ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s", tableName, constraintName);
        } else {
            LOGGER.warn("Unsupported database: {}. Skipping constraint drop.", databaseProductName);
            return;
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(dropSql);
            LOGGER.debug("Dropped existing constraint (if any): {}", constraintName);
        }
    }
    
    private void createEnumConstraint(Connection connection, String tableName, String columnName, 
                                     String constraintName, String[] enumValues, String databaseProductName) throws Exception {
        // Build the CHECK constraint with all enum values
        String enumValuesStr = Arrays.stream(enumValues)
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(", "));
        
        String createSql = String.format(
                "ALTER TABLE %s ADD CONSTRAINT %s CHECK (%s IN (%s))",
                tableName, constraintName, columnName, enumValuesStr
        );
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSql);
            LOGGER.debug("Created enum constraint: {}", constraintName);
        }
    }
}
