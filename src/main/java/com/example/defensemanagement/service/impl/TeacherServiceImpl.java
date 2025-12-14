package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseLeader;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.DefenseLeaderMapper;
import com.example.defensemanagement.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private DefenseLeaderMapper defenseLeaderMapper;
    
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
    public Teacher createTeacher(String teacherNo, String name, Long departmentId, String title, String email, String phone) {
        // 检查教师编号是否已存在
        if (teacherMapper.findByTeacherNo(teacherNo) != null) {
            throw new RuntimeException("教师编号已存在");
        }
        
        Teacher teacher = new Teacher();
        teacher.setTeacherNo(teacherNo);
        teacher.setName(name);
        teacher.setDepartmentId(departmentId);
        teacher.setTitle(title);
        teacher.setEmail(email);
        teacher.setPhone(phone);
        teacher.setStatus(1);
        // 初始密码设置为教师编号
        teacher.setPassword(passwordEncoder.encode(teacherNo));
        
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