package com.storeiq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storeiq.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<ProductCategory> {}
