package com.eagle.resource.config;

import com.eagle.common.constant.SecurityConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAPI / Swagger 配置
 * <p>
 * 功能：
 * <ul>
 *   <li>Bearer Token 直接输入（适合已有 Token 的调试场景）</li>
 *   <li>OAuth2 授权码流程（Swagger UI 内置授权按钮）</li>
 *   <li>自动从 {@code @PreAuthorize} 提取角色信息展示在文档中</li>
 *   <li>公开端点（permitAll）自动去掉锁图标</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "BearerAuth";
    private static final String OAUTH2 = "OAuth2";

    private static final Pattern HAS_ROLE_PATTERN = Pattern.compile("hasRole\\('(\\w+)'\\)");

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eagle API")
                        .version("v1.0.0")
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
     * 自动解析 @PreAuthorize 注解，将角色要求写入文档描述，公开端点去掉锁图标
     */
    @Bean
    public OperationCustomizer preAuthorizeOperationCustomizer() {
        return (operation, handlerMethod) -> {
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                return operation;
            }

            String value = preAuthorize.value();

            if (value.contains("permitAll()")) {
                operation.setSecurity(List.of());
                appendDescription(operation, "\uD83D\uDD13 公开接口，无需认证");
                return operation;
            }

            String roleDesc = extractRoleDescription(value);
            if (roleDesc != null) {
                appendDescription(operation, "\uD83D\uDD12 " + roleDesc);
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
                // 组合表达式：hasRole('admin') or isAuthenticated()
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
        return """
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
