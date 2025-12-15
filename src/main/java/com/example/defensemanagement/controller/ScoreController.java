package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/defense/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

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
}

