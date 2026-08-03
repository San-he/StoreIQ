package com.storeiq.common;

import com.storeiq.entity.OperationLog;
import com.storeiq.mapper.OperationLogMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * 自动记录 POST/PUT/DELETE 操作
 */
@Aspect
@Component
public class OperationLogAspect {

    @Autowired(required = false)
    private OperationLogMapper logMapper;

    @Autowired(required = false)
    private HttpServletRequest request;

    /** 切点：Controller 层所有 POST/PUT/DELETE 方法（排除 auth/login） */
    @Pointcut("execution(* com.storeiq.controller..*(..)) && " +
              "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)) && " +
              "!execution(* com.storeiq.controller.AuthController.*(..)) && " +
              "!execution(* com.storeiq.controller.NotificationController.*(..))")
    public void operationPointcut() {}

    @AfterReturning(pointcut = "operationPointcut()", returning = "result")
    public void afterOperation(JoinPoint joinPoint, Object result) {
        if (logMapper == null) return;

        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            // 推断模块
            String module = inferModule(className);
            String action = inferAction(methodName);

            // 获取操作人
            Long userId = null;
            String username = null;
            if (request != null) {
                userId = (Long) request.getAttribute("userId");
                username = (String) request.getAttribute("username");
            }

            // 获取 IP
            String ip = request != null ? getClientIp(request) : null;

            OperationLog log = new OperationLog();
            log.setOperatorId(userId);
            log.setOperatorName(username);
            log.setModule(module);
            log.setAction(action);
            log.setTargetType(module);
            log.setDetail("执行 " + className + "." + methodName);
            log.setIpAddress(ip);
            log.setCreateTime(LocalDateTime.now());

            logMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不应影响主流程
        }
    }

    private String inferModule(String className) {
        if (className.contains("Store")) return "store";
        if (className.contains("Product") && !className.contains("Category")) return "product";
        if (className.contains("Category")) return "category";
        if (className.contains("Inventory")) return "inventory";
        if (className.contains("Purchase")) return "purchase";
        if (className.contains("Sale")) return "sale";
        if (className.contains("Supplier")) return "supplier";
        if (className.contains("User")) return "user";
        return "unknown";
    }

    private String inferAction(String methodName) {
        if (methodName.contains("save") || methodName.startsWith("save")) return "CREATE";
        if (methodName.contains("update") || methodName.startsWith("update")) return "UPDATE";
        if (methodName.contains("delete") || methodName.startsWith("delete")) return "DELETE";
        if (methodName.contains("create")) return "CREATE";
        return "UNKNOWN";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
