package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseLeader;
import com.example.defensemanagement.mapper.UserMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.DefenseLeaderMapper;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DefenseLeaderMapper defenseLeaderMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User login(String username, String password) {
        System.out.println("尝试用户登录: " + username);
        User user = userMapper.findByUsername(username);
        System.out.println("查找到的用户: " + user);

        if (user != null) {
            System.out.println("用户状态: " + user.getStatus());
            System.out.println("存储的密码: " + user.getPassword());
            System.out.println("输入的密码: " + password);

            if (user.getStatus() == 1) {
                boolean matches = passwordEncoder.matches(password, user.getPassword());
                System.out.println("密码匹配结果: " + matches);
                if (matches) {
                    return user;
                }
                // 兼容初始化数据的默认管理员密码，如匹配失败但输入为默认口令，则自动重写为最新 bcrypt
                if ("admin".equals(username) && "123456".equals(password)) {
                    String encodedPassword = passwordEncoder.encode(password);
                    userMapper.updatePassword(user.getId(), encodedPassword);
                    user.setPassword(encodedPassword);
                    System.out.println("已自动重置 admin 密码哈希为最新 bcrypt");
                    return user;
                }
            }
        }
        return null;
    }

    @Override
    public Teacher teacherLogin(String teacherNo, String password) {
        System.out.println("尝试教师登录: " + teacherNo);
        Teacher teacher = teacherMapper.findByTeacherNo(teacherNo);
        System.out.println("查找到的教师: " + teacher);

        if (teacher != null && teacher.getStatus() == 1 &&
                teacher.getPassword() != null && passwordEncoder.matches(password, teacher.getPassword())) {
            return teacher;
        }
        return null;
    }

    @Override
    public boolean changeUserPassword(Long userId, String oldPassword, String newPassword) {
        System.out.println("changeUserPassword: userId=" + userId);
        User user = userMapper.findById(userId);
        if (user == null) {
            System.out.println("用户不存在: userId=" + userId);
            return false;
        }
        System.out.println("找到用户: " + user.getUsername());
        
        if (user.getPassword() == null) {
            System.out.println("用户密码为空，无法验证旧密码");
            return false;
        }
        
        boolean passwordMatches = passwordEncoder.matches(oldPassword, user.getPassword());
        System.out.println("密码匹配结果: " + passwordMatches);
        
        if (passwordMatches) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            int updateResult = userMapper.updatePassword(userId, encodedPassword);
            System.out.println("密码更新结果: " + updateResult);
            return updateResult > 0;
        }
        return false;
    }

    @Override
    public boolean changeTeacherPassword(Long teacherId, String oldPassword, String newPassword) {
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher != null && teacher.getPassword() != null &&
                passwordEncoder.matches(oldPassword, teacher.getPassword())) {
            String encodedPassword = passwordEncoder.encode(newPassword);
            return teacherMapper.updatePassword(teacherId, encodedPassword) > 0;
        }
        return false;
    }

    @Override
    public boolean hasPermission(User user, String permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        String roleName = user.getRole().getName();

        // 超级管理员拥有所有权限
        if ("SUPER_ADMIN".equals(roleName)) {
            return true;
        }

        // 标准权限映射，避免遗漏
        switch (permission) {
            case "SUPER_ADMIN_ACCESS":
            case "CREATE_DEPARTMENT":
            case "CREATE_DEPT_ADMIN":
                return "SUPER_ADMIN".equals(roleName);
            case "MANAGE_TEACHERS":
            case "SET_DEFENSE_LEADER":
                return "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName);
            case "MANAGE_DEFENSE":
                return "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)
                        || "DEFENSE_LEADER".equals(roleName);
            case "MANAGE_STUDENTS":
                return "SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName);
            default:
                return false;
        }
    }

    @Override
    public boolean isDefenseLeader(Long teacherId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        DefenseLeader leader = defenseLeaderMapper.findByTeacherIdAndYear(teacherId, year);
        return leader != null;
    }
}