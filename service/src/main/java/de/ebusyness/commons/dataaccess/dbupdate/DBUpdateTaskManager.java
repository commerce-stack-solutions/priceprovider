package de.ebusyness.commons.dataaccess.dbupdate;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of DatabaseUpdateManager interface.
 * Manager that discovers and executes all DBUpdateTask implementations.
 * Runs automatically on application startup via @PostConstruct.
 */
@Component
public class DBUpdateTaskManager implements DatabaseUpdateManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DBUpdateTaskManager.class);
    
    private final List<DBUpdateTask> dbUpdateTasks;
    
    @Autowired
    public DBUpdateTaskManager(List<DBUpdateTask> dbUpdateTasks) {
        this.dbUpdateTasks = dbUpdateTasks;
    }
    
    @PostConstruct
    @Override
    public void runDbUpdateTasks() {
        if (dbUpdateTasks == null || dbUpdateTasks.isEmpty()) {
            LOGGER.info("No db update tasks found");
            return;
        }
        
        LOGGER.info("Starting runDbUpdateTasks - found {} updater(s)", dbUpdateTasks.size());
        
        // Sort schema updaters by priority
        List<DBUpdateTask> sortedUpdaters = dbUpdateTasks.stream()
                .sorted((u1, u2) -> Integer.compare(u1.getPriority(), u2.getPriority()))
                .collect(Collectors.toList());
        
        // Execute each schema updater
        for (DBUpdateTask updater : sortedUpdaters) {
            try {
                LOGGER.info("Executing db update task with [Priority {}]: {}",
                    updater.getPriority(), updater.getDescription());
                updater.updateSchema();
                LOGGER.info("Successfully completed db update task: {}", updater.getDescription());
            } catch (Exception e) {
                LOGGER.error("Failed to execute db update task: {}", updater.getDescription(), e);
                // Continue with other updaters even if one fails
            }
        }
        
        LOGGER.info("runDbUpdateTasks completed");
    }
}
