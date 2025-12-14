package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;

public interface AuthService {
    
    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码
     * @return 登录成功返回用户信息，失败返回null
     */
    User login(String username, String password);
    
    /**
     * 教师登录验证
     * @param teacherNo 教师编号
     * @param password 密码
     * @return 登录成功返回教师信息，失败返回null
     */
    Teacher teacherLogin(String teacherNo, String password);
    
    /**
     * 修改用户密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改成功返回true
     */
    boolean changeUserPassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 修改教师密码
     * @param teacherId 教师ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改成功返回true
     */
    boolean changeTeacherPassword(Long teacherId, String oldPassword, String newPassword);
    
    /**
     * 检查当前用户是否有指定权限
     * @param user 当前用户
     * @param permission 权限名称
     * @return 有权限返回true
     */
    boolean hasPermission(User user, String permission);
    
    /**
     * 检查教师是否为答辩组长
     * @param teacherId 教师ID
     * @param year 年份
     * @return 是答辩组长返回true
     */
    boolean isDefenseLeader(Long teacherId, Integer year);
}