package com.eagle.system.upms.application.mapper;

import com.eagle.system.upms.domain.model.Dept;
import com.eagle.system.upms.web.dto.response.DeptResponse;
import org.mapstruct.Mapper;

/**
 * @author sunshixiong
 */
@Mapper(componentModel = "spring")
public interface DeptMapper {

    /**
     * 将Dept转换为DeptResponse
     *
     * @param dept Dept
     * @return DeptResponse
     */
    DeptResponse toResponse(Dept dept);
}
