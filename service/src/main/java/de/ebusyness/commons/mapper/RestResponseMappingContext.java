package de.ebusyness.commons.mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RestResponseMappingContext {

    private final Set<String> expandPaths = new HashSet<>();
    private final Map<String, Object> properties = new HashMap<>();

    /**
     * Adds expand paths from the $expand parameter
     * Example: "$info.taxation", "$includes.unit", "$includes.currency"
     */
    public void addExpandPaths(Set<String> paths) {
        if (paths != null) {
            this.expandPaths.addAll(paths);
        }
    }

    /**
     * Check if any expand paths are configured
     */
    public boolean hasExpands() {
        return expandPaths.size() > 0;
    }

    /**
     * Check if a specific path should be expanded
     * Example: shouldExpand("$includes.unit") checks if unit should be included
     */
    public boolean shouldExpand(String path) {
        if(expandPaths.contains("all")) {
            return true;
        }
        return expandPaths.stream().anyMatch(expandPath -> expandPath.startsWith(path));
    }

    public boolean expandWithAnyOf(String[] pathes) {
        if(expandPaths.contains("all")) {
            return true;
        }
        for (String path : pathes) {
            if(expandPaths.contains(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all configured expand paths
     */
    public Set<String> getExpandPaths() {
        return new HashSet<>(expandPaths);
    }

    /**
     * Set a custom property in the context
     */
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    /**
     * Get a custom property from the context
     */
    public Object getProperty(String key) {
        return this.properties.get(key);
    }

}