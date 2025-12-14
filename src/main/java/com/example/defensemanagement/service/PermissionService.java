package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;

public interface PermissionService {

    /**
     * Checks if the current user can edit the target user.
     * @param currentUser The user performing the action. Can be User or Teacher.
     * @param targetUser The user being edited.
     * @return true if permission is granted, false otherwise.
     */
    boolean canEditUser(Object currentUser, User targetUser);

    /**
     * Checks if the current user can create a new user with the specified role.
     * @param currentUser The user performing the action. Can be User or Teacher.
     * @param newUser The user being created.
     * @return true if permission is granted, false otherwise.
     */
    boolean canCreateUser(Object currentUser, User newUser);
}