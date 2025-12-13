package com.example.defensemanagement;

import com.example.defensemanagement.service.DefenseService;
import com.example.defensemanagement.entity.ArchiveSession;
import com.example.defensemanagement.entity.ArchiveDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class DefenseController {

    @Autowired
    private DefenseService defenseService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("groups", defenseService.getAllGroups());
        return "index";
    }

    @GetMapping("/group/{id}/members")
    @ResponseBody
    public List<String> getMembers(@PathVariable Long id) {
        return defenseService.getGroupMemberNames(id);
    }

    @PostMapping("/comment")
    public String addComment(@RequestParam Long groupId, @RequestParam String comment) {
        defenseService.addComment(groupId, comment);
        return "redirect:/";
    }

    @PostMapping("/updateOrder")
    @ResponseBody
    public void updateOrder(@RequestBody List<Long> groupIds) {
        defenseService.updateOrder(groupIds);
    }

    @PostMapping("/group/{id}/score")
    @ResponseBody
    public void updateScore(@PathVariable Long id, @RequestParam int score) {
        defenseService.updateScore(id, score);
    }

    @PostMapping("/member/add")
    @ResponseBody
    public void addMember(@RequestBody GroupMemberRequest request) {
        defenseService.addMember(request.getGroupId(), request.getName());
    }

    @DeleteMapping("/member/{id}")
    @ResponseBody
    public void deleteMember(@PathVariable Long id) {
        defenseService.deleteMember(id);
    }

    @PostMapping("/archive/current")
    @ResponseBody
    public void archiveCurrentSession() {
        defenseService.archiveCurrentSession();
    }

    @GetMapping("/archive/list")
    @ResponseBody
    public List<ArchiveSession> getArchiveList() {
        return defenseService.getArchiveList();
    }

    @GetMapping("/archive/{id}/detail")
    @ResponseBody
    public ArchiveDetail getArchiveDetail(@PathVariable Long id) {
        return defenseService.getArchiveDetail(id);
    }

    @DeleteMapping("/archive/{id}")
    @ResponseBody
    public void deleteArchive(@PathVariable Long id) {
        defenseService.deleteArchive(id);
    }

    @PostMapping("/group/add")
    @ResponseBody
    public void addGroup(@RequestBody AddGroupRequest request) {
        defenseService.addGroupWithMembers(request);
    }

    // 内部类用于接收请求参数
    public static class GroupMemberRequest {
        private Long groupId;
        private String name;
        
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
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