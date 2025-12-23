package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(String username);

    User findById(Long id);

    @Insert("INSERT INTO user (username, password, real_name, email, phone, status, role_id, department_id) " +
            "VALUES (#{username}, #{password}, #{realName}, #{email}, #{phone}, #{status}, #{roleId}, #{departmentId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET password = #{password}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @UpdateProvider(type = UserMapper.UserSqlBuilder.class, method = "buildUpdateUser")
    int update(User user);

    @Update("UPDATE user SET status = #{status}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(Long id);

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

    // 修改：增加 @Param 参数注解
    List<User> findAll(@Param("departmentId") Long departmentId);

    // 修改：增加 @Param 参数注解
    List<User> searchUsers(@Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("limit") int limit,
                           @Param("departmentId") Long departmentId);

    // 修改：增加 @Param 参数注解
    int countUsers(@Param("keyword") String keyword, @Param("departmentId") Long departmentId);

    static class UserSqlBuilder {
        public String buildUpdateUser(final User user) {
            return new org.apache.ibatis.jdbc.SQL() {{
                UPDATE("user");
                if (user.getUsername() != null) {
                    SET("username = #{username}");
                }
                // 只有当密码不为null且不为空字符串时才更新密码
                if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                    SET("password = #{password}");
                }
                if (user.getRealName() != null) {
                    SET("real_name = #{realName}");
                }
                if (user.getEmail() != null) {
                    SET("email = #{email}");
                }
                if (user.getPhone() != null) {
                    SET("phone = #{phone}");
                }
                if (user.getStatus() != null) {
                    SET("status = #{status}");
                }
                if (user.getRoleId() != null) {
                    SET("role_id = #{roleId}");
                }
                // 支持将 department_id 设置为 null（超级管理员可以没有院系）
                // 如果 departmentId 为 null，设置为 NULL；否则设置为对应的值
                // 注意：这里我们总是更新 department_id，前端需要明确发送 null 来清空院系
                if (user.getDepartmentId() != null) {
                    SET("department_id = #{departmentId}");
                } else {
                    // 如果 departmentId 为 null，设置为 NULL（允许超级管理员没有院系）
                    SET("department_id = NULL");
                }
                SET("updated_time = CURRENT_TIMESTAMP");
                WHERE("id = #{id}");
            }}.toString();
        }
    }
}