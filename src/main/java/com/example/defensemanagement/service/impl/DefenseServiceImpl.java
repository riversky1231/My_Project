package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.*;
import com.example.defensemanagement.mapper.*;
import com.example.defensemanagement.service.DefenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefenseServiceImpl implements DefenseService {

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private ArchiveSessionMapper archiveSessionMapper;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<DefenseGroup> getAllGroups() {
        return defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    public DefenseGroup getGroupById(Long id) {
        return defenseGroupMapper.findById(id);
    }

    @Override
    @Deprecated
    public List<String> getGroupMemberNames(Long groupId) {
        // 已废弃，返回空列表或调用新方法
        return getGroupStudentInfo(groupId);
    }
    
    @Override
    public List<String> getGroupStudentInfo(Long groupId) {
        DefenseGroup group = defenseGroupMapper.findById(groupId);
        if (group == null || group.getMembers() == null) {
            return new java.util.ArrayList<>();
        }
        return group.getMembers().stream()
            .map(student -> student.getName() + " - " + student.getTitle())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addComment(Long groupId, String commentContent) {
        Comment existingComment = commentMapper.findByGroupId(groupId);
        if (existingComment != null) {
            existingComment.setContent(commentContent);
            commentMapper.update(existingComment);
        } else {
            Comment comment = new Comment();
            comment.setContent(commentContent);
            comment.setGroupId(groupId);
            commentMapper.insert(comment);
        }
    }

    @Override
    @Transactional
    public void updateScore(Long groupId, int score) {
        defenseGroupMapper.updateScore(groupId, score);
    }

    @Override
    @Transactional
    public void updateOrder(List<Long> groupIds) {
        for (int i = 0; i < groupIds.size(); i++) {
            defenseGroupMapper.updateDisplayOrder(groupIds.get(i), i);
        }
    }

    @Override
    @Transactional
    public void addGroup(DefenseGroup defenseGroup) {
        defenseGroupMapper.insert(defenseGroup);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        // 学生表中的 defense_group_id 会自动设置为 NULL（需要数据库外键设置 SET NULL）
        commentMapper.deleteByGroupId(groupId);
        defenseGroupMapper.delete(groupId);
    }

    @Override
    @Transactional
    @Deprecated
    public void addMember(Long groupId, String memberName) {
        // 已废弃，通过学生表维护
        throw new UnsupportedOperationException("请通过学生管理模块添加学生到小组");
    }

    @Override
    @Transactional
    @Deprecated
    public void deleteMember(Long memberId) {
        // 已废弃，通过学生表维护
        throw new UnsupportedOperationException("请通过学生管理模块从小组移除学生");
    }

    @Override
    @Transactional
    public void archiveCurrentSession() {
        try {
            List<DefenseGroup> groups = getAllGroups();
            
            ArchiveSession archive = new ArchiveSession();
            archive.setSessionName("答辩会议 - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            archive.setArchiveTime(LocalDateTime.now());
            archive.setGroupCount(groups.size());
            
            if (!groups.isEmpty()) {
                double avgScore = groups.stream().mapToInt(DefenseGroup::getScore).average().orElse(0);
                int maxScore = groups.stream().mapToInt(DefenseGroup::getScore).max().orElse(0);
                archive.setAvgScore(avgScore);
                archive.setMaxScore(maxScore);
            }
            
            archive.setArchiveData(objectMapper.writeValueAsString(groups));
            
            archiveSessionMapper.insert(archive);
        } catch (Exception e) {
            throw new RuntimeException("归档失败", e);
        }
    }

    @Override
    public List<ArchiveSession> getArchiveList() {
        return archiveSessionMapper.findAll();
    }

    @Override
    public ArchiveDetail getArchiveDetail(Long archiveId) {
        try {
            ArchiveSession session = archiveSessionMapper.findById(archiveId);
            if (session == null) return null;
            
            ArchiveDetail detail = new ArchiveDetail();
            detail.setId(session.getId());
            detail.setSessionName(session.getSessionName());
            detail.setArchiveTime(session.getArchiveTime());
            
            List<DefenseGroup> groups = objectMapper.readValue(session.getArchiveData(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, DefenseGroup.class));
            
            List<ArchiveDetail.ArchiveGroup> archiveGroups = groups.stream().map(group -> {
                ArchiveDetail.ArchiveGroup ag = new ArchiveDetail.ArchiveGroup();
                ag.setName(group.getName());
                ag.setScore(group.getScore());
                // 使用 Student 对象
                if (group.getMembers() != null) {
                    ag.setMembers(group.getMembers().stream()
                        .map(student -> student.getName() + " - " + student.getTitle())
                        .collect(Collectors.toList()));
                }
                ag.setComment(group.getComment() != null ? group.getComment().getContent() : null);
                return ag;
            }).collect(Collectors.toList());
            
            detail.setGroups(archiveGroups);
            return detail;
        } catch (Exception e) {
            throw new RuntimeException("获取归档详情失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteArchive(Long archiveId) {
        archiveSessionMapper.delete(archiveId);
    }

    @Override
    @Transactional
    @Deprecated
    public void addGroupWithMembers(Object request) {
        // 已废弃，请通过学生管理模块维护
        throw new UnsupportedOperationException("请通过小组管理创建小组，通过学生管理添加成员");
    }

    public static class AddGroupRequest {
        private String name;
        private int score;
        private List<String> members;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public List<String> getMembers() { return members; }
        public void setMembers(List<String> members) { this.members = members; }
    }
}