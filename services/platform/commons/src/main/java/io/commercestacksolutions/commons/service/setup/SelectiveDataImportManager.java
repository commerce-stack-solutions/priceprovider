package io.commercestacksolutions.commons.service.setup;

import java.util.List;

/**
 * Manager interface for selective setup data import operations.
 * Extends DataImportManager with methods for on-demand data loading.
 */
public interface SelectiveDataImportManager extends DataImportManager {

    /**
     * Loads only essential data on demand.
     */
    void loadEssentialData();

    /**
     * Loads only sample data on demand.
     */
    void loadSampleData();

    /**
     * Asynchronously loads data based on provided flags.
     * This method runs in a background thread and handles transactions properly.
     *
     * @param loadEssential whether to load essential data
     * @param loadSample whether to load sample data
     */
    void loadDataAsync(boolean loadEssential, boolean loadSample);

    /**
     * Gets a list of all data files that would be loaded for essential data.
     * This is useful for previewing what files will be processed.
     * @return List of file paths relative to the data folder
     */
    List<String> getEssentialDataFiles();

    /**
     * Gets a list of all data files that would be loaded for sample data.
     * This is useful for previewing what files will be processed.
     * @return List of file paths relative to the data folder
     */
    List<String> getSampleDataFiles();

    /**
     * Gets the directory path for essential data files.
     * @return Directory path for essential data
     */
    String getEssentialDataDirectory();

    /**
     * Gets the directory path for sample data files.
     * @return Directory path for sample data
     */
    String getSampleDataDirectory();
}
