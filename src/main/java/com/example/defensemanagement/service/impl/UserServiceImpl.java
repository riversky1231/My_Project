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
        if (user.getId() == null) {
            // 新增用户
            if (userMapper.findByUsername(user.getUsername()) != null) {
                throw new RuntimeException("用户名已存在");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userMapper.insert(user);
        } else {
            // 更新用户
            User oldUser = userMapper.findById(user.getId());
            if (oldUser == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 检查用户名是否被修改，如果修改了需要验证新用户名是否已存在
            if (!oldUser.getUsername().equals(user.getUsername())) {
                User existingUser = userMapper.findByUsername(user.getUsername());
                if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("用户名已存在");
                }
            }
            
            // 处理密码：如果密码为空或null，保留原密码
            if (StringUtils.hasText(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                user.setPassword(oldUser.getPassword());
            }
            userMapper.update(user);
        }
        return user;
    }

    @Override
    public List<Role> getManagableRoles(User currentUser) {
        List<Role> allRoles = roleMapper.findAll();
        String currentUserRoleName = currentUser.getRole().getName();

        System.out.println("DEBUG: Current user role: " + currentUserRoleName);
        System.out.println("DEBUG: All roles from DB: " + allRoles.stream().map(Role::getName).collect(java.util.stream.Collectors.joining(", ")));

        List<Role> manageableRoles;
        switch (currentUserRoleName) {
            case "SUPER_ADMIN":
                manageableRoles = allRoles;
                break;
            case "DEPT_ADMIN":
                manageableRoles = allRoles.stream()
                        .filter(role -> role.getName().equals("TEACHER") || role.getName().equals("DEFENSE_LEADER"))
                        .collect(java.util.stream.Collectors.toList());
                break;
            case "TEACHER":
                manageableRoles = allRoles.stream()
                        .filter(role -> role.getName().equals("DEFENSE_LEADER"))
                        .collect(java.util.stream.Collectors.toList());
                break;
            default:
                manageableRoles = new java.util.ArrayList<>();
                break;
        }
        
        System.out.println("DEBUG: Roles to be returned: " + manageableRoles.stream().map(Role::getName).collect(java.util.stream.Collectors.joining(", ")));
        return manageableRoles;
    }
    
    @Override
    public boolean deleteUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userMapper.deleteById(userId) > 0;
    }
    
    @Override
    public List<User> searchUsers(String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return userMapper.searchUsers(keyword, offset, pageSize);
    }
    
    @Override
    public int countUsers(String keyword) {
        return userMapper.countUsers(keyword);
    }
}