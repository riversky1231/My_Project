package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;

import java.util.List;

public interface UserService {
    
    User findByUsername(String username);
    
    User findById(Long id);
    
    User createDepartmentAdmin(String username, String password, String realName, String email, Long departmentId);
    
    boolean updateUser(User user);
    
    boolean updateUserStatus(Long userId, Integer status);
    
    List<User> getUsersByRole(String roleName);
    
    Department createDepartment(String name, String code, String description);
    
    List<Department> getAllDepartments();
    
    boolean updateDepartment(Department department);

    // New methods for user management
    List<User> getAllUsers();
    
    List<Role> getAllRoles();
    
    User saveUser(User user);
    
    List<Role> getManagableRoles(User currentUser);
}