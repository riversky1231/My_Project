package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Teacher;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TeacherMapper {
    
    Teacher findByTeacherNo(String teacherNo);
    
    @Select("SELECT * FROM teacher WHERE id = #{id}")
    @Results({
        @Result(property = "teacherNo", column = "teacher_no"),
        @Result(property = "departmentId", column = "department_id")
    })
    Teacher findById(Long id);
    
    @Select("SELECT * FROM teacher WHERE department_id = #{departmentId}")
    @Results({
        @Result(property = "teacherNo", column = "teacher_no"),
        @Result(property = "departmentId", column = "department_id")
    })
    List<Teacher> findByDepartmentId(Long departmentId);
    
    @Insert("INSERT INTO teacher (teacher_no, name, department_id, title, email, phone, password) " +
            "VALUES (#{teacherNo}, #{name}, #{departmentId}, #{title}, #{email}, #{phone}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Teacher teacher);
    
    @Update("UPDATE teacher SET password = #{password}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    @Update("UPDATE teacher SET name = #{name}, department_id = #{departmentId}, title = #{title}, " +
            "email = #{email}, phone = #{phone}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Teacher teacher);
    
    @Update("UPDATE teacher SET status = #{status}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}