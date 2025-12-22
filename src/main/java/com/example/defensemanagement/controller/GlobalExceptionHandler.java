package com.example.defensemanagement.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理导出相关的异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        // 检查是否是导出相关的异常
        if (e.getMessage() != null && e.getMessage().contains("导出")) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            error.put("type", "export_error");
            
            // 如果是模板文件不存在的错误，返回400状态码
            if (e.getMessage().contains("模板文件不存在") || e.getMessage().contains("模板不存在")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // 其他导出错误返回500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
        
        // 其他RuntimeException，返回通用错误信息
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", e.getMessage() != null ? e.getMessage() : "服务器内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", e.getMessage());
        error.put("type", "validation_error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

