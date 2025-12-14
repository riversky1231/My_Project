package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;

import java.util.List;

public interface UserService {
    
    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);
    
    /**
     * 根据ID查找用户
     */
    User findById(Long id);
    
    /**
     * 创建院系管理员
     * @param username 用户名
     * @param password 密码
     * @param realName 真实姓名
     * @param email 邮箱
     * @param departmentId 院系ID
     * @return 创建的用户
     */
    User createDepartmentAdmin(String username, String password, String realName, String email, Long departmentId);
    
    /**
     * 更新用户信息
     */
    boolean updateUser(User user);
    
    /**
     * 更新用户状态
     */
    boolean updateUserStatus(Long userId, Integer status);
    
    /**
     * 获取指定角色的用户列表
     */
    List<User> getUsersByRole(String roleName);
    
    /**
     * 创建院系
     */
    Department createDepartment(String name, String code, String description);
    
    /**
     * 获取所有院系
     */
    List<Department> getAllDepartments();
    
    /**
     * 更新院系信息
     */
    boolean updateDepartment(Department department);
}