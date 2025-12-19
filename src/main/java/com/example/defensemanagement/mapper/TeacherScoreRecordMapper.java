package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.TeacherScoreRecord;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TeacherScoreRecordMapper {
    TeacherScoreRecord findByStudentIdAndTeacherId(Long studentId, Long teacherId);

    TeacherScoreRecord findByStudentIdAndTeacherIdAndYear(Long studentId, Long teacherId, Integer year);

    List<TeacherScoreRecord> findByStudentId(Long studentId);

    List<TeacherScoreRecord> findByStudentIdAndYear(Long studentId, Integer year);

    List<TeacherScoreRecord> findByGroupIdAndYear(Long defenseGroupId, Integer year);

    List<TeacherScoreRecord> findAll(); // 查询所有记录（超级管理员用）

    List<TeacherScoreRecord> findByYear(Integer year); // 按年份查询

    TeacherScoreRecord findById(Long id); // 根据ID查询

    int insert(TeacherScoreRecord record);

    int update(TeacherScoreRecord record);

    int deleteById(Long id); // 删除记录
}