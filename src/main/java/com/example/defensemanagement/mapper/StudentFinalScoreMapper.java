package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.StudentFinalScore;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StudentFinalScoreMapper {
    StudentFinalScore findByStudentId(Long studentId);
    StudentFinalScore findByStudentIdAndYear(@Param("studentId") Long studentId, @Param("year") Integer year);
    List<StudentFinalScore> findByStudentIdsAndYear(@Param("studentIds") List<Long> studentIds, @Param("year") Integer year);
    int insert(StudentFinalScore finalScore);
    int update(StudentFinalScore finalScore);
    
    // 清空指导教师成绩（用于修改指导教师时级联删除）
    int clearAdvisorScore(@Param("studentId") Long studentId, @Param("year") Integer year);
    
    // 清空评阅教师成绩（用于修改评阅教师时级联删除）
    int clearReviewerScore(@Param("studentId") Long studentId, @Param("year") Integer year);
}