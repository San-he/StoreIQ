package com.storeiq.controller;

import com.storeiq.common.Result;
import com.storeiq.dto.AiChatRequest;
import com.storeiq.entity.*;
import com.storeiq.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 分析控制器
 * 当前版本：基于规则的数据分析（预留 AI API 接入点）
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired private SaleRecordMapper saleMapper;
    @Autowired private ProductMapper productMapper;
    @Autowired private InventoryMapper inventoryMapper;
    @Autowired private StoreMapper storeMapper;
    @Autowired private CategoryMapper categoryMapper;

    /**
     * AI 对话接口
     * TODO: 接入真实 AI API（如 OpenAI / Claude / 通义千问）
     */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody AiChatRequest req) {
        String question = req.getQuestion() != null ? req.getQuestion().toLowerCase() : "";

        // 获取系统数据上下文
        String context = buildDataContext();

        // 基于规则匹配的简单回复（后续替换为 AI API 调用）
        String answer;
        if (question.contains("热销") || question.contains("畅销") || question.contains("卖得最好")) {
            answer = analyzeHotProducts();
        } else if (question.contains("库存") || question.contains("缺货") || question.contains("补货")) {
            answer = analyzeRestock();
        } else if (question.contains("利润") || question.contains("赚钱")) {
            answer = analyzeProfit();
        } else if (question.contains("趋势") || question.contains("预测") || question.contains("下一季度")) {
            answer = analyzeTrend();
        } else if (question.contains("店铺") && (question.contains("最好") || question.contains("排名"))) {
            answer = analyzeStoreRanking();
        } else {
            answer = "我是 StoreIQ AI 分析助手。\n\n当前系统数据概览：\n" + context + "\n您可以问我：\n" +
                    "• 哪些商品最畅销？\n• 哪些商品需要补货？\n• 本月利润分析\n" +
                    "• 下一季度趋势预测\n• 各店铺业绩排名";
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("question", req.getQuestion());
        resp.put("answer", answer);
        resp.put("context", context);
        return Result.success(resp);
    }

    /** 自动趋势分析 */
    @GetMapping("/auto/trend")
    public Result<?> autoTrend() {
        Map<String, Object> data = new HashMap<>();
        data.put("analysis", analyzeTrend());
        data.put("generateTime", LocalDateTime.now().toString());
        return Result.success(data);
    }

    /** 自动热销商品 */
    @GetMapping("/auto/hot-products")
    public Result<?> autoHotProducts() {
        Map<String, Object> data = new HashMap<>();
        data.put("analysis", analyzeHotProducts());
        data.put("generateTime", LocalDateTime.now().toString());
        return Result.success(data);
    }

    /** 自动补货建议 */
    @GetMapping("/auto/restock")
    public Result<?> autoRestock() {
        List<Map<String, Object>> suggestions = analyzeRestockList();
        Map<String, Object> data = new HashMap<>();
        data.put("analysis", analyzeRestock());
        data.put("suggestions", suggestions);
        data.put("generateTime", LocalDateTime.now().toString());
        return Result.success(data);
    }

    /** 自动利润优化 */
    @GetMapping("/auto/profit-optimization")
    public Result<?> autoProfitOptimization() {
        Map<String, Object> data = new HashMap<>();
        data.put("analysis", analyzeProfit());
        data.put("generateTime", LocalDateTime.now().toString());
        return Result.success(data);
    }

    // ========== 私有分析方法 ==========

    private String buildDataContext() {
        long storeCount = storeMapper.selectCount(null);
        long productCount = productMapper.selectCount(null);
        List<SaleRecord> allSales = saleMapper.selectList(null);
        BigDecimal totalSales = allSales.stream().map(SaleRecord::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = allSales.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("系统共管理 %d 家店铺、%d 种商品。累计销售额 ¥%s，累计利润 ¥%s。",
                storeCount, productCount, formatMoney(totalSales), formatMoney(totalProfit));
    }

    private String analyzeHotProducts() {
        List<SaleRecord> allSales = saleMapper.selectList(null);
        Map<Long, Integer> productSales = new HashMap<>();
        Map<Long, BigDecimal> productRevenue = new HashMap<>();

        for (SaleRecord s : allSales) {
            productSales.merge(s.getProductId(), s.getQuantity(), Integer::sum);
            productRevenue.merge(s.getProductId(), s.getTotalAmount(), BigDecimal::add);
        }

        // 按销量排序 Top 5
        List<Map.Entry<Long, Integer>> sorted = productSales.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5).collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("📊 热销商品 TOP 5：\n\n");
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sorted) {
            Product p = productMapper.selectById(entry.getKey());
            if (p != null) {
                sb.append(String.format("%d. %s — 销量 %d 台，销售额 ¥%s\n",
                        rank++, p.getName(), entry.getValue(),
                        formatMoney(productRevenue.get(entry.getKey()))));
            }
        }
        return sb.toString();
    }

    private String analyzeRestock() {
        List<Map<String, Object>> suggestions = analyzeRestockList();
        if (suggestions.isEmpty()) {
            return "✅ 当前各店铺库存充足，无需补货。";
        }
        StringBuilder sb = new StringBuilder("📦 补货建议：\n\n");
        for (Map<String, Object> s : suggestions) {
            sb.append(String.format("• %s — %s，当前库存 %d，建议补货 %d 件\n",
                    s.get("storeName"), s.get("productName"),
                    s.get("currentStock"), s.get("suggestQuantity")));
        }
        return sb.toString();
    }

    private List<Map<String, Object>> analyzeRestockList() {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        List<Inventory> allInv = inventoryMapper.selectList(null);
        List<SaleRecord> allSales = saleMapper.selectList(null);

        for (Inventory inv : allInv) {
            Product p = productMapper.selectById(inv.getProductId());
            if (p == null) continue;

            // 低于安全库存
            if (inv.getQuantity() <= p.getSafetyStock()) {
                // 计算月均销量
                long monthlyQty = allSales.stream()
                        .filter(s -> s.getProductId().equals(inv.getProductId())
                                && s.getStoreId().equals(inv.getStoreId()))
                        .mapToInt(SaleRecord::getQuantity).sum() / 6; // 粗略估计

                int suggestQty = Math.max(p.getSafetyStock() * 2, (int) monthlyQty * 2) - inv.getQuantity();
                if (suggestQty <= 0) suggestQty = p.getSafetyStock();

                Map<String, Object> item = new HashMap<>();
                item.put("storeId", inv.getStoreId());
                Store store = storeMapper.selectById(inv.getStoreId());
                item.put("storeName", store != null ? store.getName() : "-");
                item.put("productId", inv.getProductId());
                item.put("productName", p.getName());
                item.put("currentStock", inv.getQuantity());
                item.put("safetyStock", p.getSafetyStock());
                item.put("suggestQuantity", suggestQty);
                suggestions.add(item);
            }
        }
        return suggestions;
    }

    private String analyzeProfit() {
        List<SaleRecord> allSales = saleMapper.selectList(null);
        Map<Long, BigDecimal> productProfit = new HashMap<>();

        for (SaleRecord s : allSales) {
            productProfit.merge(s.getProductId(), s.getProfit(), BigDecimal::add);
        }

        // 按利润排序 Top 5
        List<Map.Entry<Long, BigDecimal>> sorted = productProfit.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(5).collect(Collectors.toList());

        BigDecimal totalProfit = allSales.stream().map(SaleRecord::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder("💰 利润分析报告：\n\n");
        sb.append(String.format("累计总利润：¥%s\n\n", formatMoney(totalProfit)));
        sb.append("利润贡献 TOP 5：\n");
        int rank = 1;
        for (Map.Entry<Long, BigDecimal> entry : sorted) {
            Product p = productMapper.selectById(entry.getKey());
            if (p != null) {
                sb.append(String.format("%d. %s — 贡献利润 ¥%s\n",
                        rank++, p.getName(), formatMoney(entry.getValue())));
            }
        }
        sb.append("\n💡 建议：继续保持热销商品的库存稳定，优化低利润商品的进货成本。");
        return sb.toString();
    }

    private String analyzeTrend() {
        // 简单趋势分析
        List<SaleRecord> allSales = saleMapper.selectList(null);

        // 按月分组
        Map<String, BigDecimal> monthlyAmount = new LinkedHashMap<>();
        for (SaleRecord s : allSales) {
            String month = s.getCreateTime().getYear() + "-" + String.format("%02d", s.getCreateTime().getMonthValue());
            monthlyAmount.merge(month, s.getTotalAmount(), BigDecimal::add);
        }

        StringBuilder sb = new StringBuilder("📈 趋势分析：\n\n");
        sb.append("近半年月度销售趋势：\n");

        // 取最近6个月
        List<String> months = new ArrayList<>(monthlyAmount.keySet());
        int start = Math.max(0, months.size() - 6);
        for (int i = start; i < months.size(); i++) {
            sb.append(String.format("• %s: ¥%s\n", months.get(i), formatMoney(monthlyAmount.get(months.get(i)))));
        }

        // 简单预测
        if (months.size() >= 3) {
            BigDecimal last3Avg = BigDecimal.ZERO;
            int count = 0;
            for (int i = Math.max(0, months.size() - 3); i < months.size(); i++) {
                last3Avg = last3Avg.add(monthlyAmount.get(months.get(i)));
                count++;
            }
            if (count > 0) {
                last3Avg = last3Avg.divide(new BigDecimal(count), 2, java.math.RoundingMode.HALF_UP);
                sb.append(String.format("\n🔮 基于近3个月数据，预计下月销售额约 ¥%s", formatMoney(last3Avg)));
                sb.append("\n💡 建议：提前备货高销量品类，关注季节性产品的销售窗口期。");
            }
        }
        return sb.toString();
    }

    private String analyzeStoreRanking() {
        List<Store> stores = storeMapper.selectList(null);
        List<SaleRecord> allSales = saleMapper.selectList(null);
        Map<Long, BigDecimal> storeSales = new HashMap<>();

        for (SaleRecord s : allSales) {
            storeSales.merge(s.getStoreId(), s.getTotalAmount(), BigDecimal::add);
        }

        List<Map.Entry<Long, BigDecimal>> sorted = storeSales.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("🏪 店铺业绩排名：\n\n");
        int rank = 1;
        for (Map.Entry<Long, BigDecimal> entry : sorted) {
            Store store = storeMapper.selectById(entry.getKey());
            if (store != null) {
                sb.append(String.format("%d. %s（%s）— 累计销售 ¥%s\n",
                        rank++, store.getName(), store.getRegion(), formatMoney(entry.getValue())));
            }
        }
        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.2f", amount);
    }
}
