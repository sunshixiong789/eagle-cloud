package com.eagle.auth.core.application.mapper;

import com.eagle.auth.core.domain.model.Blacklist;
import com.eagle.auth.core.interfaces.dto.response.BlacklistResponse;
import org.springframework.stereotype.Component;

/**
 * Blacklist 领域对象 → Response DTO 映射
 *
 * @author sunshixiong
 */
@Component
public class BlacklistMapper {

    public BlacklistResponse toResponse(Blacklist blacklist) {
        if (blacklist == null) {
            return null;
        }
        return new BlacklistResponse(
                blacklist.getId(),
                blacklist.getType(),
                blacklist.getValue(),
                blacklist.getReason(),
                blacklist.getExpiresAt(),
                blacklist.getOperatorId(),
                blacklist.getOperatorName(),
                blacklist.getCreateTime());
    }
}
