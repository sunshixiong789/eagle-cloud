package com.eagle.rocketmq.admin;

import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.remoting.protocol.body.ClusterInfo;
import org.apache.rocketmq.remoting.protocol.route.BrokerData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RocketMQ topic admin 客户端封装。
 *
 * <p>用 {@code rocketmq-tools}(4.x classic remoting)连接 NameServer 调 admin API
 * 在所有 broker 上幂等创建 topic。供 {@link com.eagle.rocketmq.listener.AbstractRocketMqListener}
 * 在启动期使用,避免依赖 Producer 首发消息触发 {@code autoCreateTopicEnable},也无需运维预建。
 *
 * <p>{@code rocketmq-client-java}(5.x gRPC)未暴露 admin createTopic API,
 * 因此用 4.x remoting 协议补这个能力。两套客户端独立工作,与 5.x 业务流量互不影响。
 *
 * <p>本类是线程安全的单例,实现 {@link InitializingBean} / {@link DisposableBean} 管理底层
 * {@code DefaultMQAdminExt} 生命周期。
 *
 * @author eagle
 */
@Slf4j
public class RocketMqTopicAdmin implements InitializingBean, DisposableBean {

    private final RocketMqProperties properties;
    private final DefaultMQAdminExt adminExt;
    private final Set<String> ensuredTopics = ConcurrentHashMap.newKeySet();

    public RocketMqTopicAdmin(RocketMqProperties properties) {
        this.properties = properties;
        // adminExt group 必须全局唯一,加随机后缀避免多实例冲突
        this.adminExt = new DefaultMQAdminExt("eagle-topic-admin-" + UUID.randomUUID());
        this.adminExt.setNamesrvAddr(properties.getNamesrvAddr());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RocketMqProperties.TopicAdmin cfg = properties.getTopicAdmin();
        adminExt.setVipChannelEnabled(false);
        adminExt.start();
        log.info("RocketMQ topic admin started, namesrv: {}, cluster: {}, queueNums: r={}/w={}, perm: {}",
                properties.getNamesrvAddr(), cfg.getClusterName(),
                cfg.getReadQueueNums(), cfg.getWriteQueueNums(), cfg.getPerm());
    }

    @Override
    public void destroy() {
        adminExt.shutdown();
        log.info("RocketMQ topic admin shut down");
    }

    /**
     * 在所有 broker 上幂等创建/更新 topic 配置。
     *
     * <p>已 ensured 过的 topic 进程内不再重复请求(轻量本地去重,broker 端本身也支持幂等覆盖)。
     *
     * @param topic topic 名称
     * @throws RocketMqTopicAdminException 创建失败(NameServer 不可达/无 broker/权限不足等)
     */
    public void ensureTopic(String topic) {
        if (ensuredTopics.contains(topic)) {
            return;
        }
        RocketMqProperties.TopicAdmin cfg = properties.getTopicAdmin();
        TopicConfig topicConfig = new TopicConfig(topic);
        topicConfig.setReadQueueNums(cfg.getReadQueueNums());
        topicConfig.setWriteQueueNums(cfg.getWriteQueueNums());
        topicConfig.setPerm(cfg.getPerm());

        try {
            Set<String> brokerAddrs = resolveBrokerAddrs(cfg.getClusterName());
            if (brokerAddrs.isEmpty()) {
                throw new RocketMqTopicAdminException(
                        "no broker available in cluster: " + cfg.getClusterName());
            }
            for (String brokerAddr : brokerAddrs) {
                adminExt.createAndUpdateTopicConfig(brokerAddr, topicConfig);
            }
            ensuredTopics.add(topic);
            log.info("RocketMQ topic ensured: {} (brokers: {})", topic, brokerAddrs.size());
        } catch (RocketMqTopicAdminException e) {
            throw e;
        } catch (Exception e) {
            throw new RocketMqTopicAdminException(
                    "failed to ensure topic: " + topic + ", reason: " + e.getMessage(), e);
        }
    }

    /** 通过 NameServer 拿当前集群里所有 broker master 的地址。 */
    private Set<String> resolveBrokerAddrs(String clusterName) throws Exception {
        ClusterInfo clusterInfo = adminExt.examineBrokerClusterInfo();
        Set<String> result = new HashSet<>();
        Set<String> brokerNames = clusterInfo.getClusterAddrTable().get(clusterName);
        if (brokerNames == null) {
            return result;
        }
        for (String brokerName : brokerNames) {
            BrokerData brokerData = clusterInfo.getBrokerAddrTable().get(brokerName);
            if (brokerData != null) {
                // 0 是 master,slave 不接受写操作
                String masterAddr = brokerData.getBrokerAddrs().get(0L);
                if (masterAddr != null) {
                    result.add(masterAddr);
                }
            }
        }
        return result;
    }

    /** topic admin 操作异常,starter 后台重试会接管。 */
    public static class RocketMqTopicAdminException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public RocketMqTopicAdminException(String message) {
            super(message);
        }

        public RocketMqTopicAdminException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
