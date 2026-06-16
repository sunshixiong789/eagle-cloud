package com.eagle.rocketmq.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RocketMQ 配置属性。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.rocketmq")
public class RocketMqProperties {

    /**
     * 接入点地址（5.x gRPC Proxy），如 {@code localhost:8081}。
     */
    private String endpoints = "localhost:8081";

    /**
     * NameServer 地址（4.x classic remoting),仅供 starter 启动期幂等建 topic 用。
     *
     * <p>5.x 业务流量(发消息/拉消息)走 {@link #endpoints} Proxy gRPC,但 admin createTopic
     * 必须走 NameServer remoting(rocketmq-client-java 5.x 未暴露 admin API)。
     * 容器部署默认 {@code rocketmq-namesrv:9876};本地开发可改为 {@code localhost:9876}。
     */
    private String namesrvAddr = "rocketmq-namesrv:9876";

    /**
     * 兼容历史配置键 {@code eagle.rocketmq.name-server}。
     *
     * <p>标准键仍是 {@code eagle.rocketmq.namesrv-addr}；业务仓库曾使用
     * {@code name-server}，这里保留别名，避免 TopicAdmin 静默落回默认 NameServer。
     */
    public String getNameServer() {
        return namesrvAddr;
    }

    public void setNameServer(String nameServer) {
        this.namesrvAddr = nameServer;
    }

    /**
     * Topic admin 配置(启动期建 topic 行为)。
     */
    private TopicAdmin topicAdmin = new TopicAdmin();

    /**
     * 默认生产者组。
     */
    private String producerGroup = "eagle-producer-group";

    /**
     * 消费者组（未在监听器中显式覆盖时使用此默认值）。
     */
    private String consumerGroup = "eagle-consumer-group";

    /**
     * 默认 Topic 前缀，自动推导 Topic 时使用。
     */
    private String topicPrefix = "eagle-";

    /**
     * 客户端请求超时时间（毫秒）。
     * 适用于生产者发送和消费者拉取，默认 3000 ms。
     */
    private int requestTimeoutMillis = 3000;

    /**
     * 消息发送失败后的最大重试次数（同步发送）。
     * 默认 2 次，与 RocketMQ 客户端内置重试叠加时请注意幂等性。
     */
    private int maxAttempts = 2;

    /**
     * 是否启用 TLS 连接 broker。
     *
     * <p>RocketMQ 5.x {@link org.apache.rocketmq.client.apis.ClientConfigurationBuilder}
     * 默认 {@code sslEnabled = true},会导致 client 用 TLS + ALPN 协商连接 broker;
     * 若 broker 是明文部署(标准 docker-compose 部署),客户端启动时会抛
     * {@code Failed ALPN negotiation: Unable to find compatible protocol}。
     * 本项目默认部署形态是明文,因此默认 {@code false};生产 TLS 部署再显式打开。
     */
    private boolean sslEnabled = false;

    /**
     * 消费者配置。
     */
    private Consumer consumer = new Consumer();

    /**
     * 消费者细粒度配置。
     */
    @Data
    public static class Consumer {

        /**
         * 本地缓存消息条数上限。
         * 控制 PushConsumer 的消费速率，防止内存溢出，默认 1024。
         */
        private int maxCachedMessageCount = 1024;

        /**
         * 本地缓存消息总字节数上限（字节），默认 64 MB。
         * 与 {@link #maxCachedMessageCount} 同时生效，任意一个触发则暂停拉取。
         */
        private int maxCachedMessageSizeInBytes = 64 * 1024 * 1024;

        /**
         * 重试次数告警阈值。
         *
         * <p>消息投递次数达到此值时，调用 {@code onRetryAlert()} 触发告警。
         * RocketMQ 默认最大重试 16 次，超过后进入死信队列（DLQ）。
         * 默认告警阈值为 3，提前感知消费异常。
         */
        private int retryAlertThreshold = 3;

        /**
         * Consumer 启动期容错配置。
         */
        private StartupRetry startupRetry = new StartupRetry();
    }

    /**
     * Topic admin(启动期幂等建 topic)。
     *
     * <p>启用后,{@link com.eagle.rocketmq.listener.AbstractRocketMqListener} 在 build Consumer
     * 之前会主动通过 {@code DefaultMQAdminExt} 在所有 broker 上创建 topic(已存在则更新配置,幂等)。
     * 这样无需依赖 Producer 首发消息触发 {@code autoCreateTopicEnable},也无需运维预建。
     *
     * <p>生产环境通常关闭(topic 严格由运维预建),开发环境默认开启简化部署。
     */
    @Data
    public static class TopicAdmin {

        /** 是否启用启动期建 topic。默认 {@code true}。 */
        private boolean enabled = true;

        /** 集群名,需与 broker.conf 中 {@code brokerClusterName} 一致。默认 {@code DefaultCluster}。 */
        private String clusterName = "DefaultCluster";

        /** 读队列数。默认 4。 */
        private int readQueueNums = 4;

        /** 写队列数。默认 4。 */
        private int writeQueueNums = 4;

        /** 权限位:6=RW(读+写),4=R(只读),2=W(只写)。默认 6。 */
        private int perm = 6;

        /** admin client 建 topic 超时(毫秒)。默认 10000。 */
        private long timeoutMillis = 10_000L;
    }

    /**
     * Consumer 启动期容错策略。
     *
     * <p>开启后,{@code AbstractRocketMqListener.afterPropertiesSet()} 不再因 topic 路由不存在
     * (Producer 还没发过第一条消息)/ broker 暂不可达 / proxy 重启 等瞬态异常导致应用启动失败,
     * 改为后台调度异步重试。期间 Producer 一旦发出首条消息触发 {@code autoCreateTopicEnable},
     * Consumer 下次重试即可成功订阅,无需任何手工干预。
     *
     * <p>关闭后行为退回到 fail-fast(适合生产 topic 必须预创建的严格运维场景)。
     */
    @Data
    public static class StartupRetry {

        /**
         * 是否启用启动期后台重试。默认 {@code true}。
         */
        private boolean enabled = true;

        /**
         * 首次重试等待时长。默认 5 秒。
         */
        private Duration initialBackoff = Duration.ofSeconds(5);

        /**
         * 单次重试最长等待时长(指数退避上限)。默认 60 秒。
         */
        private Duration maxBackoff = Duration.ofSeconds(60);

        /**
         * 指数退避倍数。默认 2.0(5s → 10s → 20s → 40s → 60s → 60s ...)。
         */
        private double multiplier = 2.0;
    }
}
