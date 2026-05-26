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
        BlacklistResponse response = new BlacklistResponse();
        response.setId(blacklist.getId());
        response.setType(blacklist.getType());
        response.setValue(blacklist.getValue());
        response.setReason(blacklist.getReason());
        response.setExpiresAt(blacklist.getExpiresAt());
        response.setOperatorId(blacklist.getOperatorId());
        response.setOperatorName(blacklist.getOperatorName());
        response.setCreateTime(blacklist.getCreateTime());
        return response;
    }
}
