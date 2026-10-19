package com.storeiq.common;

import com.storeiq.dto.PurchaseRequest;
import com.storeiq.dto.SaleRequest;
import com.storeiq.entity.OperationLog;
import com.storeiq.mapper.*;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志 AOP 切面
 * 自动记录真正的业务变更操作（POST/PUT/DELETE），排除只读接口；
 * 操作详情尽量生成人类可读描述，如：店铺「总店」入库商品「iPhone 15」× 2
 */
@Aspect
@Component
public class OperationLogAspect {

    @Autowired(required = false)
    private OperationLogMapper logMapper;

    @Autowired(required = false)
    private HttpServletRequest request;

    // 详情富化所需的业务 mapper（查库得到实体名称）
    @Autowired(required = false)
    private ProductMapper productMapper;
    @Autowired(required = false)
    private StoreMapper storeMapper;
    @Autowired(required = false)
    private SupplierMapper supplierMapper;
    @Autowired(required = false)
    private CategoryMapper categoryMapper;
    @Autowired(required = false)
    private UserMapper userMapper;

    /**
     * 切点：Controller 层 POST/PUT/DELETE 方法
     * 排除：登录/通知、AI 问答，以及 list/export/recent/modules/scan 等只读接口
     * （此前只读接口也会记日志，产生 "执行 LogController.list" 这类无意义噪声）
     */
    @Pointcut("execution(* com.storeiq.controller..*(..)) && " +
              "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
              "@annotation(org.springframework.web.bind.annotation.DeleteMapping)) && " +
              "!execution(* com.storeiq.controller.AuthController.*(..)) && " +
              "!execution(* com.storeiq.controller.NotificationController.*(..)) && " +
              "!execution(* com.storeiq.controller.AiController.*(..)) && " +
              "!execution(* com.storeiq.controller..*.list(..)) && " +
              "!execution(* com.storeiq.controller..*.export(..)) && " +
              "!execution(* com.storeiq.controller..*.recent(..)) && " +
              "!execution(* com.storeiq.controller..*.modules(..)) && " +
              "!execution(* com.storeiq.controller..*.scan*(..))")
    public void operationPointcut() {}

