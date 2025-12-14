package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    
    User findByUsername(String username);
    
    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results({
        @Result(property = "realName", column = "real_name"),
        @Result(property = "roleId", column = "role_id"),
        @Result(property = "departmentId", column = "department_id")
    })
    User findById(Long id);
    
    @Insert("INSERT INTO user (username, password, real_name, email, phone, role_id, department_id) " +
            "VALUES (#{username}, #{password}, #{realName}, #{email}, #{phone}, #{roleId}, #{departmentId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE user SET password = #{password}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    @Update("UPDATE user SET real_name = #{realName}, email = #{email}, phone = #{phone}, " +
            "role_id = #{roleId}, department_id = #{departmentId}, updated_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int update(User user);
    
    @Update("UPDATE user SET status = #{status}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    @Select("SELECT u.*, r.name as role_name, d.name as department_name " +
            "FROM user u " +
            "LEFT JOIN role r ON u.role_id = r.id " +
            "LEFT JOIN department d ON u.department_id = d.id " +
            "WHERE u.role_id = #{roleId}")
    @Results({
        @Result(property = "realName", column = "real_name"),
        @Result(property = "roleId", column = "role_id"),
        @Result(property = "departmentId", column = "department_id"),
        @Result(property = "role.name", column = "role_name"),
        @Result(property = "department.name", column = "department_name")
    })
    List<User> findByRoleId(Long roleId);
}