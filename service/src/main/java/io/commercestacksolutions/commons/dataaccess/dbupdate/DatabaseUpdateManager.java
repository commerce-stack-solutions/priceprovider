package io.commercestacksolutions.commons.dataaccess.dbupdate;

/**
 * Manager interface for database update operations.
 * This interface defines the contract for database update task management,
 * following Interface Driven Design (IDD) principles.
 */
public interface DatabaseUpdateManager {
    
    /**
     * Runs all registered database update tasks.
     * Tasks are executed in priority order.
     * This method is typically called during application startup.
     */
    void runDbUpdateTasks();
}
