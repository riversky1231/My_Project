package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StudentMapper {

    // 基础 CRUD
    Student findById(Long id);
    int insert(Student student);
    int update(Student student);
    int deleteById(@Param("id") Long id);

    /**
     * 仅更新 defense_group_id，允许置空
     */
    int updateDefenseGroupId(@Param("id") Long id, @Param("groupId") Long groupId);

    // 查询学生列表
    List<Student> findAll(); // 【补充】Controller中调用，需要获取所有学生列表

    // 根据学号和年份查找
    Student findByStudentNoAndYear(@Param("studentNo") String studentNo, @Param("year") Integer year);

    // 根据院系和年份查找 【补充】院系管理员需要此方法
    List<Student> findByDepartmentAndYear(@Param("departmentId") Long departmentId, @Param("year") Integer year);

    // 根据指导教师和年份查找
    List<Student> findByAdvisorIdAndYear(@Param("advisorId") Long advisorId, @Param("year") Integer year);

    // 根据评阅人和年份查找
    List<Student> findByReviewerIdAndYear(@Param("reviewerId") Long reviewerId, @Param("year") Integer year);

    // 根据答辩小组ID查找
    List<Student> findByDefenseGroupId(Long groupId);
}