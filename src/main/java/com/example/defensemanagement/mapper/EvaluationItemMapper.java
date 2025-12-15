package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.EvaluationItem;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface EvaluationItemMapper {
    List<EvaluationItem> findByType(String defenseType);
    int insert(EvaluationItem item);
    int update(EvaluationItem item);
    int delete(Long id);
    int deleteAllByType(String defenseType);
}