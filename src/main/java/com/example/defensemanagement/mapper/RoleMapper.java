package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {
    
    @Select("SELECT * FROM role WHERE id = #{id}")
    Role findById(Long id);
    
    @Select("SELECT * FROM role WHERE name = #{name}")
    Role findByName(String name);
    
    @Select("SELECT * FROM role ORDER BY id")
    List<Role> findAll();
    
    @Insert("INSERT INTO role (name, description) VALUES (#{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);
    
    @Update("UPDATE role SET name = #{name}, description = #{description} WHERE id = #{id}")
    int update(Role role);
    
    @Delete("DELETE FROM role WHERE id = #{id}")
    int deleteById(Long id);
}