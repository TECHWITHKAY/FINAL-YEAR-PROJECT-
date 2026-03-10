package com.ghana.commoditymonitor.security;

import com.ghana.commoditymonitor.enums.Role;

/**
 * Lightweight principal record representing an authenticated user.
 * <p>
 * This record is used to pass user information to controllers without
 * exposing the full User entity. It provides convenient role-checking methods.
 * </p>
 */
public record UserPrincipal(Long id, String username, Role role) {
    
    /**
     * Check if the user has ADMIN role.
     * 
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
    
    /**
     * Check if the user has FIELD_AGENT role.
     * 
     * @return true if user is a field agent
     */
    public boolean isFieldAgent() {
        return role == Role.FIELD_AGENT;
    }
    
    /**
     * Check if the user has ANALYST or ADMIN role.
     * 
     * @return true if user is an analyst or admin
     */
    public boolean isAnalyst() {
        return role == Role.ANALYST || role == Role.ADMIN;
    }
    
    /**
     * Check if the user can read full data (any authenticated role).
     * 
     * @return true if user has any role (is authenticated)
     */
    public boolean canReadFull() {
        return role != null;
    }
}
