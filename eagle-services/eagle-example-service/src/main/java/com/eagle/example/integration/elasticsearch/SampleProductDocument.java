package com.eagle.example.integration.elasticsearch;

import com.eagle.es.base.EagleDocument;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

/**
 * Elasticsearch Starter 验证：商品搜索文档。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "sample_product")
@ConditionalOnProperty(prefix = "eagle.elasticsearch", name = "enabled", havingValue = "true")
public class SampleProductDocument extends EagleDocument {

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Boolean)
    private Boolean enabled;
}
