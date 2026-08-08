package com.storeiq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storeiq.entity.SaleRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface SaleRecordMapper extends BaseMapper<SaleRecord> {

    /** 按店铺聚合销售额（只查需要的店铺） */
    @Select("<script>SELECT store_id, SUM(total_amount) AS total FROM sale_record WHERE store_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> GROUP BY store_id</script>")
    List<Map<String, Object>> sumAmountByStoreIds(@Param("ids") Set<Long> storeIds);

    /** 按商品聚合销售额和数量（支持店铺/分类筛选） */
    @Select("<script>SELECT product_id, SUM(quantity) AS qty, SUM(total_amount) AS amount FROM sale_record WHERE 1=1 <if test='storeId != null'> AND store_id = #{storeId}</if> GROUP BY product_id</script>")
    List<Map<String, Object>> sumAmountByProduct(@Param("storeId") Long storeId);

    /** 按商品聚合指定时段的销售额（用于趋势对比） */
    @Select("<script>SELECT product_id, SUM(total_amount) AS amount FROM sale_record WHERE create_time >= #{start} AND create_time &lt;= #{end} <if test='storeId != null'> AND store_id = #{storeId}</if> GROUP BY product_id</script>")
    List<Map<String, Object>> sumAmountByProductAndDate(@Param("storeId") Long storeId, @Param("start") String start, @Param("end") String end);

    /** 按月份+商品聚合销售数量（指定店铺+年份） */
    @Select("SELECT MONTH(sr.create_time) AS month, sr.product_id, p.name AS product_name, SUM(sr.quantity) AS quantity FROM sale_record sr LEFT JOIN product p ON sr.product_id = p.id WHERE sr.store_id = #{storeId} AND YEAR(sr.create_time) = #{year} GROUP BY MONTH(sr.create_time), sr.product_id, p.name ORDER BY month, sr.product_id")
    List<Map<String, Object>> sumQuantityByMonth(@Param("storeId") Long storeId, @Param("year") int year);

    /** 某店铺有销售记录的年份列表 */
    @Select("SELECT DISTINCT YEAR(create_time) AS year FROM sale_record WHERE store_id = #{storeId} ORDER BY year DESC")
    List<Map<String, Object>> distinctSaleYears(@Param("storeId") Long storeId);
}
