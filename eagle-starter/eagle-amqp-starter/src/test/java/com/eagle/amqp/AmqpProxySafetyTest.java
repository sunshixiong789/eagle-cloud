package com.eagle.amqp;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.common.event.BaseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * listener 被 AOP 代理后仍必须可用。
 *
 * <p>背景：listener 是普通 {@code @Component}，业务方随时可能在上面加
 * {@code @Transactional} / {@code @RateLimit} 等注解，Spring 就会把它包成 CGLIB 代理。
 * CGLIB 代理实例经 Objenesis <b>绕过构造器</b>创建，其 {@code amqpProperties} 恒为 null，
 * 而基类的 {@code resolveXxx()} 是 final、覆盖不了，只能在代理实例上执行 ——
 * 任何 final 方法只要直读 {@code amqpProperties} 就会 NPE，且是在启动期
 * {@code AmqpListenerRegistrar} 注册 listener 时炸，整个应用起不来。
 *
 * <p>本测试用与 Spring AOP 完全相同的 {@code ObjenesisCglibAopProxy} 造出代理，
 * 反射遍历<b>全部</b> final 方法逐个调用，任何一个漏走可覆盖方法都会在这里失败。
 */
@DisplayName("listener 的 AOP 代理安全性")
class AmqpProxySafetyTest {

    private static final String PREFIX = "prod_";
    private static final String TOPIC = "user_invitation_bound";
    private static final String GROUP = "user_membership_invitation_bound";

    static class SampleMessage extends BaseEvent {
    }

    static class SampleConsumer extends AbstractAmqpListener<SampleMessage> {
        final List<SampleMessage> handled = new ArrayList<>();

        SampleConsumer(AmqpProperties p) {
            super(p);
        }

        @Override
        protected String getTopic() {
            return TOPIC;
        }

        @Override
        protected String getConsumerGroup() {
            return GROUP;
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handle(SampleMessage event) {
            handled.add(event);
        }
    }

    static class SampleDlqListener extends AbstractDlqListener<SampleMessage> {
        SampleDlqListener(AmqpProperties p) {
            super(p);
        }

        @Override
        protected String getOriginalTopic() {
            return TOPIC;
        }

        @Override
        protected String getOriginalConsumerGroup() {
            return GROUP;
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handleDeadLetter(SampleMessage event, int totalAttempts) {
        }
    }

    private static AmqpProperties props() {
        AmqpProperties p = new AmqpProperties();
        p.setExchangePrefix(PREFIX);
        return p;
    }

    /** 造一个与 Spring @Transactional 同款的 CGLIB 代理：Objenesis 建实例，构造器不执行。 */
    @SuppressWarnings("unchecked")
    private static <T> T cglibProxy(T target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        // 空 advice：只为触发代理生成，等价于业务方加了个 @Transactional
        factory.addAdvice((org.aopalliance.intercept.MethodInterceptor) org.aopalliance.intercept.MethodInvocation::proceed);
        return (T) factory.getProxy();
    }

    @Test
    @DisplayName("代理实例自身的 amqpProperties 确为 null —— 前提成立，本测试才有意义")
    void proxyInstanceFieldIsNull() throws Exception {
        SampleConsumer proxy = cglibProxy(new SampleConsumer(props()));

        java.lang.reflect.Field field = AbstractAmqpListener.class.getDeclaredField("amqpProperties");
        field.setAccessible(true);
        assertThat(field.get(proxy))
                .as("CGLIB 代理绕过构造器创建，该字段必然为 null；若哪天不再为 null，本测试的前提需重估")
                .isNull();
    }

    @Test
    @DisplayName("被代理的主 listener：全部 final 方法都不能抛 NPE")
    void allFinalMethodsSurviveProxyingOnMainListener() {
        SampleConsumer target = new SampleConsumer(props());
        SampleConsumer proxy = cglibProxy(target);

        assertThat(proxy.resolveExchangeName()).isEqualTo(PREFIX + TOPIC);
        assertThat(proxy.resolveQueueName()).isEqualTo(PREFIX + TOPIC + "." + GROUP);
        assertThat(proxy.resolveEventClass()).isEqualTo(SampleMessage.class);
        assertThat(proxy.resolveRetryAlertThreshold())
                .isEqualTo(props().getConsumer().getRetryAlertThreshold());

        SampleMessage event = new SampleMessage();
        proxy.dispatch(event);
        assertThat(target.handled).as("业务处理必须落到真正的 target 实例上").containsExactly(event);

        invokeEveryFinalMethod(proxy);
    }

    @Test
    @DisplayName("被代理的 DLQ listener：DLQ 名解析与全部 final 方法同样不能抛 NPE")
    void allFinalMethodsSurviveProxyingOnDlqListener() {
        SampleDlqListener proxy = cglibProxy(new SampleDlqListener(props()));

        assertThat(proxy.resolveDlqName()).isEqualTo(PREFIX + TOPIC + "." + GROUP + ".dlq");

        invokeEveryFinalMethod(proxy);
    }

    /**
     * 反射调用 listener 继承链上的每一个 final 实例方法。
     *
     * <p>这是这条约束的护栏：将来谁在基类新增一个直读 {@code amqpProperties} 的 final 方法，
     * 这里就会以 NPE 失败，而不是等到线上启动时才炸。
     */
    private static void invokeEveryFinalMethod(AbstractAmqpListener<?> proxy) {
        List<String> invoked = new ArrayList<>();
        // 从 target 类而非代理类起步：CGLIB 生成类自己也声明了一堆 final 的 Advised 接口方法，
        // 那些与本约束无关。这里要遍历的是 listener 继承链上的 final 方法。
        for (Class<?> type = AopUtils.getTargetClass(proxy); type != null && type != Object.class;
                type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                int mods = method.getModifiers();
                if (!Modifier.isFinal(mods) || Modifier.isStatic(mods)
                        || Modifier.isPrivate(mods) || method.isSynthetic()) {
                    continue;
                }
                method.setAccessible(true);
                try {
                    method.invoke(proxy, argumentsFor(method));
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof NullPointerException npe) {
                        fail(("final 方法 %s 在 CGLIB 代理实例上抛了 NPE —— 它直读了 amqpProperties，"
                                + "请改为经可覆盖方法取值（见 AbstractAmqpListener#getExchangePrefix）")
                                .formatted(method.getName()), npe);
                    }
                    // 其它异常与代理安全性无关（如子类业务逻辑自身抛错），忽略
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                invoked.add(method.getName());
            }
        }
        assertThat(invoked).as("至少应覆盖到 resolveExchangeName / resolveQueueName").isNotEmpty();
    }

    /** 按参数类型造调用实参；遇到未知类型直接失败，提示补充映射而不是静默跳过。 */
    private static Object[] argumentsFor(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == Message.class) {
                args[i] = MessageBuilder.withBody("{}".getBytes(StandardCharsets.UTF_8)).build();
            } else if (type == String.class) {
                args[i] = "{}";
            } else if (type == int.class) {
                args[i] = 1;
            } else if (Exception.class.isAssignableFrom(type)) {
                args[i] = new IllegalStateException("boom");
            } else if (BaseEvent.class.isAssignableFrom(type)) {
                args[i] = new SampleMessage();
            } else {
                fail("final 方法 %s 有本测试不认识的参数类型 %s，请在 argumentsFor 中补充映射"
                        .formatted(method.getName(), type.getName()));
            }
        }
        return args;
    }
}
