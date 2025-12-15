package com.example.defensemanagement.mapper;

import com.example.defensemanagement.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemConfigMapper {
    SystemConfig findByKey(String configKey);
    int insertOrUpdate(SystemConfig config); // 通常使用UPSERT
}