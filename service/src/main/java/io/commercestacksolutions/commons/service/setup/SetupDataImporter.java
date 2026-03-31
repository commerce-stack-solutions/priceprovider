package io.commercestacksolutions.commons.service.setup;

public interface SetupDataImporter {
    int getPriority();

    void loadEssentialData();

    void loadSampleData();
}