package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.Student;
import java.util.List;

public interface StudentService {

    /**
     * 根据ID查找学生
     */
    Student findById(Long studentId);

    /**
     * 根据答辩小组ID获取学生列表
     */
    List<Student> getStudentsByGroup(Long groupId);

    /**
     * 根据指导教师ID和年份获取学生列表
     */
    List<Student> getStudentsByAdvisor(Long teacherId, Integer year);

    /**
     * 分配/更新指导教师
     */
    boolean assignAdvisor(Long studentId, Long teacherId); //

    /**
     * 分配/更新评阅人
     */
    boolean assignReviewer(Long studentId, Long teacherId); //

    /**
     * 分配/更新学生所在的答辩小组
     */
    boolean assignDefenseGroup(Long studentId, Long groupId); //

    /**
     * 保存或更新学生基础信息
     */
    boolean saveStudent(Student student);

    /**
     * 获取所有学生列表
     */
    List<Student> findAll();

    List<Student> findByDepartmentAndYear(Long departmentId, Integer currentYear);
}