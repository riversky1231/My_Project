package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseLeader;

import java.util.List;

public interface TeacherService {
    
    /**
     * 根据教师编号查找教师
     */
    Teacher findByTeacherNo(String teacherNo);
    
    /**
     * 根据ID查找教师
     */
    Teacher findById(Long id);
    
    /**
     * 根据院系ID查找教师列表
     */
    List<Teacher> findByDepartmentId(Long departmentId);
    
    /**
     * 创建教师
     */
    Teacher createTeacher(String teacherNo, String name, Long departmentId, String title, String email, String phone);
    
    /**
     * 更新教师信息
     */
    boolean updateTeacher(Teacher teacher);
    
    /**
     * 更新教师状态
     */
    boolean updateTeacherStatus(Long teacherId, Integer status);
    
    /**
     * 设置答辩组长
     * @param teacherId 教师ID
     * @param year 年份
     * @param departmentId 院系ID
     */
    boolean setDefenseLeader(Long teacherId, Integer year, Long departmentId);
    
    /**
     * 取消答辩组长
     */
    boolean removeDefenseLeader(Long teacherId, Integer year);
    
    /**
     * 获取指定年份的答辩组长列表
     */
    List<DefenseLeader> getDefenseLeadersByYear(Integer year);
    
    /**
     * 获取指定院系和年份的答辩组长列表
     */
    List<DefenseLeader> getDefenseLeadersByDepartmentAndYear(Long departmentId, Integer year);
}