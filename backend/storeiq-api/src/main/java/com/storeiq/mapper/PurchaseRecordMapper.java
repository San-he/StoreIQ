package com.storeiq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storeiq.entity.PurchaseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface PurchaseRecordMapper extends BaseMapper<PurchaseRecord> {

    /** 按商品聚合最早/最晚进货日期（指定店铺+商品列表） */
    @Select("<script>SELECT product_id, MIN(create_time) AS earliest, MAX(create_time) AS latest FROM purchase_record WHERE store_id = #{storeId} AND product_id IN <foreach item='id' collection='productIds' open='(' separator=',' close=')'>#{id}</foreach> GROUP BY product_id</script>")
    List<Map<String, Object>> minMaxPurchaseDateByProductIds(@Param("storeId") Long storeId, @Param("productIds") Set<Long> productIds);
}
