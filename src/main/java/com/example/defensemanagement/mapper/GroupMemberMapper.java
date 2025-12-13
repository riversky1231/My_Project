package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GroupMemberMapper {
    
    List<GroupMember> findByGroupId(Long groupId);
    
    void insert(GroupMember groupMember);
    
    void update(GroupMember groupMember);
    
    void delete(Long id);
    
    void deleteByGroupId(Long groupId);
}