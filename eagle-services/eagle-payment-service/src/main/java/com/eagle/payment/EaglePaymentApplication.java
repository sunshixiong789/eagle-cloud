package com.eagle.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.modulith.Modulithic;

import java.util.Optional;

/**
 * Eagle 支付服务入口 (Servlet 栈)。
 *
 * <p>承载 Payment / Refund / Transfer 聚合根的生命周期管理,支付宝/微信渠道适配,
 * 异步回调验签与状态机推进,清算对账。
 *
 * <p>对外通过 RocketMQ {@code payment_payment_events} / {@code payment_refund_events}
 * / {@code payment_transfer_events} / {@code payment_reconcile_events} 发布集成事件,
 * 上游业务方 (order-service / ledger-service 等) 通过订阅 MQ 推进自身状态。
 *
 * <p>同步入口走 {@code /internal/payments}、{@code /internal/refunds}、
 * {@code /internal/transfers},仅服务间调用;{@code /payment/alipay/notify}、
 * {@code /payment/wechat/notify} 为外部回调,通过 yml permit-paths + 网关 IP 白名单防护。
 *
 * <p>主类放在 {@code com.eagle.payment} 包内,避免与其他 starter
 * {@code AutoConfigurationPackages} 注册产生祖先/子包重叠。
 *
 * @author sunshixiong
 */
@Slf4j
@Modulithic(systemName = "EaglePayment")
@SpringBootApplication
@ConfigurationPropertiesScan
public class EaglePaymentApplication {

    private final Environment env;

    public EaglePaymentApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(EaglePaymentApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = Optional.ofNullable(env.getProperty("server.port")).orElse("8083");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path")).orElse("");
        String baseUrl = "http://localhost:" + port + contextPath;

        log.info(
                """

                        ╔══════════════════════════════════════════════════════════════╗
                        ║  💳 Eagle Payment Service Started Successfully!              ║
                        ╠══════════════════════════════════════════════════════════════╣
                        ║  📚 Swagger UI:      {}║
                        ║  📖 API Docs:        {}║
                        ║  🪙 Alipay Notify:   {}║
                        ║  🪙 Wechat Notify:   {}║
                        ╚══════════════════════════════════════════════════════════════╝
                        """,
                padRight(baseUrl + "/swagger-ui.html", 38),
                padRight(baseUrl + "/v3/api-docs", 38),
                padRight(baseUrl + "/payment/alipay/notify", 38),
                padRight(baseUrl + "/payment/wechat/notify", 38));
    }

    private String padRight(String s, int n) {
        if (s.length() > n) {
            return s.substring(0, n - 3) + "...";
        }
        return String.format("%-" + n + "s", s);
    }
}
