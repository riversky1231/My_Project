package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.service.UserService;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.PermissionService;
import com.example.defensemanagement.service.TeacherService;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.RoleMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/departments")
    public String departmentManagement(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "redirect:/";
        }

        List<Department> departments = userService.getAllDepartments();
        model.addAttribute("departments", departments);
        return "admin/departments";
    }

    @PostMapping("/department/create")
    @ResponseBody
    public String createDepartment(@RequestParam String name,
            @RequestParam String code,
            @RequestParam String description,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            userService.createDepartment(name, code, description);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/department/update")
    @ResponseBody
    public String updateDepartment(@RequestBody Department department, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            if (userService.updateDepartment(department)) {
                return "success";
            } else {
                return "error:更新失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @DeleteMapping("/department/{id}")
    @ResponseBody
    public String deleteDepartment(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            // 检查是否有用户或学生关联到此院系
            List<User> users = userService.getAllUsers(id);
            if (users != null && !users.isEmpty()) {
                return "error:该院系下还有用户，无法删除";
            }

            // 删除院系
            if (userService.deleteDepartment(id)) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 批量删除院系
     * POST /admin/departments/batch-delete
     */
    @PostMapping("/departments/batch-delete")
    @ResponseBody
    public String batchDeleteDepartments(@RequestBody List<Long> ids, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        if (ids == null || ids.isEmpty()) {
            return "error:请选择要删除的院系";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (Long id : ids) {
            try {
                // 检查是否有用户或学生关联到此院系
                List<User> users = userService.getAllUsers(id);
                if (users != null && !users.isEmpty()) {
                    failCount++;
                    Department dept = departmentMapper.findById(id);
                    String deptName = dept != null ? dept.getName() : "ID:" + id;
                    errorMessages.append("院系[").append(deptName).append("]下还有用户，无法删除；");
                    continue;
                }

                // 删除院系
                if (userService.deleteDepartment(id)) {
                    successCount++;
                } else {
                    failCount++;
                    Department dept = departmentMapper.findById(id);
                    String deptName = dept != null ? dept.getName() : "ID:" + id;
                    errorMessages.append("院系[").append(deptName).append("]删除失败；");
                }
            } catch (Exception e) {
                failCount++;
                Department dept = departmentMapper.findById(id);
                String deptName = dept != null ? dept.getName() : "ID:" + id;
                errorMessages.append("院系[").append(deptName).append("]删除失败：").append(e.getMessage()).append("；");
            }
        }

        // 构建返回消息
        if (failCount == 0) {
            return "success";
        } else {
            String errorMsg = errorMessages.toString();
            if (errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500) + "...";
            }
            return "error:成功删除" + successCount + "个，失败" + failCount + "个。" + errorMsg;
        }
    }

    // 修改：传入 departmentId
    @GetMapping("/users/list")
    @ResponseBody
    public List<User> getUserList(HttpSession session) {
        Object userObject = session.getAttribute("currentUser");
        if (userObject == null) {
            userObject = session.getAttribute("currentTeacher");
        }
        if (userObject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        Long departmentId = getDepartmentIdIfDeptAdmin(userObject);
        return userService.getAllUsers(departmentId);
    }

    // 修改：传入 departmentId
    @GetMapping("/users/search")
    @ResponseBody
    public Map<String, Object> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int pageSize,
            HttpSession session) {

        Object userObject = session.getAttribute("currentUser");
        if (userObject == null) {
            userObject = session.getAttribute("currentTeacher");
        }
        if (userObject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        Long departmentId = getDepartmentIdIfDeptAdmin(userObject);

        List<User> users = userService.searchUsers(keyword, page, pageSize, departmentId);
        int total = userService.countUsers(keyword, departmentId);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        return result;
    }

    @GetMapping("/departments/list")
    @ResponseBody
    public List<Department> getDepartmentList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userService.getAllDepartments();
    }

    /**
     * 搜索院系（支持分页）
     * GET /admin/departments/search?keyword=xxx&page=1&pageSize=10
     */
    @GetMapping("/departments/search")
    @ResponseBody
    public Map<String, Object> searchDepartments(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        int offset = (page - 1) * pageSize;
        List<Department> departments = departmentMapper.searchDepartments(keyword, offset, pageSize);
        int total = departmentMapper.countDepartments(keyword);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("departments", departments);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        return result;
    }

    @GetMapping("/roles/list")
    @ResponseBody
    public List<com.example.defensemanagement.entity.Role> getRoleList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userService.getManagableRoles(currentUser);
    }

    @PostMapping("/users/save")
    @ResponseBody
    public String saveUser(@RequestBody User user, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        // Check permission for user creation/update
        if (user.getId() == null) { // For new user creation
            if (!permissionService.canCreateUser(currentUserObj, user)) {
                return "error:权限不足，无法创建该角色的用户";
            }
        } else { // For updates, check permission
            User targetUser = userService.findById(user.getId());
            if (targetUser == null) {
                return "error:目标用户不存在";
            }
            if (!permissionService.canEditUser(currentUserObj, targetUser)) {
                return "error:权限不足";
            }
        }

        try {
            userService.saveUser(user);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/user/{id}/status")
    @ResponseBody
    public String updateUserStatus(@PathVariable Long id,
            @RequestParam Integer status,
            HttpSession session) {

        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        User targetUser = userService.findById(id);
        if (targetUser == null) {
            return "error:目标用户不存在";
        }

        if (!permissionService.canEditUser(currentUserObj, targetUser)) {
            return "error:权限不足";
        }

        if (userService.updateUserStatus(id, status)) {
            return "success";
        } else {
            return "error:更新失败";
        }
    }

    @DeleteMapping("/user/{id}")
    @ResponseBody
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        User targetUser = userService.findById(id);
        if (targetUser == null) {
            return "error:目标用户不存在";
        }

        if (!permissionService.canEditUser(currentUserObj, targetUser)) {
            return "error:权限不足";
        }

        // 防止删除自己
        if (currentUserObj instanceof User) {
            User currentUser = (User) currentUserObj;
            if (currentUser.getId().equals(id)) {
                return "error:不能删除自己";
            }
        }

        try {
            if (userService.deleteUser(id)) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 批量删除用户
     * POST /admin/users/batch-delete
     */
    @PostMapping("/users/batch-delete")
    @ResponseBody
    public String batchDeleteUsers(@RequestBody List<Long> ids, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        if (ids == null || ids.isEmpty()) {
            return "error:请选择要删除的用户";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        Long currentUserId = null;
        if (currentUserObj instanceof User) {
            currentUserId = ((User) currentUserObj).getId();
        }

        for (Long id : ids) {
            try {
                // 防止删除自己
                if (currentUserId != null && currentUserId.equals(id)) {
                    failCount++;
                    User targetUser = userService.findById(id);
                    String username = targetUser != null ? targetUser.getUsername() : "ID:" + id;
                    errorMessages.append("用户[").append(username).append("]不能删除自己；");
                    continue;
                }

                User targetUser = userService.findById(id);
                if (targetUser == null) {
                    failCount++;
                    errorMessages.append("用户[ID:").append(id).append("]不存在；");
                    continue;
                }

                if (!permissionService.canEditUser(currentUserObj, targetUser)) {
                    failCount++;
                    errorMessages.append("用户[").append(targetUser.getUsername()).append("]权限不足，无法删除；");
                    continue;
                }

                // 删除用户
                if (userService.deleteUser(id)) {
                    successCount++;
                } else {
                    failCount++;
                    errorMessages.append("用户[").append(targetUser.getUsername()).append("]删除失败；");
                }
            } catch (Exception e) {
                failCount++;
                User targetUser = userService.findById(id);
                String username = targetUser != null ? targetUser.getUsername() : "ID:" + id;
                errorMessages.append("用户[").append(username).append("]删除失败：").append(e.getMessage()).append("；");
            }
        }

        // 构建返回消息
        if (failCount == 0) {
            return "success";
        } else {
            String errorMsg = errorMessages.toString();
            if (errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500) + "...";
            }
            return "error:成功删除" + successCount + "个，失败" + failCount + "个。" + errorMsg;
        }
    }

    /**
     * 检查用户是否是答辩组长
     * GET /admin/user/{userId}/isDefenseLeader
     */
    @GetMapping("/user/{userId}/isDefenseLeader")
    @ResponseBody
    public boolean isDefenseLeader(@PathVariable Long userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return false;
        }

        // 查找对应的教师ID
        User targetUser = userService.findById(userId);
        if (targetUser == null || targetUser.getRole() == null || !"TEACHER".equals(targetUser.getRole().getName())) {
            return false;
        }

        // 查找教师记录
        com.example.defensemanagement.entity.Teacher teacher = teacherMapper.findByUserId(userId);
        if (teacher == null) {
            return false;
        }

        // 检查是否是答辩组长（使用当前年份）
        java.time.LocalDate now = java.time.LocalDate.now();
        return authService.isDefenseLeader(teacher.getId(), now.getYear());
    }

    // 辅助方法：如果是院系管理员，返回其院系ID；否则返回null
    private Long getDepartmentIdIfDeptAdmin(Object userObj) {
        if (userObj instanceof User) {
            User user = (User) userObj;
            if (user.getRole() != null && "DEPT_ADMIN".equals(user.getRole().getName())) {
                return user.getDepartmentId();
            }
        }
        return null;
    }

    /**
     * 从Excel文件导入用户数据
     * POST /admin/users/import/excel
     * Excel文件应包含以下列：用户名、真实姓名、角色、院系、状态
     * 每行至少需要填写一个字段才能插入
     */
    @PostMapping("/users/import/excel")
    @ResponseBody
    public String importUsersFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        // 权限检查：只有超级管理员可以导入用户
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_USER")) {
            return "error:权限不足";
        }

        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return "error:请上传Excel文件（.xlsx或.xls格式）";
        }

        try {
            InputStream inputStream = file.getInputStream();
            Workbook workbook;

            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                workbook.close();
                inputStream.close();
                return "error:Excel文件为空";
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            // 识别表头
            int usernameCol = -1;
            int realNameCol = -1;
            int roleCol = -1;
            int departmentCol = -1;
            int statusCol = -1;
            int startRowIndex = 0;

            // 首先尝试识别表头
            boolean hasHeader = false;
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                    Cell cell = firstRow.getCell(i);
                    if (cell != null) {
                        String cellValue = getCellValueAsString(cell).trim();
                        if (cellValue.contains("用户名") || cellValue.equalsIgnoreCase("username")
                                || cellValue.equalsIgnoreCase("user")) {
                            usernameCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("真实姓名") || cellValue.contains("姓名")
                                || cellValue.equalsIgnoreCase("realname") || cellValue.equalsIgnoreCase("real_name")
                                || cellValue.equalsIgnoreCase("name")) {
                            realNameCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("角色") || cellValue.equalsIgnoreCase("role")) {
                            roleCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("院系") || cellValue.equalsIgnoreCase("department")
                                || cellValue.equalsIgnoreCase("dept")) {
                            departmentCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("状态") || cellValue.equalsIgnoreCase("status")) {
                            statusCol = i;
                            hasHeader = true;
                        }
                    }
                }
            }

            // 如果找到了表头，数据从第二行开始
            if (hasHeader && (usernameCol != -1 || realNameCol != -1 || roleCol != -1 || departmentCol != -1
                    || statusCol != -1)) {
                startRowIndex = 1;
            } else {
                // 如果没有找到表头，假设第一列是用户名，第二列是真实姓名，第三列是角色，第四列是院系，第五列是状态
                usernameCol = 0;
                realNameCol = 1;
                roleCol = 2;
                departmentCol = 3;
                statusCol = 4;
                startRowIndex = 0;
            }

            // 验证至少有一个有效列
            if (usernameCol == -1 && realNameCol == -1 && roleCol == -1 && departmentCol == -1 && statusCol == -1) {
                workbook.close();
                inputStream.close();
                return "error:Excel文件必须包含至少一列有效数据（用户名、真实姓名、角色、院系、状态中的任意一个）";
            }

            // 遍历数据行
            for (int rowIndex = startRowIndex; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    // 读取各列数据
                    String username = "";
                    String realName = "";
                    String roleName = "";
                    String departmentStr = "";
                    String statusStr = "";

                    if (usernameCol != -1) {
                        Cell usernameCell = row.getCell(usernameCol);
                        if (usernameCell != null) {
                            username = getCellValueAsString(usernameCell).trim();
                        }
                    }

                    if (realNameCol != -1) {
                        Cell realNameCell = row.getCell(realNameCol);
                        if (realNameCell != null) {
                            realName = getCellValueAsString(realNameCell).trim();
                        }
                    }

                    if (roleCol != -1) {
                        Cell roleCell = row.getCell(roleCol);
                        if (roleCell != null) {
                            roleName = getCellValueAsString(roleCell).trim();
                        }
                    }

                    if (departmentCol != -1) {
                        Cell deptCell = row.getCell(departmentCol);
                        if (deptCell != null) {
                            departmentStr = getCellValueAsString(deptCell).trim();
                        }
                    }

                    if (statusCol != -1) {
                        Cell statusCell = row.getCell(statusCol);
                        if (statusCell != null) {
                            statusStr = getCellValueAsString(statusCell).trim();
                        }
                    }

                    // 验证：至少有一个字段有数据
                    if (username.isEmpty() && realName.isEmpty() && roleName.isEmpty() && departmentStr.isEmpty()
                            && statusStr.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：用户名、真实姓名、角色、院系、状态不能同时为空；");
                        continue;
                    }

                    // 如果提供了用户名，检查是否已存在
                    if (!username.isEmpty()) {
                        User existingUser = userService.findByUsername(username);
                        if (existingUser != null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：用户名 ").append(username)
                                    .append("已存在；");
                            continue;
                        }
                    } else {
                        // 如果没有提供用户名，生成一个默认用户名
                        if (!realName.isEmpty()) {
                            username = realName + "_" + System.currentTimeMillis();
                        } else {
                            username = "user_" + System.currentTimeMillis();
                        }
                    }

                    // 解析角色
                    Long roleId = null;
                    if (!roleName.isEmpty()) {
                        // 支持角色名称（如SUPER_ADMIN、DEPT_ADMIN、TEACHER、DEFENSE_LEADER）或中文名称
                        Role role = null;
                        if (roleName.equalsIgnoreCase("SUPER_ADMIN") || roleName.contains("超级管理员")) {
                            role = roleMapper.findByName("SUPER_ADMIN");
                        } else if (roleName.equalsIgnoreCase("DEPT_ADMIN") || roleName.contains("院系管理员")) {
                            role = roleMapper.findByName("DEPT_ADMIN");
                        } else if (roleName.equalsIgnoreCase("TEACHER") || roleName.contains("教师")) {
                            role = roleMapper.findByName("TEACHER");
                        } else if (roleName.equalsIgnoreCase("DEFENSE_LEADER") || roleName.contains("答辩组长")) {
                            role = roleMapper.findByName("DEFENSE_LEADER");
                        } else {
                            // 尝试直接通过名称查找
                            role = roleMapper.findByName(roleName);
                        }

                        if (role == null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：角色 ").append(roleName)
                                    .append("不存在；");
                            continue;
                        }
                        roleId = role.getId();
                    }

                    // 解析院系
                    Long departmentId = null;
                    if (!departmentStr.isEmpty()) {
                        // 尝试通过院系代码查找
                        Department dept = departmentMapper.findByCode(departmentStr);
                        if (dept == null) {
                            // 如果代码找不到，尝试通过名称查找
                            List<Department> allDepts = userService.getAllDepartments();
                            for (Department d : allDepts) {
                                if (d.getName() != null && d.getName().equals(departmentStr)) {
                                    dept = d;
                                    break;
                                }
                            }
                        }

                        if (dept == null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：院系 ").append(departmentStr)
                                    .append("不存在；");
                            continue;
                        }
                        departmentId = dept.getId();
                    }

                    // 解析状态
                    Integer status = 1; // 默认启用
                    if (!statusStr.isEmpty()) {
                        try {
                            int statusInt = Integer.parseInt(statusStr);
                            if (statusInt == 0 || statusInt == 1) {
                                status = statusInt;
                            } else {
                                // 尝试解析中文状态
                                if (statusStr.contains("启用") || statusStr.contains("激活")
                                        || statusStr.equalsIgnoreCase("enabled")
                                        || statusStr.equalsIgnoreCase("active")) {
                                    status = 1;
                                } else if (statusStr.contains("禁用") || statusStr.contains("停用")
                                        || statusStr.equalsIgnoreCase("disabled")
                                        || statusStr.equalsIgnoreCase("inactive")) {
                                    status = 0;
                                } else {
                                    failCount++;
                                    errorMessages.append("第").append(rowIndex + 1).append("行：状态格式无效（应为0或1，或启用/禁用）；");
                                    continue;
                                }
                            }
                        } catch (NumberFormatException e) {
                            // 尝试解析中文状态
                            if (statusStr.contains("启用") || statusStr.contains("激活")
                                    || statusStr.equalsIgnoreCase("enabled") || statusStr.equalsIgnoreCase("active")) {
                                status = 1;
                            } else if (statusStr.contains("禁用") || statusStr.contains("停用")
                                    || statusStr.equalsIgnoreCase("disabled")
                                    || statusStr.equalsIgnoreCase("inactive")) {
                                status = 0;
                            } else {
                                failCount++;
                                errorMessages.append("第").append(rowIndex + 1).append("行：状态格式无效；");
                                continue;
                            }
                        }
                    }

                    // 判断是否为教师角色
                    boolean isTeacher = false;
                    if (roleId != null) {
                        Role role = roleMapper.findById(roleId);
                        if (role != null && "TEACHER".equals(role.getName())) {
                            isTeacher = true;
                        }
                    }

                    // 如果是教师角色，用户名就是教师编号，需要检查
                    if (isTeacher) {
                        if (username.isEmpty()) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：教师角色必须提供用户名（作为教师编号）；");
                            continue;
                        }
                        
                        // 检查教师编号（用户名）是否已存在
                        if (teacherMapper.findByTeacherNo(username) != null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：教师编号（用户名） ").append(username)
                                    .append("已存在；");
                            continue;
                        }
                    }

                    // 创建用户对象
                    User user = new User();
                    user.setUsername(username);
                    // 密码固定为"123456"（明文，saveUser方法会自动加密）
                    user.setPassword("123456");
                    user.setRealName(realName.isEmpty() ? null : realName);
                    user.setRoleId(roleId);
                    user.setDepartmentId(departmentId);
                    user.setStatus(status);
                    // 其他字段保持为null

                    // 保存用户（如果是教师角色，UserServiceImpl.saveUser会自动创建教师记录）
                    // saveUser方法会自动将密码加密后存入数据库
                    userService.saveUser(user);
                    
                    // 如果是教师角色，确保教师记录的信息正确（UserServiceImpl.saveUser已自动创建，这里只需要更新信息）
                    if (isTeacher && !username.isEmpty()) {
                        // 查找教师记录（UserServiceImpl.saveUser应该已自动创建）
                        com.example.defensemanagement.entity.Teacher existingTeacher = teacherMapper.findByUserId(user.getId());
                        
                        if (existingTeacher != null) {
                            // 更新教师记录的信息，确保与Excel中的数据一致
                            existingTeacher.setName(realName.isEmpty() ? username : realName);
                            existingTeacher.setDepartmentId(departmentId);
                            existingTeacher.setStatus(status);
                            // 确保教师编号 = 用户名（虽然应该已经一致，但为了保险还是更新一下）
                            if (!username.equals(existingTeacher.getTeacherNo())) {
                                teacherMapper.updateTeacherNo(existingTeacher.getId(), username);
                            }
                            // 更新密码为123456的加密值（与用户表一致）
                            teacherMapper.updatePassword(existingTeacher.getId(), user.getPassword());
                            teacherMapper.update(existingTeacher);
                        } else {
                            // 如果不存在（理论上不应该发生，因为UserServiceImpl.saveUser会自动创建），手动创建
                            com.example.defensemanagement.entity.Teacher newTeacher = new com.example.defensemanagement.entity.Teacher();
                            newTeacher.setTeacherNo(username); // 教师编号 = 用户名
                            newTeacher.setName(realName.isEmpty() ? username : realName);
                            newTeacher.setDepartmentId(departmentId);
                            newTeacher.setStatus(status);
                            newTeacher.setPassword(user.getPassword()); // 使用用户表的加密密码（saveUser已加密）
                            newTeacher.setUserId(user.getId());
                            teacherMapper.insert(newTeacher);
                        }
                    }
                    
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    String errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = e.getClass().getSimpleName();
                        if (e.getCause() != null) {
                            errorMsg += ": " + e.getCause().getMessage();
                        }
                    }
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(errorMsg).append("；");
                    // 打印完整异常堆栈以便调试
                    System.err.println("导入用户Excel第" + (rowIndex + 1) + "行失败：");
                    e.printStackTrace();
                }
            }

            workbook.close();
            inputStream.close();

            // 构建返回消息
            StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
            if (failCount > 0) {
                result.append("，失败").append(failCount).append("条");
                if (errorMessages.length() > 0) {
                    String errorMsg = errorMessages.toString();
                    // 限制错误消息长度
                    if (errorMsg.length() > 500) {
                        errorMsg = errorMsg.substring(0, 500) + "...";
                    }
                    result.append("。错误详情：").append(errorMsg);
                }
            }

            return result.toString();

        } catch (Exception e) {
            System.err.println("导入用户Excel失败：");
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
                if (e.getCause() != null) {
                    errorMsg += ": " + e.getCause().getMessage();
                }
            }
            return "error:导入失败：" + errorMsg;
        }
    }

    /**
     * 从Excel文件导入院系数据
     * POST /admin/departments/import/excel
     * Excel文件应包含以下列：ID、院系名称、院系代码、描述
     * 每行至少需要填写一个字段才能插入
     */
    @PostMapping("/departments/import/excel")
    @ResponseBody
    public String importDepartmentsFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        // 权限检查：只有超级管理员和院系管理员可以导入
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
            return "error:请上传Excel文件（.xlsx或.xls格式）";
        }

        try {
            InputStream inputStream = file.getInputStream();
            Workbook workbook;

            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 1) {
                workbook.close();
                inputStream.close();
                return "error:Excel文件为空";
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            // 识别表头
            int idCol = -1;
            int nameCol = -1;
            int codeCol = -1;
            int descriptionCol = -1;
            int startRowIndex = 0;

            // 首先尝试识别表头
            boolean hasHeader = false;
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                    Cell cell = firstRow.getCell(i);
                    if (cell != null) {
                        String cellValue = getCellValueAsString(cell).trim();
                        if (cellValue.contains("ID") || cellValue.contains("id") || cellValue.equalsIgnoreCase("id")) {
                            idCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("院系名称") || cellValue.contains("名称")
                                || cellValue.equalsIgnoreCase("name")) {
                            nameCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("院系代码") || cellValue.contains("代码")
                                || cellValue.equalsIgnoreCase("code")) {
                            codeCol = i;
                            hasHeader = true;
                        } else if (cellValue.contains("描述") || cellValue.equalsIgnoreCase("description")
                                || cellValue.equalsIgnoreCase("desc")) {
                            descriptionCol = i;
                            hasHeader = true;
                        }
                    }
                }
            }

            // 如果找到了表头，数据从第二行开始
            if (hasHeader && (idCol != -1 || nameCol != -1 || codeCol != -1 || descriptionCol != -1)) {
                startRowIndex = 1;
            } else {
                // 如果没有找到表头，假设第一列是ID，第二列是院系名称，第三列是院系代码，第四列是描述
                idCol = 0;
                nameCol = 1;
                codeCol = 2;
                descriptionCol = 3;
                startRowIndex = 0;
            }

            // 验证至少有一个有效列
            if (idCol == -1 && nameCol == -1 && codeCol == -1 && descriptionCol == -1) {
                workbook.close();
                inputStream.close();
                return "error:Excel文件必须包含至少一列有效数据（ID、院系名称、院系代码、描述中的任意一个）";
            }

            // 遍历数据行
            for (int rowIndex = startRowIndex; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    // 读取各列数据
                    String idStr = "";
                    String name = "";
                    String code = "";
                    String description = "";

                    if (idCol != -1) {
                        Cell idCell = row.getCell(idCol);
                        if (idCell != null) {
                            idStr = getCellValueAsString(idCell).trim();
                        }
                    }

                    if (nameCol != -1) {
                        Cell nameCell = row.getCell(nameCol);
                        if (nameCell != null) {
                            name = getCellValueAsString(nameCell).trim();
                        }
                    }

                    if (codeCol != -1) {
                        Cell codeCell = row.getCell(codeCol);
                        if (codeCell != null) {
                            code = getCellValueAsString(codeCell).trim();
                        }
                    }

                    if (descriptionCol != -1) {
                        Cell descCell = row.getCell(descriptionCol);
                        if (descCell != null) {
                            description = getCellValueAsString(descCell).trim();
                        }
                    }

                    // 验证：至少有一个字段有数据
                    if (idStr.isEmpty() && name.isEmpty() && code.isEmpty() && description.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：ID、院系名称、院系代码、描述不能同时为空；");
                        continue;
                    }

                    // 如果提供了ID，检查是否已存在
                    Long departmentId = null;
                    if (!idStr.isEmpty()) {
                        try {
                            departmentId = Long.parseLong(idStr);
                            Department existingDept = departmentMapper.findById(departmentId);
                            if (existingDept != null) {
                                failCount++;
                                errorMessages.append("第").append(rowIndex + 1).append("行：ID ").append(departmentId)
                                        .append("已存在；");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：ID格式无效；");
                            continue;
                        }
                    }

                    // 如果提供了院系代码，检查是否已存在
                    if (!code.isEmpty()) {
                        Department existingDeptByCode = departmentMapper.findByCode(code);
                        if (existingDeptByCode != null) {
                            failCount++;
                            errorMessages.append("第").append(rowIndex + 1).append("行：院系代码 ").append(code)
                                    .append("已存在；");
                            continue;
                        }
                    }

                    // 创建院系对象
                    Department department = new Department();
                    if (departmentId != null) {
                        department.setId(departmentId);
                    }
                    department.setName(name.isEmpty() ? null : name);
                    department.setCode(code.isEmpty() ? null : code);
                    department.setDescription(description.isEmpty() ? null : description);

                    // 保存院系
                    departmentMapper.insert(department);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }

            workbook.close();
            inputStream.close();

            // 构建返回消息
            StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
            if (failCount > 0) {
                result.append("，失败").append(failCount).append("条");
                if (errorMessages.length() > 0) {
                    String errorMsg = errorMessages.toString();
                    // 限制错误消息长度
                    if (errorMsg.length() > 500) {
                        errorMsg = errorMsg.substring(0, 500) + "...";
                    }
                    result.append("。错误详情：").append(errorMsg);
                }
            }

            return result.toString();

        } catch (Exception e) {
            return "error:导入失败：" + e.getMessage();
        }
    }

    /**
     * 辅助方法：获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}