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
    int insert(TeacherScoreRecord record);
    int update(TeacherScoreRecord record);
}