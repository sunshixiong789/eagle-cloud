package com.eagle.system.base.application.mapper;

import com.eagle.system.base.domain.model.Dept;
import com.eagle.system.base.web.dto.response.DeptResponse;
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
