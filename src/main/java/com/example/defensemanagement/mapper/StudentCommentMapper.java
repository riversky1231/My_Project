package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.StudentComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StudentCommentMapper {
    StudentComment findByStudentIdAndYear(@Param("studentId") Long studentId, @Param("year") Integer year);

    List<StudentComment> findByGroupIdAndYear(@Param("groupId") Long groupId, @Param("year") Integer year);

    int insert(StudentComment comment);

    int update(StudentComment comment);

    int deleteById(Long id);
}
