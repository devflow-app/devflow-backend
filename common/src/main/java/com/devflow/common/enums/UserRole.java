package com.devflow.common.enums;

public enum UserRole {
    /**
     * Owner of an organization — full control, billing management
     */
    OWNER,

    /**
     * Administrator of a project — can manage members and settings
     */
    ADMIN,

    /**
     * Regular member — can create and manage tasks
     */
    MEMBER,

    /**
     * Read-only access to projects and boards
     */
    VIEWER
}
