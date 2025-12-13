package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.DefenseGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DefenseGroupMapper {
    
    List<DefenseGroup> findAllByOrderByDisplayOrderAsc();
    
    DefenseGroup findById(Long id);
    
    void insert(DefenseGroup defenseGroup);
    
    void update(DefenseGroup defenseGroup);
    
    void delete(Long id);
    
    void updateScore(@Param("id") Long id, @Param("score") int score);
    
    void updateDisplayOrder(@Param("id") Long id, @Param("displayOrder") int displayOrder);
}