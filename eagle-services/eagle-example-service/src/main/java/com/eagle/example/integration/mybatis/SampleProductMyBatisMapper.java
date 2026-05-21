package com.eagle.example.integration.mybatis;

import com.eagle.example.sample.domain.model.SampleProduct;
import com.eagle.mybatis.base.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus Starter 验证 Mapper。
 */
@Mapper
public interface SampleProductMyBatisMapper extends BaseMapperPlus<SampleProduct> {

    @Select("SELECT * FROM sample_product WHERE enabled = 1 LIMIT 10")
    List<SampleProduct> selectEnabledTop10();
}
