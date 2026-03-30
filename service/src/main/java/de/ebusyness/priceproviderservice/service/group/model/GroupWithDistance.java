package de.ebusyness.priceproviderservice.service.group.model;

/**
 * Represents a group with its distance level in the hierarchy.
 * 
 * The distance level indicates how far the group is from a reference group:
 * - Level 0: The group itself
 * - Level 1: Direct parent
 * - Level 2: Grandparent
 * - Level 3+: Further ancestors
 * 
 * This is used for sorting price rows where the nearest group in the hierarchy
 * should win (lower level = higher priority).
 */
public class GroupWithDistance {
    
    private final String groupId;
    private final int level;
    
    public GroupWithDistance(String groupId, int level) {
        this.groupId = groupId;
        this.level = level;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public int getLevel() {
        return level;
    }
    
    @Override
    public String toString() {
        return "GroupWithDistance{" +
                "groupId='" + groupId + '\'' +
                ", level=" + level +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        GroupWithDistance that = (GroupWithDistance) o;
        
        if (level != that.level) return false;
        return groupId != null ? groupId.equals(that.groupId) : that.groupId == null;
    }
    
    @Override
    public int hashCode() {
        int result = groupId != null ? groupId.hashCode() : 0;
        result = 31 * result + level;
        return result;
    }
}
