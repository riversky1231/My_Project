package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.DefenseLeader;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DefenseLeaderMapper {
    
    @Select("SELECT dl.*, t.name as teacher_name, t.teacher_no, d.name as department_name " +
            "FROM defense_leader dl " +
            "LEFT JOIN teacher t ON dl.teacher_id = t.id " +
            "LEFT JOIN department d ON dl.department_id = d.id " +
            "WHERE dl.teacher_id = #{teacherId} AND dl.year = #{year}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "teacherId", column = "teacher_id"),
        @Result(property = "year", column = "year"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "teacher.id", column = "teacher_id"),
        @Result(property = "teacher.name", column = "teacher_name"),
        @Result(property = "teacher.teacherNo", column = "teacher_no"),
        @Result(property = "department.id", column = "department_id"),
        @Result(property = "department.name", column = "department_name")
    })
    DefenseLeader findByTeacherIdAndYear(@Param("teacherId") Long teacherId, @Param("year") Integer year);
    
    @Select("SELECT dl.*, t.name as teacher_name, t.teacher_no " +
            "FROM defense_leader dl " +
            "LEFT JOIN teacher t ON dl.teacher_id = t.id " +
            "WHERE dl.department_id = #{departmentId} AND dl.year = #{year}")
    @Results({
        @Result(property = "teacherId", column = "teacher_id"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "teacher.name", column = "teacher_name"),
        @Result(property = "teacher.teacherNo", column = "teacher_no")
    })
    List<DefenseLeader> findByDepartmentIdAndYear(@Param("departmentId") Long departmentId, @Param("year") Integer year);
    
    @Insert("INSERT INTO defense_leader (teacher_id, year, department_id) " +
            "VALUES (#{teacherId}, #{year}, #{departmentId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DefenseLeader defenseLeader);
    
    @Delete("DELETE FROM defense_leader WHERE teacher_id = #{teacherId} AND year = #{year}")
    int deleteByTeacherIdAndYear(@Param("teacherId") Long teacherId, @Param("year") Integer year);
    
    @Select("SELECT * FROM defense_leader WHERE year = #{year}")
    @Results({
        @Result(property = "teacherId", column = "teacher_id"),
        @Result(property = "departmentId", column = "department_id")
    })
    List<DefenseLeader> findByYear(Integer year);
}