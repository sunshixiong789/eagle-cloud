package com.eagle.example.integration.elasticsearch;

import com.eagle.es.repository.BaseElasticSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;

/**
 * ES 商品搜索仓储。
 */
@Repository
@ConditionalOnProperty(prefix = "eagle.elasticsearch", name = "enabled", havingValue = "true")
public class SampleProductEsRepository extends BaseElasticSearchRepository<SampleProductDocument> {

    public SampleProductEsRepository(ElasticsearchOperations operations) {
        super(operations, SampleProductDocument.class);
    }
}
