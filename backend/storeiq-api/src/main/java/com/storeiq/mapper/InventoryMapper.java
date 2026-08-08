package com.storeiq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storeiq.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /** 按店铺聚合库存总量 */
    @Select("<script>SELECT store_id, SUM(quantity) AS total FROM inventory WHERE store_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> GROUP BY store_id</script>")
    List<Map<String, Object>> sumQuantityByStoreIds(@Param("ids") Set<Long> storeIds);

    /** 按商品聚合库存总量 */
    @Select("<script>SELECT product_id, SUM(quantity) AS total FROM inventory WHERE product_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> GROUP BY product_id</script>")
    List<Map<String, Object>> sumQuantityByProductIds(@Param("ids") Set<Long> productIds);
}
