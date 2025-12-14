package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Department;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DepartmentMapper {
    
    @Select("SELECT * FROM department WHERE id = #{id}")
    Department findById(Long id);
    
    @Select("SELECT * FROM department WHERE code = #{code}")
    Department findByCode(String code);
    
    @Select("SELECT * FROM department ORDER BY id")
    List<Department> findAll();
    
    @Insert("INSERT INTO department (name, code, description) VALUES (#{name}, #{code}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Department department);
    
    @Update("UPDATE department SET name = #{name}, code = #{code}, description = #{description}, " +
            "updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Department department);
    
    @Delete("DELETE FROM department WHERE id = #{id}")
    int deleteById(Long id);
}