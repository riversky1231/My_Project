package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.DefenseGroupTeacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DefenseGroupTeacherMapper {

    List<DefenseGroupTeacher> findByGroupId(@Param("groupId") Long groupId);

    DefenseGroupTeacher findLeaderByGroupId(@Param("groupId") Long groupId);

    int insert(@Param("groupId") Long groupId, @Param("teacherId") Long teacherId, @Param("isLeader") Integer isLeader);

    int delete(@Param("groupId") Long groupId, @Param("teacherId") Long teacherId);

    int clearLeader(@Param("groupId") Long groupId);

    int setLeader(@Param("groupId") Long groupId, @Param("teacherId") Long teacherId);
}


