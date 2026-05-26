package com.eagle.auth.core.infrastructure.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 可信客户端 IP 解析器。
 *
 * <p>{@code X-Forwarded-For} 是用户可伪造的请求头，只有来自 {@link TrustedProxyProperties} 中
 * 配置的可信反向代理的请求才采纳 XFF；否则一律使用 {@link HttpServletRequest#getRemoteAddr()}。
 *
 * <p>解析规则：
 * <ol>
 *   <li>取 {@code request.getRemoteAddr()} 作为直连远端地址</li>
 *   <li>若直连地址在可信 CIDR 列表内，从 {@code X-Forwarded-For} 头从右向左反向遍历，
 *       跳过仍在可信列表内的中间代理，返回第一个非可信代理 IP（即真实客户端 IP）</li>
 *   <li>否则直接返回直连地址</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestIpResolver {

    private static final String HEADER_XFF = "X-Forwarded-For";

    private final TrustedProxyProperties properties;
    private final List<CidrRange> trustedRanges = new ArrayList<>();

    @PostConstruct
    void init() {
        for (String cidr : properties.getTrustedProxies()) {
            try {
                trustedRanges.add(CidrRange.parse(cidr));
            } catch (Exception ex) {
                log.warn("invalid trusted-proxy cidr: {}", cidr, ex);
            }
        }
        log.info("RequestIpResolver initialized, trusted proxies count={}", trustedRanges.size());
    }

    /**
     * 解析请求的真实客户端 IP。
     *
     * @param request HTTP 请求（不可为 null）
     * @return 真实客户端 IP；解析失败返回直连远端地址
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        String xff = request.getHeader(HEADER_XFF);
        if (xff == null || xff.isBlank()) {
            return remoteAddr;
        }
        String[] hops = xff.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (hop.isEmpty()) {
                continue;
            }
            if (!isTrustedProxy(hop)) {
                return hop;
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            for (CidrRange range : trustedRanges) {
                if (range.contains(addr)) {
                    return true;
                }
            }
        } catch (UnknownHostException ex) {
            log.debug("unparseable ip: {}", ip);
        }
        return false;
    }

    /**
     * CIDR 网段（支持 IPv4 / IPv6）。
     */
    private record CidrRange(BigInteger network, BigInteger mask, int addressLength) {

        static CidrRange parse(String cidr) throws UnknownHostException {
            int slash = cidr.indexOf('/');
            String addrPart = slash < 0 ? cidr : cidr.substring(0, slash);
            InetAddress base = InetAddress.getByName(addrPart);
            int addressBits = base.getAddress().length * 8;
            int prefix = slash < 0 ? addressBits : Integer.parseInt(cidr.substring(slash + 1));
            if (prefix < 0 || prefix > addressBits) {
                throw new IllegalArgumentException("invalid prefix length: " + cidr);
            }
            BigInteger mask = prefix == 0
                    ? BigInteger.ZERO
                    : BigInteger.ONE.shiftLeft(addressBits).subtract(BigInteger.ONE)
                    .shiftLeft(addressBits - prefix)
                    .and(BigInteger.ONE.shiftLeft(addressBits).subtract(BigInteger.ONE));
            BigInteger network = new BigInteger(1, base.getAddress()).and(mask);
            return new CidrRange(network, mask, base.getAddress().length);
        }

        boolean contains(InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes.length != addressLength) {
                return false;
            }
            BigInteger value = new BigInteger(1, bytes);
            return value.and(mask).equals(network);
        }
    }
}
