package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/defense/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    /**
     * 教师提交/更新打分。
     */
    @PostMapping("/teacher/save")
    public String saveTeacherScore(@RequestBody TeacherScoreRecord record) {
        scoreService.saveTeacherScore(record);
        return "success";
    }

    /**
     * 小组得分汇总：计算小组均分、调节系数、最终答辩成绩、总评成绩。
     */
    @PostMapping("/group/finalize")
    public String finalizeGroup(@RequestParam Long groupId,
            @RequestParam Integer year,
            @RequestParam(required = false) Integer largeGroupScore) {
        scoreService.finalizeGroupScores(groupId, year, largeGroupScore);
        return "success";
    }

    /**
     * 获取小组的调节系数（答辩组长可以查看）
     * GET /defense/score/group/{groupId}/adjustmentFactor?year=2024
     */
    @GetMapping("/group/{groupId}/adjustmentFactor")
    public Map<String, Object> getAdjustmentFactor(@PathVariable Long groupId,
                                                    @RequestParam Integer year) {
        return scoreService.getGroupAdjustmentFactor(groupId, year);
    }

    /**
     * 设计类：输入总分，自动按权值拆分六个小项并保存。
     */
    @PostMapping("/design/autoSplit")
    public String autoSplitDesign(@RequestParam Long studentId,
            @RequestParam Long teacherId,
            @RequestParam Integer year,
            @RequestParam Integer totalScore,
            @RequestParam(required = false) Long defenseGroupId) {
        scoreService.autoSplitDesignScore(studentId, teacherId, year, totalScore, defenseGroupId);
        return "success";
    }

    /**
     * 设置指导教师成绩
     */
    @PostMapping("/advisor/set")
    public String setAdvisorScore(@RequestParam Long studentId,
            @RequestParam Integer year,
            @RequestParam Integer score) {
        scoreService.setAdvisorScore(studentId, year, score);
        return "success";
    }

    /**
     * 设置评阅人成绩
     */
    @PostMapping("/reviewer/set")
    public String setReviewerScore(@RequestParam Long studentId,
            @RequestParam Integer year,
            @RequestParam Integer score) {
        scoreService.setReviewerScore(studentId, year, score);
        return "success";
    }

    /**
     * 获取所有打分记录（超级管理员用）
     * GET /defense/score/records/list?year=2024
     */
    @GetMapping("/records/list")
    @ResponseBody
    public List<TeacherScoreRecord> getAllScoreRecords(@RequestParam(required = false) Integer year,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }

        if (year != null) {
            return teacherScoreRecordMapper.findByYear(year);
        } else {
            return teacherScoreRecordMapper.findAll();
        }
    }

    /**
     * 根据ID获取打分记录
     * GET /defense/score/records/{id}
     */
    @GetMapping("/records/{id}")
    @ResponseBody
    public TeacherScoreRecord getScoreRecordById(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
        return teacherScoreRecordMapper.findById(id);
    }

    /**
     * 更新打分记录
     * PUT /defense/score/records/update
     */
    @PutMapping("/records/update")
    @ResponseBody
    public String updateScoreRecord(@RequestBody TeacherScoreRecord record, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            return "error:权限不足";
        }

        try {
            if (teacherScoreRecordMapper.update(record) > 0) {
                return "success";
            } else {
                return "error:更新失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 删除打分记录
     * DELETE /defense/score/records/{id}
     */
    @DeleteMapping("/records/{id}")
    @ResponseBody
    public String deleteScoreRecord(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            return "error:权限不足";
        }

        try {
            if (teacherScoreRecordMapper.deleteById(id) > 0) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
