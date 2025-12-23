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
}