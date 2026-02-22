package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.StudentPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentPreferenceMapper {
    StudentPreference findByStudentIdAndYear(@Param("studentId") Long studentId, @Param("year") Integer year);

    int insert(StudentPreference preference);

    int update(StudentPreference preference);
}
