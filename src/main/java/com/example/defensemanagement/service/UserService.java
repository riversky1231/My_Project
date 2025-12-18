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

    // 修改：接收 departmentId 参数
    List<User> getAllUsers(Long departmentId);

    List<Role> getAllRoles();

    User saveUser(User user);

    List<Role> getManagableRoles(User currentUser);

    boolean deleteUser(Long userId);

    // 修改：接收 departmentId 参数
    List<User> searchUsers(String keyword, int page, int pageSize, Long departmentId);

    // 修改：接收 departmentId 参数
    int countUsers(String keyword, Long departmentId);
}