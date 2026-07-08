package io.commercestacksolutions.commons.service.setup;

/**
 * Manager interface for setup data import operations.
 * This interface defines the contract for data import management,
 * following Interface Driven Design (IDD) principles.
 */
public interface DataImportManager {
    
    /**
     * Loads all setup data (essential and sample data).
     * This method is typically called during application startup.
     */
    void loadData();
}
