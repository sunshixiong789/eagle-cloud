package com.eagle.ai.config;

import com.eagle.ai.properties.AiProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Eagle AI Embedding / RAG 自动配置。
 *
 * <p>当 {@link EmbeddingModel} 在类路径且 Bean 可用时自动激活，注册：
 * <ul>
 *   <li>{@link VectorStore}（{@link SimpleVectorStore} 回退实现）—— 未配置真实向量数据库时
 *       提供内存向量检索，便于开发和单元测试</li>
 * </ul>
 *
 * <p>生产环境应引入真实的向量存储 starter 来替换内存实现，例如：
 * <ul>
 *   <li>PgVector：{@code spring-ai-starter-vector-store-pgvector}</li>
 *   <li>Redis：{@code spring-ai-starter-vector-store-redis}</li>
 *   <li>Elasticsearch：{@code spring-ai-starter-vector-store-elasticsearch}</li>
 * </ul>
 * 这些 starter 会自动注册各自的 {@link VectorStore} Bean，覆盖此处的 {@link SimpleVectorStore}。
 *
 * <h2>RAG 使用示例</h2>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class RagService {
 *
 *     private final ChatClient chatClient;
 *     private final VectorStore vectorStore;
 *     private final AiProperties aiProperties;
 *
 *     // 向量化并存储文档
 *     public void ingestDocuments(List<Document> docs) {
 *         vectorStore.add(docs);
 *     }
 *
 *     // RAG 检索问答
 *     public String query(String question) {
 *         return chatClient.prompt()
 *             .advisors(new QuestionAnswerAdvisor(vectorStore,
 *                 SearchRequest.defaults()
 *                     .withTopK(aiProperties.getEmbedding().getDefaultTopK())
 *                     .withSimilarityThreshold(aiProperties.getEmbedding().getDefaultSimilarityThreshold())))
 *             .user(question)
 *             .call()
 *             .content();
 *     }
 * }
 * }</pre>
 */
@AutoConfiguration(after = EagleAiAutoConfiguration.class)
@ConditionalOnClass({EmbeddingModel.class, VectorStore.class, SimpleVectorStore.class})
@ConditionalOnProperty(name = "eagle.ai.embedding.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AiProperties.class)
public class EagleAiEmbeddingAutoConfiguration {

    /**
     * 内存向量存储回退实现。
     *
     * <p>仅在 {@link EmbeddingModel} Bean 可用且未配置其他 {@link VectorStore} 时注册。
     * 数据存储在 JVM 堆内，重启后丢失，仅适合开发调试。
     */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnBean(EmbeddingModel.class)
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
