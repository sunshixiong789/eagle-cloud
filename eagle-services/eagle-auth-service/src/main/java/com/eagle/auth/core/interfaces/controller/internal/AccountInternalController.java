package com.eagle.auth.core.interfaces.controller.internal;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.config.AdminProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Account 内部 API（仅供 system-service 通过服务发现调用）。
 *
 * <p>主用途：system-service 启动期主动拉 admin Account 兜底，
 * 解决"AccountRegisteredEvent 只首次发布一次、MQ 不可用就永远拿不到"的强耦合。
 *
 * <p>路径前缀 {@code /internal/**} 由网关 + client-credentials scope 鉴权。
 *
 * @author sunshixiong
 */
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class AccountInternalController {

    private final AccountRepository accountRepository;
    private final AdminProperties adminProperties;

    /**
     * 按用户名查 Account 快照。Account 不存在时返回 404（client 端 RestClient 错误处理器会
     * 自动转为 {@code NotFoundException}，调用方据此走 fallback 流程）。
     *
     * <p>仅暴露持久化字段——nickname / avatar / email 在 Account 上是
     * {@code @Transient ProfileHints}，注册事件已发出即被清除，此处取不到。
     */
    @GetMapping("/by-username/{username}")
    public AccountSnapshot findByUsername(@PathVariable String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);
        return new AccountSnapshot(account.getId(), account.getUsername(), account.getPhone(),
                account.getStatus() == AccountStatus.FROZEN);
    }

    /**
     * 按 accountId 查 Account 快照。下游读模型（如 member-stats）持有的 userId 实为 auth
     * accountId，经此端点解析 username / phone / 冻结态。Account 不存在时返回 404（client 端
     * RestClient 错误处理器会自动转为 {@code NotFoundException}，调用方据此走 fallback 流程）。
     *
     * <p>仅暴露持久化字段——nickname / avatar / email 在 Account 上是
     * {@code @Transient ProfileHints}，注册事件已发出即被清除，此处取不到。
     */
    @GetMapping("/{accountId}")
    public AccountSnapshot findById(@PathVariable Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);
        return new AccountSnapshot(account.getId(), account.getUsername(), account.getPhone(),
                account.getStatus() == AccountStatus.FROZEN);
    }

    /**
     * 批量查询账号快照。不存在的 ID 不返回，调用方可用返回集合与请求集合的差集识别孤儿数据。
     */
    @Operation(summary = "批量查询内部账号快照", description = "最多查询 100 个账号；不存在的账号 ID 不返回")
    @PostMapping("/batch")
    public List<AccountSnapshot> findBatch(@Valid @RequestBody AccountBatchRequest request) {
        return accountRepository.findAllById(request.accountIds()).stream()
                .map(this::toSnapshot)
                .sorted(Comparator.comparing(AccountSnapshot::accountId))
                .toList();
    }

    /**
     * 按手机号批量查询账号快照。未注册的号码不返回，调用方用差集识别未注册号码。
     *
     * <p>单个手机号查询也走本端点（集合传一个元素）——手机号属敏感字段，放在请求体里
     * 避免进入 URL、access log 与 Referer（见 rules/12-security.md）。
     *
     * <p>注意手机号账号的 {@code username} 是 {@code "phone_" + shortHash(phone)}，
     * 不是手机号本身，所以 {@link #findByUsername(String)} 无法用于按手机号查账号。
     */
    @Operation(summary = "按手机号批量查询内部账号快照",
            description = "最多查询 100 个手机号；未注册的号码不返回")
    @PostMapping("/by-phones")
    public List<AccountSnapshot> findBatchByPhones(@Valid @RequestBody AccountPhoneBatchRequest request) {
        return accountRepository.findByPhoneIn(request.phones()).stream()
                .map(this::toSnapshot)
                .sorted(Comparator.comparing(AccountSnapshot::accountId))
                .toList();
    }

    /**
     * 全量账号数（权威源，不包含初始化管理员）。
     *
     * <p>主用途：system-service Dashboard 统计"总用户数"——auth_account 是注册的事实来源,
     * 比 base_user 镜像更可靠（后者依赖 RocketMQ 同步链路,broker 抖动期间会偏小）。
     */
    @GetMapping("/count")
    public long count() {
        return accountRepository.countByUsernameNot(adminProperties.getUsername());
    }

    private AccountSnapshot toSnapshot(Account account) {
        return new AccountSnapshot(account.getId(), account.getUsername(), account.getPhone(),
                account.getStatus() == AccountStatus.FROZEN);
    }

    @Schema(description = "批量账号快照查询请求")
    public record AccountBatchRequest(
            @Schema(description = "认证账号 ID 集合", example = "[7, 8]", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotEmpty @Size(max = 100) Set<@NotNull Long> accountIds) {
    }

    /** 按手机号批量查询请求。手机号为敏感字段，故不提供 {@code example}。 */
    @Schema(description = "按手机号批量查询账号快照请求")
    public record AccountPhoneBatchRequest(
            @Schema(description = "手机号集合", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotEmpty @Size(max = 100) Set<@NotBlank String> phones) {
    }

    /** 内部 Account 快照（仅持久化字段）。{@code locked} = 账号是否冻结（status=FROZEN）。 */
    public record AccountSnapshot(Long accountId, String username, String phone, boolean locked) {
    }
}
