package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseLeader;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Role;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.DefenseLeaderMapper;
import com.example.defensemanagement.mapper.UserMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private DefenseLeaderMapper defenseLeaderMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RoleMapper roleMapper;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public Teacher findByTeacherNo(String teacherNo) {
        return teacherMapper.findByTeacherNo(teacherNo);
    }
    
    @Override
    public Teacher findById(Long id) {
        return teacherMapper.findById(id);
    }
    
    @Override
    public List<Teacher> findByDepartmentId(Long departmentId) {
        return teacherMapper.findByDepartmentId(departmentId);
    }
    
    @Override
    @Transactional
    public Teacher createTeacher(String teacherNo, String name, Long departmentId, String title, String email, String phone) {
        // 检查教师编号是否已存在
        if (teacherMapper.findByTeacherNo(teacherNo) != null) {
            throw new RuntimeException("教师编号已存在");
        }
        
        // 1. 先在 user 表中创建教师用户
        Role teacherRole = roleMapper.findByName("TEACHER");
        if (teacherRole == null) {
            throw new RuntimeException("教师角色不存在");
        }
        
        User user = new User();
        user.setUsername("teacher_" + teacherNo.toLowerCase());
        user.setPassword(passwordEncoder.encode(teacherNo)); // 初始密码为教师编号
        user.setRealName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRoleId(teacherRole.getId());
        user.setDepartmentId(departmentId);
        user.setStatus(1);
        
        userMapper.insert(user);
        
        // 2. 在 teacher 表中创建教师记录，关联到 user
        Teacher teacher = new Teacher();
        teacher.setTeacherNo(teacherNo);
        teacher.setName(name);
        teacher.setDepartmentId(departmentId);
        teacher.setTitle(title);
        teacher.setEmail(email);
        teacher.setPhone(phone);
        teacher.setStatus(1);
        teacher.setPassword(passwordEncoder.encode(teacherNo)); // 保持兼容性，teacher表也存密码
        teacher.setUserId(user.getId()); // 关联到 user 表
        
        teacherMapper.insert(teacher);
        return teacher;
    }
    
    @Override
    public boolean updateTeacher(Teacher teacher) {
        return teacherMapper.update(teacher) > 0;
    }
    
    @Override
    public boolean updateTeacherStatus(Long teacherId, Integer status) {
        return teacherMapper.updateStatus(teacherId, status) > 0;
    }
    
    @Override
    public boolean setDefenseLeader(Long teacherId, Integer year, Long departmentId) {
        // 检查是否已经是答辩组长
        DefenseLeader existing = defenseLeaderMapper.findByTeacherIdAndYear(teacherId, year);
        if (existing != null) {
            return true; // 已经是答辩组长
        }
        
        DefenseLeader leader = new DefenseLeader();
        leader.setTeacherId(teacherId);
        leader.setYear(year);
        leader.setDepartmentId(departmentId);
        
        return defenseLeaderMapper.insert(leader) > 0;
    }
    
    @Override
    public boolean removeDefenseLeader(Long teacherId, Integer year) {
        return defenseLeaderMapper.deleteByTeacherIdAndYear(teacherId, year) > 0;
    }
    
    @Override
    public List<DefenseLeader> getDefenseLeadersByYear(Integer year) {
        return defenseLeaderMapper.findByYear(year);
    }
    
    @Override
    public List<DefenseLeader> getDefenseLeadersByDepartmentAndYear(Long departmentId, Integer year) {
        return defenseLeaderMapper.findByDepartmentIdAndYear(departmentId, year);
    }
}