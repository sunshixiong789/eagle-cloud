package com.eagle.system.base.application.service;

import com.eagle.common.exception.ResourceNotFoundException;
import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.service.PasswordEncryptor;
import com.eagle.system.base.interfaces.dto.request.ChangePasswordRequest;
import com.eagle.system.base.interfaces.dto.request.CreateUserRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务（轻量级 DDD）
 * <p>
 * 职责：
 * <ul>
 *   <li>协调领域对象完成业务用例</li>
 *   <li>管理事务边界</li>
 *   <li>使用 MapStruct 进行对象转换</li>
 *   <li>业务逻辑委托给实体的业务方法</li>
 * </ul>
 * <p>
 * 改进点：
 * <ul>
 *   <li>合并了 UserCommandService 和 UserQueryService</li>
 *   <li>删除了空洞的 UserDomainService</li>
 *   <li>删除了 Command 层，直接使用 Request</li>
 *   <li>使用 MapStruct 自动映射，消除手动转换代码</li>
 *   <li>唯一性校验依赖数据库约束 + 全局异常处理</li>
 *   <li>使用领域层的 PasswordEncryptor 接口，隔离技术细节</li>
 * </ul>
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncryptor passwordEncryptor;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 命令操作（写） ====================

    /**
     * 创建用户
     *
     * @param request 创建用户请求
     * @return 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(CreateUserRequest request) {
        // 1. 使用 MapStruct 映射基础字段
        User user = userMapper.requestToEntity(request);

        // 2. 手动设置值对象（因为值对象不可变）
        user.setProfile(userMapper.createProfile(request));

        // 3. 设置部门（如果有）
        if (request.getDepartmentId() != null) {
            user.assignToDept(request.getDepartmentId());
        }

        // 4. 调用聚合根的业务方法（会发布 UserCreatedEvent）
        user.initializeAsNewUser(passwordEncryptor, request.getPassword());

        // 5. 持久化并发布领域事件（唯一性约束由数据库保证）
        User savedUser = userRepository.save(user);

        return savedUser.getId();
    }

    /**
     * 更新用户信息
     *
     * @param userId  用户 ID
     * @param request 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UpdateUserRequest request) {
        // 1. 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 2. 调用实体的业务方法
        user.updateProfile(request.getName(), request.getNickname(), request.getAvatar());

        // 3. 持久化
        userRepository.save(user);
    }

    /**
     * 修改密码
     *
     * @param userId  用户 ID
     * @param request 修改密码请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 1. 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 2. 调用实体的业务方法（包含旧密码验证，会发布 UserPasswordChangedEvent）
        user.changePassword(request.getOldPassword(), request.getNewPassword(), passwordEncryptor);

        // 3. 保存并领域事件
        userRepository.save(user);
    }

    /**
     * 锁定用户
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 调用实体的业务方法（包含状态校验）
        user.lock();
        userRepository.save(user);
    }

    /**
     * 解锁用户
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 调用实体的业务方法（包含状态校验）
        user.unlock();
        userRepository.save(user);
    }
}
