package com.eagle.system.system.application.mapper;

import com.eagle.eagle.system.domain.model.User;
import com.eagle.eagle.system.domain.model.valueobject.UserProfile;
import com.eagle.eagle.system.interfaces.dto.request.CreateUserRequest;
import org.mapstruct.Mapper;

/**
 * 用户对象映射器
 * <p>
 * 使用 MapStruct 自动生成映射代码，消除手动转换
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    // ==================== Request → Entity ====================

    /**
     * 创建请求转实体（仅用于字段映射，不包含密码和初始化逻辑）
     * <p>
     * 注意：UserProfile 是不可变值对象，需要在应用层手动设置
     * @param request 创建用户请求
     * @return 用户实体
     */
    User requestToEntity(CreateUserRequest request);

    /**
     * 手动创建 UserProfile（因为值对象不可变）
     */
    default UserProfile createProfile(CreateUserRequest request) {
        return new UserProfile(
                request.getAvatar(),
                request.getNickname(),
                request.getName(),
                null,  // gender
                null   // bio
        );
    }

    // ==================== 脱敏方法 ====================

    /**
     * 手机号脱敏
     */
    default String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
