package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.TeacherProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeacherProfileMapper {
    TeacherProfile findByTeacherId(@Param("teacherId") Long teacherId);

    int insert(TeacherProfile profile);

    int update(TeacherProfile profile);
}
