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
    private GroupMemberMapper groupMemberMapper;
    
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
    public List<String> getGroupMemberNames(Long groupId) {
        List<GroupMember> members = groupMemberMapper.findByGroupId(groupId);
        return members.stream().map(GroupMember::getName).collect(Collectors.toList());
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
        groupMemberMapper.deleteByGroupId(groupId);
        commentMapper.deleteByGroupId(groupId);
        defenseGroupMapper.delete(groupId);
    }

    @Override
    @Transactional
    public void addMember(Long groupId, String memberName) {
        GroupMember member = new GroupMember();
        member.setName(memberName);
        member.setGroupId(groupId);
        groupMemberMapper.insert(member);
    }

    @Override
    @Transactional
    public void deleteMember(Long memberId) {
        groupMemberMapper.delete(memberId);
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
                ag.setMembers(group.getMembers().stream().map(GroupMember::getName).collect(Collectors.toList()));
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
    public void addGroupWithMembers(Object request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            AddGroupRequest addGroupRequest = objectMapper.readValue(requestJson, AddGroupRequest.class);
            
            DefenseGroup group = new DefenseGroup();
            group.setName(addGroupRequest.getName());
            group.setScore(addGroupRequest.getScore());
            
            List<DefenseGroup> existingGroups = defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
            int nextOrder = existingGroups.isEmpty() ? 0 : existingGroups.size();
            group.setDisplayOrder(nextOrder);
            
            defenseGroupMapper.insert(group);
            
            if (addGroupRequest.getMembers() != null && !addGroupRequest.getMembers().isEmpty()) {
                for (String memberName : addGroupRequest.getMembers()) {
                    if (memberName.trim().length() > 0) {
                        GroupMember member = new GroupMember();
                        member.setName(memberName.trim());
                        member.setGroupId(group.getId());
                        groupMemberMapper.insert(member);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("添加小组失败", e);
        }
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