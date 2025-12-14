package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.service.PermissionService;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private AuthService authService;

    @Override
    public boolean canEditUser(Object currentUser, User targetUser) {
        if (currentUser == null || targetUser == null || targetUser.getRole() == null) {
            return false;
        }

        if (currentUser instanceof User) {
            User adminUser = (User) currentUser;
            String adminRole = adminUser.getRole().getName();

            // 1. Super Admin can edit anyone
            if ("SUPER_ADMIN".equals(adminRole)) {
                return true;
            }

            // 2. Department Admin can edit Teachers and Defense Leaders in their department
            if ("DEPT_ADMIN".equals(adminRole)) {
                String targetRole = targetUser.getRole().getName();
                boolean isTargetTeacherOrLeader = "TEACHER".equals(targetRole) || "DEFENSE_LEADER".equals(targetRole);
                
                // Check if the target user (who must be a teacher) is in the same department
                if (isTargetTeacherOrLeader && adminUser.getDepartmentId().equals(targetUser.getDepartmentId())) {
                    return true;
                }
            }
        }

        if (currentUser instanceof Teacher) {
            Teacher teacherUser = (Teacher) currentUser;
            String targetRole = targetUser.getRole().getName();

            // 3. Teacher can edit Defense Leaders
            if ("DEFENSE_LEADER".equals(targetRole)) {
                // Additional logic could be added here, e.g., only from the same department
                return true;
            }
        }

        return false;
    }
}