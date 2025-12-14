package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.mapper.UserMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private RoleMapper roleMapper;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    @Override
    public User createDepartmentAdmin(String username, String password, String realName, String email, Long departmentId) {
        if (userMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        Role deptAdminRole = roleMapper.findByName("DEPT_ADMIN");
        if (deptAdminRole == null) {
            throw new RuntimeException("院系管理员角色不存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setEmail(email);
        user.setStatus(1);
        user.setRoleId(deptAdminRole.getId());
        user.setDepartmentId(departmentId);
        userMapper.insert(user);
        return user;
    }
    
    @Override
    public boolean updateUser(User user) {
        return userMapper.update(user) > 0;
    }
    
    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        return userMapper.updateStatus(userId, status) > 0;
    }
    
    @Override
    public List<User> getUsersByRole(String roleName) {
        Role role = roleMapper.findByName(roleName);
        if (role != null) {
            return userMapper.findByRoleId(role.getId());
        }
        return null;
    }
    
    @Override
    public Department createDepartment(String name, String code, String description) {
        if (departmentMapper.findByCode(code) != null) {
            throw new RuntimeException("院系代码已存在");
        }
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setDescription(description);
        departmentMapper.insert(department);
        return department;
    }
    
    @Override
    public List<Department> getAllDepartments() {
        return departmentMapper.findAll();
    }
    
    @Override
    public boolean updateDepartment(Department department) {
        return departmentMapper.update(department) > 0;
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }

    @Override
    public List<Role> getAllRoles() {
        return roleMapper.findAll();
    }

    @Override
    public User saveUser(User user) {
        if (user.getId() == null) { // Create new user
            if (userMapper.findByUsername(user.getUsername()) != null) {
                throw new RuntimeException("用户名已存在");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userMapper.insert(user);
        } else { // Update existing user
            if (StringUtils.hasText(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                // Keep old password if not provided
                User oldUser = userMapper.findById(user.getId());
                user.setPassword(oldUser.getPassword());
            }
            userMapper.update(user);
        }
        return user;
    }
}