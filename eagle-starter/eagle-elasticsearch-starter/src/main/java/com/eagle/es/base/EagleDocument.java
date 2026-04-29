package com.eagle.es.base;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * Elasticsearch 文档基类。
 *
 * <p>所有业务 ES Document 均应继承此类，以获得统一的文档 ID 及审计时间字段。
 * 注意：此类不依赖 JPA，仅使用 Spring Data 通用注解。
 *
 * @author eagle
 */
@Getter
@NoArgsConstructor
public abstract class EagleDocument {

    /** 文档 ID */
    @Id
    private String id;

    /** 创建时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;
}
