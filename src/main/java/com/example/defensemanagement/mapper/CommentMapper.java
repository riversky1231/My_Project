package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper {
    
    Comment findByGroupId(Long groupId);
    
    void insert(Comment comment);
    
    void update(Comment comment);
    
    void delete(Long id);
    
    void deleteByGroupId(Long groupId);
}