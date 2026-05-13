package com.eagle.openapi.config;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.openapi.properties.OpenApiProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Eagle OpenAPI 自动配置。
 *
 * <p>提供统一的 Swagger/OpenAPI 配置：
 * <ul>
 *   <li>Bearer Token 直接输入</li>
 *   <li>OAuth2 授权码流程</li>
 *   <li>自动从 {@code @PreAuthorize} 提取角色信息展示在文档中</li>
 *   <li>公开端点自动去掉锁图标</li>
 * </ul>
 *
 * @author 孙士雄
 */
@AutoConfiguration
@ConditionalOnClass(io.swagger.v3.oas.models.OpenAPI.class)
@EnableConfigurationProperties(OpenApiProperties.class)
public class EagleOpenApiAutoConfiguration {

    private static final String BEARER_AUTH = "BearerAuth";
    private static final String OAUTH2 = "OAuth2";
    private static final Pattern HAS_ROLE_PATTERN = Pattern.compile("hasRole\\('(\\w+)'\\)");

    private final OpenApiProperties properties;

    public EagleOpenApiAutoConfiguration(OpenApiProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 OpenAPI 基础信息与安全方案。
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                // servers 显式置为相对路径 "/"，覆盖 SpringDoc 根据请求 URL 自动生成的内网绝对地址。
                // 通过网关聚合访问时 Swagger UI 加载 doc 的 origin 即 gateway host，
                // Try-it-out 调用前缀自动随 origin 走；直接访问下游服务时同理用本地 host。
                // Controller 路径本身已含 /api/{alias} 前缀，server.url 不需要重复。
                .servers(List.of(new Server().url("/")))
                .info(new Info()
                        .title(properties.getTitle())
                        .version(properties.getVersion())
                        .description(apiDescription())
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, bearerScheme())
                        .addSecuritySchemes(OAUTH2, oauth2Scheme()))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_AUTH)
                        .addList(OAUTH2, List.of("openid", "profile")));
    }

    /**
     * 自动解析 {@code @PreAuthorize} 注解，将角色要求写入文档描述，公开端点去掉锁图标。
     */
    @Bean
    @ConditionalOnMissingBean
    public OperationCustomizer preAuthorizeOperationCustomizer() {
        return (operation, handlerMethod) -> {
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                return operation;
            }

            String value = preAuthorize.value();

            if (value.contains("permitAll()")) {
                operation.setSecurity(List.of());
                appendDescription(operation, "🔓 公开接口，无需认证");
                return operation;
            }

            String roleDesc = extractRoleDescription(value);
            if (roleDesc != null) {
                appendDescription(operation, "🔒 " + roleDesc);
            }

            return operation;
        };
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        直接输入 JWT Access Token。
                        
                        获取方式：
                        1. 用户名密码：通过 OAuth2 授权码流程获取
                        2. 短信验证码：POST /oauth2/token (grant_type=sms_code&phone=xxx&code=xxx&client_id=eagleWeb)
                        3. 微信登录：通过微信扫码/小程序流程获取""");
    }

    private SecurityScheme oauth2Scheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 授权码流程（PKCE），适用于用户名密码登录")
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl(SecurityConstants.AUTH_AUTHORIZE)
                                .tokenUrl(SecurityConstants.AUTH_TOKEN)
                                .scopes(new Scopes()
                                        .addString("openid", "OpenID 身份标识")
                                        .addString("profile", "用户基本信息"))));
    }

    private String extractRoleDescription(String preAuthorizeValue) {
        if (preAuthorizeValue.contains("isAuthenticated()")) {
            if (preAuthorizeValue.contains("hasRole")) {
                Matcher matcher = HAS_ROLE_PATTERN.matcher(preAuthorizeValue);
                StringBuilder sb = new StringBuilder("需要角色 ");
                while (matcher.find()) {
                    sb.append("`").append(matcher.group(1)).append("` ");
                }
                sb.append("或已登录用户");
                return sb.toString();
            }
            return "需要登录";
        }

        Matcher matcher = HAS_ROLE_PATTERN.matcher(preAuthorizeValue);
        if (matcher.find()) {
            StringBuilder sb = new StringBuilder("需要角色 `").append(matcher.group(1)).append("`");
            while (matcher.find()) {
                sb.append(" / `").append(matcher.group(1)).append("`");
            }
            return sb.toString();
        }

        if (preAuthorizeValue.contains("authentication.principal.id")) {
            Matcher roleMatcher = HAS_ROLE_PATTERN.matcher(preAuthorizeValue);
            if (roleMatcher.find()) {
                return "需要角色 `" + roleMatcher.group(1) + "` 或本人操作";
            }
            return "仅限本人操作";
        }

        return null;
    }

    private void appendDescription(io.swagger.v3.oas.models.Operation operation, String text) {
        String existing = operation.getDescription();
        if (existing != null && !existing.isBlank()) {
            operation.setDescription(existing + "<br/>" + text);
        } else {
            operation.setDescription(text);
        }
    }

    private String apiDescription() {
        return properties.getDescription() != null ? properties.getDescription() : """
                Eagle 企业级应用接口文档
                
                ## 认证方式
                
                ### 方式一：Bearer Token（推荐调试用）
                点击右上角 **Authorize** 按钮，在 BearerAuth 中粘贴 JWT Token。
                
                ### 方式二：OAuth2 授权码流程
                点击 **Authorize** 按钮，在 OAuth2 中点击授权，跳转登录页完成认证。
                
                ### 方式三：短信验证码获取 Token
                1. 调用 `POST /sms/code?phone=手机号` 获取验证码
                2. 调用 `POST /oauth2/token` 参数：
                   - `grant_type=sms_code`
                   - `phone=手机号`
                   - `code=验证码`
                   - `client_id=eagleWeb`
                3. 复制返回的 `access_token`，粘贴到 BearerAuth 中
                
                ## 角色说明
                
                | 角色 | 说明 |
                |------|------|
                | `admin` | 系统管理员，拥有所有管理权限 |
                | `user` | 普通用户，拥有基础查看权限 |
                """;
    }
}