    @AfterReturning(pointcut = "operationPointcut()", returning = "result")
    public void afterOperation(JoinPoint joinPoint, Object result) {
        if (logMapper == null) return;

        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();

            String module = inferModule(className);
            String action = inferAction(methodName);

            // 生成可读详情（无法富化时兜底为 "执行 Class.method"）
            String detail = buildDetail(module, action, joinPoint.getArgs());
            if (detail == null) detail = "执行 " + className + "." + methodName;

            Long userId = null;
            String username = null;
            if (request != null) {
                userId = (Long) request.getAttribute("userId");
                username = (String) request.getAttribute("username");
            }
            String ip = request != null ? getClientIp(request) : null;

            OperationLog log = new OperationLog();
            log.setOperatorId(userId);
            log.setOperatorName(username);
            log.setModule(module);
            log.setAction(action);
            log.setTargetType(module);
            log.setDetail(detail);
            log.setIpAddress(ip);
            log.setCreateTime(LocalDateTime.now());

            logMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不应影响主流程
        }
    }

    /** 生成人类可读的操作详情 */
    private String buildDetail(String module, String action, Object[] args) {
        // 1. 入库/出库：DTO 只有 id，查库得到名称
        if ("CREATE".equals(action)) {
            for (Object arg : args) {
                if (arg instanceof PurchaseRequest) {
                    PurchaseRequest r = (PurchaseRequest) arg;
                    return String.format("店铺「%s」入库商品「%s」× %d",
                            lookupStoreName(r.getStoreId()), lookupProductName(r.getProductId()), r.getQuantity());
                }
                if (arg instanceof SaleRequest) {
                    SaleRequest r = (SaleRequest) arg;
                    return String.format("店铺「%s」出库商品「%s」× %d",
                            lookupStoreName(r.getStoreId()), lookupProductName(r.getProductId()), r.getQuantity());
                }
            }
        }
        // 2. 批量操作：参数为 List（批量入库/出库）
        for (Object arg : args) {
            if (arg instanceof List) {
                String verb = "批量操作";
                if ("purchase".equals(module)) verb = "批量入库";
                if ("sale".equals(module)) verb = "批量出库";
                return verb + " " + ((List<?>) arg).size() + " 条记录";
            }
        }
        // 3. 按 id 操作：删除查库得到名称；修改给出 id（如库存状态切换等无实体参数的操作）
        if ("DELETE".equals(action) || "UPDATE".equals(action)) {
            for (Object arg : args) {
                if (arg instanceof Long) {
                    if ("DELETE".equals(action)) {
                        return "删除" + moduleLabel(module) + "「" + lookupNameByModule(module, (Long) arg) + "」";
                    }
                    return "修改" + moduleLabel(module) + "（id=" + arg + "）";
                }
            }
        }
        // 4. 通用：实体/DTO 自带名称字段（name / nickname / username 依次尝试）
        for (Object arg : args) {
            String name = extractName(arg);
            if (name != null) {
                String verb = "CREATE".equals(action) ? "新增" : "UPDATE".equals(action) ? "修改" : "操作";
                return verb + moduleLabel(module) + "「" + name + "」";
            }
        }
        return null;
    }

    /** 反射提取对象中的名称（getName / getNickname / getUsername 依次尝试） */
    private String extractName(Object obj) {
        if (obj == null) return null;
        for (String getter : new String[]{"getName", "getNickname", "getUsername"}) {
            try {
                Method m = obj.getClass().getMethod(getter);
                Object v = m.invoke(obj);
                if (v instanceof String && !((String) v).isEmpty()) return (String) v;
            } catch (Exception ignored) { /* 不存在则尝试下一个 */ }
        }
        return null;
    }

    private String lookupStoreName(Long id) {
        try {
            return id != null && storeMapper != null && storeMapper.selectById(id) != null
                    ? storeMapper.selectById(id).getName() : "未知店铺";
        } catch (Exception e) { return "未知店铺"; }
    }

    private String lookupProductName(Long id) {
        try {
            return id != null && productMapper != null && productMapper.selectById(id) != null
                    ? productMapper.selectById(id).getName() : "未知商品";
        } catch (Exception e) { return "未知商品"; }
    }

    /** 按模块查库得到实体名称（删除日志用） */
    private String lookupNameByModule(String module, Long id) {
        try {
            switch (module) {
                case "store": return storeMapper != null && storeMapper.selectById(id) != null ? storeMapper.selectById(id).getName() : "未知";
                case "product": return productMapper != null && productMapper.selectById(id) != null ? productMapper.selectById(id).getName() : "未知";
                case "category": return categoryMapper != null && categoryMapper.selectById(id) != null ? categoryMapper.selectById(id).getName() : "未知";
                case "supplier": return supplierMapper != null && supplierMapper.selectById(id) != null ? supplierMapper.selectById(id).getName() : "未知";
                case "user": return userMapper != null && userMapper.selectById(id) != null ? userMapper.selectById(id).getUsername() : "未知";
                default: return "id=" + id;
            }
        } catch (Exception e) { return "id=" + id; }
    }

    /** 模块代码 → 中文名（用于拼接可读详情） */
    private String moduleLabel(String module) {
        switch (module) {
            case "store": return "店铺";
            case "product": return "商品";
            case "category": return "分类";
            case "inventory": return "库存记录";
            case "purchase": return "进货记录";
            case "sale": return "销售记录";
            case "supplier": return "供应商";
            case "user": return "用户";
            case "cleanup": return "数据清理任务";
            default: return "记录";
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
        if (className.contains("Cleanup")) return "cleanup";
        return "unknown";
    }

    private String inferAction(String methodName) {
        if (methodName.contains("save") || methodName.startsWith("save")) return "CREATE";
        if (methodName.contains("update") || methodName.startsWith("update")) return "UPDATE";
        if (methodName.contains("delete") || methodName.startsWith("delete")) return "DELETE";
        if (methodName.contains("create")) return "CREATE";
        if (methodName.contains("add")) return "UPDATE";
        if (methodName.contains("remove")) return "DELETE";
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
