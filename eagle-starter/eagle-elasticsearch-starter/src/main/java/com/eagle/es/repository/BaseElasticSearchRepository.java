package com.eagle.es.repository;

import com.eagle.es.base.EagleDocument;
import com.eagle.es.model.EsPageResult;
import com.eagle.es.util.EsHighlightUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Elasticsearch 通用 Repository 基类。
 *
 * <p>封装 {@link ElasticsearchOperations} 的常用操作，为子类提供开箱即用的 CRUD、
 * 分页搜索（含高亮）和批量写入能力，避免在每个业务 Repository 中重复实现相同逻辑。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Repository
 * public class ProductRepository extends BaseElasticSearchRepository<ProductDocument> {
 *
 *     public ProductRepository(ElasticsearchOperations operations) {
 *         super(operations, ProductDocument.class);
 *     }
 *
 *     public EsPageResult<ProductDocument> searchProducts(String keyword, int page, int size) {
 *         NativeQuery query = EsQueryBuilder.of()
 *             .multiMatch(keyword, "title", "description")
 *             .highlight("title", "description")
 *             .page(page, size)
 *             .build();
 *         return search(query);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 文档类型，必须继承 {@link EagleDocument}
 * @author eagle
 */
@RequiredArgsConstructor
public abstract class BaseElasticSearchRepository<T extends EagleDocument> {

    /** 批量写入的默认分批大小，避免单次 bulk 请求过大 */
    private static final int DEFAULT_BATCH_SIZE = 500;

    /** Spring Data Elasticsearch 操作模板 */
    protected final ElasticsearchOperations operations;

    /** 文档实体类型，用于反射和 ES 索引映射 */
    protected final Class<T> entityClass;

    /**
     * 保存或更新单个文档。
     *
     * <p>若文档 ID 已存在，则执行更新（完整替换）；不存在则插入。
     *
     * @param document 待保存的文档对象；不能为 {@code null}
     * @return 保存后的文档对象（包含 ES 回填的元数据）
     */
    public T save(T document) {
        return operations.save(document);
    }

    /**
     * 批量保存文档，自动按 {@value #DEFAULT_BATCH_SIZE} 分批提交。
     *
     * <p>使用 Spring Data ES 的批量 save，避免超出 ES bulk 请求体大小限制。
     * 列表为空时直接返回，不执行任何操作。
     *
     * @param documents 待批量保存的文档列表；不能为 {@code null}
     */
    public void saveAll(List<T> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        int total = documents.size();
        for (int start = 0; start < total; start += DEFAULT_BATCH_SIZE) {
            int end = Math.min(start + DEFAULT_BATCH_SIZE, total);
            List<T> batch = documents.subList(start, end);
            operations.save(batch);
        }
    }

    /**
     * 根据文档 ID 查询单个文档。
     *
     * @param id 文档 ID；不能为 {@code null} 或空字符串
     * @return 包含文档对象的 {@link Optional}，不存在时返回 {@link Optional#empty()}
     */
    public Optional<T> findById(String id) {
        T result = operations.get(id, entityClass);
        return Optional.ofNullable(result);
    }

    /**
     * 根据文档 ID 删除文档。
     *
     * <p>若 ID 对应的文档不存在，ES 不会报错（幂等操作）。
     *
     * @param id 文档 ID；不能为 {@code null} 或空字符串
     */
    public void deleteById(String id) {
        operations.delete(id, entityClass);
    }

    /**
     * 执行查询并返回分页结果，自动处理高亮字段回写。
     *
     * <p>搜索命中的每条记录均会调用 {@link EsHighlightUtil#applyHighlight(SearchHit)}，
     * 将 ES 返回的高亮片段（{@code <em>...</em>} 标签）反射回写至文档对应字段。
     *
     * @param query 构建好的 {@link NativeQuery}，包含分页、排序、高亮等参数
     * @return 包含文档列表和分页信息的 {@link EsPageResult}
     */
    public EsPageResult<T> search(NativeQuery query) {
        SearchHits<T> searchHits = operations.search(query, entityClass);

        List<T> content = new ArrayList<>(searchHits.getSearchHits().size());
        for (SearchHit<T> hit : searchHits.getSearchHits()) {
            // 高亮字段回写至文档对象
            EsHighlightUtil.applyHighlight(hit);
            content.add(hit.getContent());
        }

        long total = searchHits.getTotalHits();
        int from = query.getFrom() != null ? query.getFrom() : 0;
        int size = query.getMaxResults() != null ? query.getMaxResults() : 20;
        // page 从 1 开始
        int page = size > 0 ? (from / size) + 1 : 1;

        return new EsPageResult<>(content, total, page, size);
    }

    /**
     * 统计符合查询条件的文档总数。
     *
     * <p>仅执行 count 查询，不返回文档内容，性能优于 {@link #search(NativeQuery)} 后取 total。
     *
     * @param query 查询条件（分页、排序、高亮参数将被忽略）
     * @return 匹配文档的总数
     */
    public long count(NativeQuery query) {
        return operations.count(query, entityClass);
    }
}
