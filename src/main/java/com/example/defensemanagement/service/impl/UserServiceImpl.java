package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.UserMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;

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
        return new java.util.ArrayList<>();
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
    public boolean deleteDepartment(Long id) {
        return departmentMapper.deleteById(id) > 0;
    }

    // 修改：实现带 departmentId 的查询
    @Override
    public List<User> getAllUsers(Long departmentId) {
        return userMapper.findAll(departmentId);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleMapper.findAll();
    }

    @Override
    @Transactional
    public User saveUser(User user) {
        if (user.getId() == null) {
            // 新增用户
            if (userMapper.findByUsername(user.getUsername()) != null) {
                throw new RuntimeException("用户名已存在");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userMapper.insert(user);
            
            // 如果创建的是 TEACHER 角色的用户，自动在 teacher 表中创建对应的教师记录
            if (user.getRoleId() != null) {
                Role role = roleMapper.findById(user.getRoleId());
                if (role != null && "TEACHER".equals(role.getName())) {
                    // 生成教师编号（使用用户名去掉 teacher_ 前缀，或使用 user ID）
                    String teacherNo = user.getUsername().startsWith("teacher_") 
                        ? user.getUsername().substring(8).toUpperCase() 
                        : "T" + String.format("%03d", user.getId());
                    
                    // 检查教师编号是否已存在
                    Teacher existingTeacher = teacherMapper.findByTeacherNo(teacherNo);
                    if (existingTeacher == null) {
                        Teacher teacher = new Teacher();
                        teacher.setTeacherNo(teacherNo);
                        teacher.setName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                        teacher.setDepartmentId(user.getDepartmentId());
                        teacher.setEmail(user.getEmail());
                        teacher.setPhone(user.getPhone());
                        teacher.setStatus(user.getStatus());
                        teacher.setPassword(user.getPassword()); // 使用相同的加密密码
                        teacher.setUserId(user.getId()); // 关联到 user 表
                        teacherMapper.insert(teacher);
                    }
                }
            }
        } else {
            // 更新用户
            User oldUser = userMapper.findById(user.getId());
            if (oldUser == null) {
                throw new RuntimeException("用户不存在");
            }

            // 检查用户名是否被修改
            if (!oldUser.getUsername().equals(user.getUsername())) {
                User existingUser = userMapper.findByUsername(user.getUsername());
                if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("用户名已存在");
                }
            }

            // 处理密码：如果密码为空或只有空白字符，则不更新密码（设置为null，让UserSqlBuilder跳过）
            if (StringUtils.hasText(user.getPassword())) {
                System.out.println("更新用户密码: userId=" + user.getId() + ", 密码长度=" + user.getPassword().length());
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                // 如果密码为空，设置为null，这样UserSqlBuilder就不会更新密码字段
                System.out.println("密码为空，不更新密码字段: userId=" + user.getId());
                user.setPassword(null);
            }
            userMapper.update(user);
            
            // 如果用户关联了教师，同步更新教师信息
            if (user.getRoleId() != null) {
                Role role = roleMapper.findById(user.getRoleId());
                if (role != null && "TEACHER".equals(role.getName())) {
                    // 查找关联的教师
                    Teacher teacher = teacherMapper.findByUserId(user.getId());
                    
                    if (teacher != null) {
                        teacher.setName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                        teacher.setEmail(user.getEmail());
                        teacher.setPhone(user.getPhone());
                        teacher.setStatus(user.getStatus());
                        if (StringUtils.hasText(user.getPassword())) {
                            teacher.setPassword(user.getPassword());
                        }
                        teacherMapper.update(teacher);
                    }
                }
            }
        }
        return user;
    }

    @Override
    public List<Role> getManagableRoles(User currentUser) {
        List<Role> allRoles = roleMapper.findAll();
        if (currentUser.getRole() == null) {
            return new java.util.ArrayList<>();
        }
        String currentUserRoleName = currentUser.getRole().getName();

        List<Role> manageableRoles;
        switch (currentUserRoleName) {
            case "SUPER_ADMIN":
                manageableRoles = allRoles;
                break;
            case "DEPT_ADMIN":
                // 院系管理员只能创建 TEACHER 角色，不能创建 DEFENSE_LEADER（答辩组长在小组管理中指定）
                manageableRoles = allRoles.stream()
                        .filter(role -> role.getName().equals("TEACHER"))
                        .collect(Collectors.toList());
                break;
            case "TEACHER":
                manageableRoles = allRoles.stream()
                        .filter(role -> role.getName().equals("DEFENSE_LEADER"))
                        .collect(Collectors.toList());
                break;
            default:
                manageableRoles = new java.util.ArrayList<>();
                break;
        }

        return manageableRoles;
    }

    @Override
    public boolean deleteUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userMapper.deleteById(userId) > 0;
    }

    // 修改：实现带 departmentId 的搜索
    @Override
    public List<User> searchUsers(String keyword, int page, int pageSize, Long departmentId) {
        int offset = (page - 1) * pageSize;
        return userMapper.searchUsers(keyword, offset, pageSize, departmentId);
    }

    // 修改：实现带 departmentId 的计数
    @Override
    public int countUsers(String keyword, Long departmentId) {
        return userMapper.countUsers(keyword, departmentId);
    }
}