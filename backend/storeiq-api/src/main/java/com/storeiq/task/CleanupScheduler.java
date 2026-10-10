package com.storeiq.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storeiq.entity.*;
import com.storeiq.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据清理定时调度器
 * 02:00 扫描 → 创建通知
 * 02:30 若通知已读 → 执行清理
 */
@Component
public class CleanupScheduler {

    @Autowired private CleanupRuleMapper cleanupRuleMapper;
    @Autowired private NotificationMapper notificationMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private PurchaseRecordMapper purchaseRecordMapper;
    @Autowired private SaleRecordMapper saleRecordMapper;
    @Autowired private ProductMapper productMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 每日 02:00 — 扫描过期数据，创建通知 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scanAndNotify() {
        doScanAndNotify();
    }

    /** 每日 02:30 — 若通知已读则执行清理 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void executeCleanupIfAccepted() {
        doCleanupIfAccepted();
    }

    /** 公开方法，供手动触发 — 跳过已读检查直接执行 */
    public void executeNow() {
        doScanAndNotify();
        // 手动触发时跳过确认，直接清理
        doCleanup();
    }

    /** 手动调用的清理，不检查 isRead */
    private void doCleanup() {
        doCleanupInternal();
    }

    /** 定时调用的清理，需要检查 isRead */
    private void doCleanupIfAccepted() {
        Notification note = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType, "data_cleanup")
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 1"));
        if (note == null || note.getIsRead() == 0) return;
        doCleanupInternal();
    }

    // === 内部逻辑 ===

    private void doScanAndNotify() {
        List<CleanupRule> rules = cleanupRuleMapper.selectList(null);
        List<Map<String, Object>> tables = new ArrayList<>();
        int totalRecords = 0;

        for (CleanupRule rule : rules) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(rule.getRetentionDays());
            int count = countOldRecords(rule.getTableType(), cutoff, rule);
            if (count <= 0) continue;

            // 获取最早记录日期
            String oldestDate = getOldestDate(rule.getTableType(), cutoff);
            // 按月分布
            List<Map<String, Object>> monthly = getMonthlyBreakdown(rule.getTableType(), cutoff, rule);

            Map<String, Object> info = new HashMap<>();
            info.put("tableType", rule.getTableType());
            info.put("tableLabel", getTableLabel(rule.getTableType()));
            info.put("retentionDays", rule.getRetentionDays());
            info.put("totalRecords", count);
            info.put("oldestDate", oldestDate);
            info.put("monthlyBreakdown", monthly);
            tables.add(info);
            totalRecords += count;
        }

        // 构建 content JSON
        Map<String, Object> content = new HashMap<>();
        content.put("tables", tables);
        content.put("totalRecords", totalRecords);
        content.put("scanTime", LocalDateTime.now().format(DTF));

        String contentJson = new com.alibaba.fastjson.JSONObject(content).toJSONString();
        String todayStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd"));

        if (totalRecords == 0) {
            // 没有需要清理的数据，不创建通知
            return;
        }

        // 检查是否已有未读的 data_cleanup 通知
        Notification existing = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getType, "data_cleanup")
                        .eq(Notification::getIsRead, 0)
                        .orderByDesc(Notification::getCreateTime)
                        .last("LIMIT 1"));

        if (existing != null) {
            // 更新现有通知
            existing.setTitle("数据清理提醒 (" + todayStr + ")");
            existing.setContent(contentJson);
            existing.setCreateTime(LocalDateTime.now());
            notificationMapper.updateById(existing);
        } else {
            // 创建新通知
            Notification note = new Notification();
            note.setTitle("数据清理提醒 (" + todayStr + ")");
            note.setContent(contentJson);
            note.setType("data_cleanup");
            note.setLevel("warning");
            note.setIsRead(0);
            note.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(note);
        }
    }

    private void doCleanupInternal() {
        List<CleanupRule> rules = cleanupRuleMapper.selectList(null);
        Map<String, Integer> deletedMap = new LinkedHashMap<>();

        for (CleanupRule rule : rules) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(rule.getRetentionDays());
            int deleted = deleteOldRecords(rule.getTableType(), cutoff, rule);
            deletedMap.put(rule.getTableType(), deleted);
        }

        // 插入完成通知
        Map<String, Object> result = new HashMap<>();
        result.put("tables", deletedMap);
        result.put("executedAt", LocalDateTime.now().format(DTF));

        Notification note = new Notification();
        note.setTitle("数据清理完成");
        note.setContent(new com.alibaba.fastjson.JSONObject(result).toJSONString());
        note.setType("data_cleanup");
        note.setLevel("success");
        note.setIsRead(1);
        note.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(note);
    }

    // === 统计/删除工具方法 ===

    private int countOldRecords(String tableType, LocalDateTime cutoff, CleanupRule rule) {
        if ("operation_log".equals(tableType)) {
            return Math.toIntExact(operationLogMapper.selectCount(
                    new LambdaQueryWrapper<OperationLog>().lt(OperationLog::getCreateTime, cutoff)));
        } else if ("purchase_record".equals(tableType)) {
            return Math.toIntExact(purchaseRecordMapper.selectCount(
                    buildPurchaseWrapper(cutoff, rule)));
        } else if ("sale_record".equals(tableType)) {
            return Math.toIntExact(saleRecordMapper.selectCount(
                    buildSaleWrapper(cutoff, rule)));
        }
        return 0;
    }

    private String getOldestDate(String tableType, LocalDateTime cutoff) {
        try {
            if ("operation_log".equals(tableType)) {
                List<OperationLog> list = operationLogMapper.selectList(
                        new LambdaQueryWrapper<OperationLog>().lt(OperationLog::getCreateTime, cutoff)
                                .orderByAsc(OperationLog::getCreateTime).last("LIMIT 1"));
                if (!list.isEmpty() && list.get(0).getCreateTime() != null)
                    return list.get(0).getCreateTime().format(DTF);
            } else if ("purchase_record".equals(tableType)) {
                List<PurchaseRecord> list = purchaseRecordMapper.selectList(
                        buildPurchaseWrapper(cutoff, null)
                                .orderByAsc(PurchaseRecord::getCreateTime).last("LIMIT 1"));
                if (!list.isEmpty() && list.get(0).getCreateTime() != null)
                    return list.get(0).getCreateTime().format(DTF);
            } else if ("sale_record".equals(tableType)) {
                List<SaleRecord> list = saleRecordMapper.selectList(
                        buildSaleWrapper(cutoff, null)
                                .orderByAsc(SaleRecord::getCreateTime).last("LIMIT 1"));
                if (!list.isEmpty() && list.get(0).getCreateTime() != null)
                    return list.get(0).getCreateTime().format(DTF);
            }
        } catch (Exception ignored) {}
        return "-";
    }

    private List<Map<String, Object>> getMonthlyBreakdown(String tableType, LocalDateTime cutoff, CleanupRule rule) {
        // 简化实现：根据最早日期到截止日期之间按月分组
        // 由于项目 Mapper 无按月分组 SQL，这里使用 Java 端简单聚合
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if ("operation_log".equals(tableType)) {
                List<OperationLog> logs = operationLogMapper.selectList(
                        new LambdaQueryWrapper<OperationLog>().lt(OperationLog::getCreateTime, cutoff));
                result = aggregateByMonth(logs.stream().map(l -> l.getCreateTime()).collect(Collectors.toList()));
            } else if ("purchase_record".equals(tableType)) {
                List<PurchaseRecord> records = purchaseRecordMapper.selectList(
                        buildPurchaseWrapper(cutoff, rule));
                result = aggregateByMonth(records.stream().map(r -> r.getCreateTime()).collect(Collectors.toList()));
            } else if ("sale_record".equals(tableType)) {
                List<SaleRecord> records = saleRecordMapper.selectList(
                        buildSaleWrapper(cutoff, rule));
                result = aggregateByMonth(records.stream().map(r -> r.getCreateTime()).collect(Collectors.toList()));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private List<Map<String, Object>> aggregateByMonth(List<LocalDateTime> times) {
        Map<String, Long> grouped = new TreeMap<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (LocalDateTime t : times) {
            if (t == null) continue;
            String month = t.format(monthFmt);
            grouped.merge(month, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : grouped.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("month", e.getKey());
            m.put("count", e.getValue());
            result.add(m);
        }
        return result;
    }

    private int deleteOldRecords(String tableType, LocalDateTime cutoff, CleanupRule rule) {
        // 批处理删除，每次至多1000条，避免锁表
        int totalDeleted = 0;
        int batchSize = 1000;
        while (true) {
            int deleted;
            if ("operation_log".equals(tableType)) {
                deleted = operationLogMapper.delete(
                        new LambdaQueryWrapper<OperationLog>().lt(OperationLog::getCreateTime, cutoff).last("LIMIT " + batchSize));
            } else if ("purchase_record".equals(tableType)) {
                deleted = purchaseRecordMapper.delete(
                        buildPurchaseWrapper(cutoff, rule).last("LIMIT " + batchSize));
            } else if ("sale_record".equals(tableType)) {
                deleted = saleRecordMapper.delete(
                        buildSaleWrapper(cutoff, rule).last("LIMIT " + batchSize));
            } else {
                break;
            }
            if (deleted == 0) break;
            totalDeleted += deleted;
        }
        return totalDeleted;
    }

    private LambdaQueryWrapper<PurchaseRecord> buildPurchaseWrapper(LocalDateTime cutoff, CleanupRule rule) {
        LambdaQueryWrapper<PurchaseRecord> w = new LambdaQueryWrapper<>();
        w.lt(PurchaseRecord::getCreateTime, cutoff);
        if (rule != null && rule.getStoreIds() != null && !rule.getStoreIds().trim().isEmpty()) {
            List<Long> ids = parseStoreIds(rule.getStoreIds());
            if (!ids.isEmpty()) w.notIn(PurchaseRecord::getStoreId, ids);
        }
        return w;
    }

    private LambdaQueryWrapper<SaleRecord> buildSaleWrapper(LocalDateTime cutoff, CleanupRule rule) {
        LambdaQueryWrapper<SaleRecord> w = new LambdaQueryWrapper<>();
        w.lt(SaleRecord::getCreateTime, cutoff);
        if (rule != null && rule.getStoreIds() != null && !rule.getStoreIds().trim().isEmpty()) {
            List<Long> ids = parseStoreIds(rule.getStoreIds());
            if (!ids.isEmpty()) w.notIn(SaleRecord::getStoreId, ids);
        }
        return w;
    }

    /** 解析逗号分隔的店铺 ID */
    private List<Long> parseStoreIds(String storeIds) {
        return Arrays.stream(storeIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).collect(Collectors.toList());
    }

    private String getTableLabel(String tableType) {
        switch (tableType) {
            case "operation_log": return "操作日志";
            case "purchase_record": return "进货记录";
            case "sale_record": return "销售记录";
            default: return tableType;
        }
    }
}
