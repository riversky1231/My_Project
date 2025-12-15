package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.StudentFinalScore;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentFinalScoreMapper {
    StudentFinalScore findByStudentId(Long studentId);
    StudentFinalScore findByStudentIdAndYear(Long studentId, Integer year);
    int insert(StudentFinalScore finalScore);
    int update(StudentFinalScore finalScore);
}