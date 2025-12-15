package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Override
    public Student findById(Long studentId) {
        return studentMapper.findById(studentId);
    }

    @Override
    public List<Student> findAll() {
        return studentMapper.findAll();
    }

    @Override
    public List<Student> findByDepartmentAndYear(Long departmentId, Integer currentYear) {
        return studentMapper.findByDepartmentAndYear(departmentId, currentYear);
    }

    @Override
    public List<Student> getStudentsByGroup(Long groupId) {
        return studentMapper.findByDefenseGroupId(groupId);
    }

    @Override
    public List<Student> getStudentsByAdvisor(Long teacherId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return studentMapper.findByAdvisorIdAndYear(teacherId, year);
    }

    @Override
    @Transactional
    public boolean assignAdvisor(Long studentId, Long teacherId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        // 检查教师是否存在
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("指导教师不存在：" + teacherId);
        }

        student.setAdvisorTeacherId(teacherId);
        return studentMapper.update(student) > 0;
    }

    @Override
    @Transactional
    public boolean assignReviewer(Long studentId, Long teacherId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        // 检查教师是否存在
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("评阅教师不存在：" + teacherId);
        }

        student.setReviewerTeacherId(teacherId);
        return studentMapper.update(student) > 0;
    }

    @Override
    @Transactional
    public boolean assignDefenseGroup(Long studentId, Long groupId) {
        Student student = findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在：" + studentId);
        }

        // 检查小组是否存在
        DefenseGroup group = defenseGroupMapper.findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("答辩小组不存在：" + groupId);
        }

        student.setDefenseGroupId(groupId);
        return studentMapper.update(student) > 0;
    }

    @Override
    @Transactional
    public boolean saveStudent(Student student) {
        if (student.getId() == null) {
            // 检查学号是否重复
            if (student.getStudentNo() != null && studentMapper.findByStudentNoAndYear(student.getStudentNo(), student.getDefenseYear()) != null) {
                throw new RuntimeException("学号在当前年份已存在");
            }
            return studentMapper.insert(student) > 0;
        } else {
            // 更新逻辑
            return studentMapper.update(student) > 0;
        }
    }
}