package io.commercestacksolutions.commons.dataaccess.dbupdate;

/**
 * Interface for database schema updaters.
 * Implementations of this interface are automatically discovered and executed
 * by the SchemaUpdateManager on application startup.
 * 
 * Use this interface to update database constraints, especially enum type constraints
 * that need to be synchronized with Java enum definitions.
 */
public interface DBUpdateTask {
    
    /**
     * Returns the priority of this schema updater.
     * Lower numbers execute first. Use this to control the order of schema updates.
     * 
     * @return priority value (lower = higher priority)
     */
    int getPriority();
    
    /**
     * Executes the schema update operation.
     * This method is called once during application startup.
     * 
     * @throws Exception if the schema update fails
     */
    void updateSchema() throws Exception;
    
    /**
     * Returns a description of what this schema updater does.
     * Used for logging purposes.
     * 
     * @return human-readable description
     */
    String getDescription();
}
