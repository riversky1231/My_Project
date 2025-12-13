package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.*;

import java.util.List;

public interface DefenseService {
    
    /**
     * 获取所有小组，按显示顺序排序
     */
    List<DefenseGroup> getAllGroups();
    
    /**
     * 根据ID获取小组信息
     */
    DefenseGroup getGroupById(Long id);
    
    /**
     * 获取小组成员名称列表
     */
    List<String> getGroupMemberNames(Long groupId);
    
    /**
     * 添加评语
     */
    void addComment(Long groupId, String commentContent);
    
    /**
     * 更新小组得分
     */
    void updateScore(Long groupId, int score);
    
    /**
     * 更新小组显示顺序
     */
    void updateOrder(List<Long> groupIds);
    
    /**
     * 添加新小组
     */
    void addGroup(DefenseGroup defenseGroup);
    
    /**
     * 删除小组
     */
    void deleteGroup(Long groupId);
    
    /**
     * 添加成员
     */
    void addMember(Long groupId, String memberName);
    
    /**
     * 删除成员
     */
    void deleteMember(Long memberId);
    
    /**
     * 归档当前答辩
     */
    void archiveCurrentSession();
    
    /**
     * 获取归档列表
     */
    List<ArchiveSession> getArchiveList();
    
    /**
     * 获取归档详情
     */
    ArchiveDetail getArchiveDetail(Long archiveId);
    
    /**
     * 删除归档
     */
    void deleteArchive(Long archiveId);
    
    /**
     * 添加小组及其成员
     */
    void addGroupWithMembers(Object request);
}