package com.example.defensemanagement;

import com.example.defensemanagement.service.DefenseService;
import com.example.defensemanagement.entity.ArchiveSession;
import com.example.defensemanagement.entity.ArchiveDetail;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class DefenseController {

    @Autowired
    private DefenseService defenseService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // 检查是否已登录
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            return "redirect:/login";
        }

        model.addAttribute("groups", defenseService.getAllGroups());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentTeacher", currentTeacher);

        return "index";
    }

    @GetMapping("/group/{id}/members")
    @ResponseBody
    public List<String> getMembers(@PathVariable Long id) {
        // 返回学生姓名和项目名称
        return defenseService.getGroupStudentInfo(id);
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
        // Historical endpoint kept for backward compatibility with old UI.
        // Member management is now based on Student records (t_student.defense_group_id).
        throw new UnsupportedOperationException("成员维护已迁移到学生管理：请在学生管理中分配/移除学生的小组");
    }

    @DeleteMapping("/member/{id}")
    @ResponseBody
    public void deleteMember(@PathVariable Long id) {
        // Historical endpoint kept for backward compatibility with old UI.
        // Member management is now based on Student records (t_student.defense_group_id).
        throw new UnsupportedOperationException("成员维护已迁移到学生管理：请在学生管理中分配/移除学生的小组");
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
        DefenseGroup g = new DefenseGroup();
        g.setName(request.getName());
        g.setScore(request.getScore());
        // Put it at the end by default; user can reorder via updateOrder.
        int order = defenseService.getAllGroups() != null ? defenseService.getAllGroups().size() : 0;
        g.setDisplayOrder(order);
        defenseService.addGroup(g);
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