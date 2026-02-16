package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private AuthService authService;

    // 检查超级管理员权限的辅助方法
    private String checkSuperAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            return "error:权限不足";
        }
        return null; // 权限通过
    }
    
    // 检查管理员权限（超级管理员或院系管理员）
    private String checkAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:未登录";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"SUPER_ADMIN".equals(roleName) && !"DEPT_ADMIN".equals(roleName)) {
            return "error:权限不足";
        }
        return null; // 权限通过
    }

    /**
     * 获取评分指标配置列表
     * GET /admin/config/evaluation/list?type=PAPER/DESIGN
     */
    @GetMapping("/evaluation/list")
    @ResponseBody
    public List<EvaluationItem> getEvaluationItems(@RequestParam String type, HttpSession session) {
        // 允许超级管理员和院系管理员查看
        if (checkAdmin(session) != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }

        return configService.getEvaluationItems(type.toUpperCase());
    }

    /**
     * 保存或更新评分指标配置
     * POST /admin/config/evaluation/save
     */
    @PostMapping("/evaluation/save")
    @ResponseBody
    public String saveEvaluationItems(@RequestParam String type,
            @RequestBody List<EvaluationItem> items,
            HttpSession session) {
        // 允许超级管理员和院系管理员保存
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            configService.saveEvaluationItems(type.toUpperCase(), items);
            return "success";
        } catch (Exception e) {
            return "error:保存评分指标失败, " + e.getMessage();
        }
    }

    /**
     * 设置当前答辩年份
     * POST /admin/config/year/set?year=2025
     */
    @PostMapping("/year/set")
    @ResponseBody
    public String setCurrentDefenseYear(@RequestParam Integer year, HttpSession session) {
        // 允许超级管理员和院系管理员设置年份
        String permissionError = checkAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            configService.setCurrentDefenseYear(year);
            return "success";
        } catch (Exception e) {
            return "error:设置答辩年份失败, " + e.getMessage();
        }
    }

    /**
     * 获取当前答辩年份
     * GET /admin/config/year/current
     */
    @GetMapping("/year/current")
    @ResponseBody
    public Integer getCurrentDefenseYear() {
        // 此接口允许所有已登录用户获取，以便于页面加载时显示
        return configService.getCurrentDefenseYear();
    }

    /**
     * 获取所有年份列表（从学生表中提取）
     * GET /admin/config/years/list
     */
    @GetMapping("/years/list")
    @ResponseBody
    public List<Integer> getAllYears(HttpSession session) {
        // 允许所有已登录用户获取年份列表
        return configService.getAllYears();
    }

    /**
     * 设置答辩成绩表/成绩评定表的年、月、日
     * POST /admin/config/date/set
     * request body: { "dateKeyPrefix": "DEFENSE_DATE", "year": 2025, "month": 6,
     * "day": 30 }
     */
    @PostMapping("/date/set")
    @ResponseBody
    public String setDefenseDate(@RequestBody Map<String, Object> request, HttpSession session) {
        String permissionError = checkSuperAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String dateKeyPrefix = (String) request.get("dateKeyPrefix");
            Integer year = (Integer) request.get("year");
            Integer month = (Integer) request.get("month");
            Integer day = (Integer) request.get("day");

            if (dateKeyPrefix == null || year == null || month == null || day == null) {
                return "error:缺少日期参数";
            }

            configService.setDefenseDate(dateKeyPrefix, year, month, day);
            return "success";
        } catch (Exception e) {
            return "error:设置日期失败, " + e.getMessage();
        }
    }

    /**
     * 保存QWEN API Key
     * POST /admin/config/ai/key/save?apiKey=your_key
     */
    @PostMapping("/ai/key/save")
    @ResponseBody
    public String setQwenApiKey(@RequestParam String apiKey, HttpSession session) {
        String permissionError = checkSuperAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            configService.setQwenApiKey(apiKey);
            return "success";
        } catch (Exception e) {
            return "error:保存API Key失败, " + e.getMessage();
        }
    }

    /**
     * 保存评语提示词模板
     * POST /admin/config/ai/template/save
     * request body: { "templateKey": "PAPER_PROMPT", "templateContent": "基于摘要，请..."
     * }
     */
    @PostMapping("/ai/template/save")
    @ResponseBody
    public String setPromptTemplate(@RequestBody Map<String, String> request, HttpSession session) {
        String permissionError = checkSuperAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String templateKey = request.get("templateKey");
            String templateContent = request.get("templateContent");

            if (templateKey == null || templateContent == null) {
                return "error:缺少模板参数";
            }

            configService.setPromptTemplate(templateKey, templateContent);
            return "success";
        } catch (Exception e) {
            return "error:保存提示词模板失败, " + e.getMessage();
        }
    }

    /**
     * 获取评语提示词模板
     * GET /admin/config/ai/template/get?templateKey=PAPER_PROMPT
     */
    @GetMapping("/ai/template/get")
    @ResponseBody
    public String getPromptTemplate(@RequestParam String templateKey, HttpSession session) {
        String permissionError = checkSuperAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String template = configService.getPromptTemplate(templateKey);
            return template != null ? template : "";
        } catch (Exception e) {
            return "error:获取提示词模板失败, " + e.getMessage();
        }
    }

    /**
     * 获取日期配置
     * GET /admin/config/date/get?key=DEFENSE_DATE_YEAR
     */
    @GetMapping("/date/get")
    @ResponseBody
    public String getDefenseDatePart(@RequestParam String key, HttpSession session) {
        String permissionError = checkSuperAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String value = configService.getDefenseDatePart(key);
            return value != null ? value : "";
        } catch (Exception e) {
            return "error:获取日期配置失败, " + e.getMessage();
        }
    }

    /**
     * 获取QWEN API Key
     * GET /admin/config/ai/key/get
     */
    @GetMapping("/ai/key/get")
    @ResponseBody
    public String getQwenApiKey(HttpSession session) {
        String permissionError = checkSuperAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            String apiKey = configService.getConfigValue("QWEN_API_KEY");
            return apiKey != null ? apiKey : "";
        } catch (Exception e) {
            return "error:获取API Key失败, " + e.getMessage();
        }
    }
}