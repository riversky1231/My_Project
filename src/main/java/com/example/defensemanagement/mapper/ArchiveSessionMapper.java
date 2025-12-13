package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.ArchiveSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ArchiveSessionMapper {
    
    List<ArchiveSession> findAll();
    
    ArchiveSession findById(Long id);
    
    void insert(ArchiveSession archiveSession);
    
    void delete(Long id);
}