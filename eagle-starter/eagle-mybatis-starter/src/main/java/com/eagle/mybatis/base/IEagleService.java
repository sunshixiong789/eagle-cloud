package com.eagle.mybatis.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.eagle.mybatis.model.EaglePageQuery;
import com.eagle.mybatis.model.EaglePageResult;

import java.io.Serializable;
import java.util.List;

/**
 * Eagle 通用 Service 接口，扩展 MyBatis-Plus {@link IService}。
 *
 * <p>在 {@link IService} 的基础上增加以下项目级通用能力：
 * <ul>
 *   <li>分页查询（含排序，返回 {@link EaglePageResult} 封装）</li>
 *   <li>按 ID 查询（不存在时抛 {@link com.eagle.common.exception.NotFoundException}，而非返回 null）</li>
 *   <li>批量保存优化（自动分批提交，避免超出数据库单次写入限制）</li>
 * </ul>
 *
 * <p>所有业务 Service 接口应继承此接口（而非直接继承 {@link IService}），
 * 对应实现类继承 {@link EagleServiceImpl}：
 * <pre>{@code
 * // Service 接口
 * public interface UserService extends IEagleService<User> { }
 *
 * // Service 实现
 * @Service
 * public class UserServiceImpl extends EagleServiceImpl<UserMapper, User>
 *         implements UserService { }
 * }</pre>
 *
 * @param <T> 实体类型
 * @author eagle
 */
public interface IEagleService<T> extends IService<T> {

    /**
     * 分页查询，返回统一分页响应结果。
     *
     * <p>自动处理 {@link EaglePageQuery#getOrderBy()} 和 {@link EaglePageQuery#getOrderDirection()}
     * 排序参数，无需在业务层手动构建排序条件。
     *
     * <p>使用示例：
     * <pre>{@code
     * LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
     *     .like(StringUtils.hasText(query.getUsername()), User::getUsername, query.getUsername())
     *     .eq(query.getStatus() != null, User::getStatus, query.getStatus());
     * EaglePageResult<User> result = userService.pageQuery(query, wrapper);
     * }</pre>
     *
     * @param query   分页查询入参（页码、每页大小、排序字段和方向）；不能为 {@code null}
     * @param wrapper 查询条件（过滤条件）；为 {@code null} 时查询所有记录
     * @return 包含当前页数据和分页元信息的 {@link EaglePageResult}
     */
    EaglePageResult<T> pageQuery(EaglePageQuery query, Wrapper<T> wrapper);

    /**
     * 根据 ID 查询实体，不存在时抛出 {@link com.eagle.common.exception.NotFoundException}。
     *
     * <p>与 {@link IService#getById(Serializable)} 的区别：
     * {@code getById} 返回 {@code null}，调用方需要自行 null 检查；
     * 此方法保证返回非 null 实体，否则抛出业务异常，简化 Controller/Application 层代码。
     *
     * <p>使用示例：
     * <pre>{@code
     * // 不存在时自动抛出 NotFoundException（HTTP 404）
     * User user = userService.getByIdOrThrow(userId, "用户不存在");
     * }</pre>
     *
     * @param id       主键 ID；不能为 {@code null}
     * @param errorMsg 实体不存在时的错误提示信息（中文），将作为异常消息返回给客户端
     * @return 查询到的实体对象；保证非 {@code null}
     * @throws com.eagle.common.exception.NotFoundException 当 ID 对应记录不存在时抛出
     */
    T getByIdOrThrow(Serializable id, String errorMsg);

    /**
     * 批量保存或更新，自动按指定批次大小分批提交，提升大批量写入性能。
     *
     * <p>底层委托给 MyBatis-Plus 的 {@code saveOrUpdateBatch}，分批提交以避免单次
     * SQL 超出数据库 {@code max_allowed_packet} 限制或事务过大导致锁竞争。
     *
     * @param list      待批量写入的实体列表；为空时直接返回 {@code true}
     * @param batchSize 每批次提交的记录数；建议值 100 ~ 1000
     * @return 操作是否全部成功
     */
    boolean saveOrUpdateBatchOptimized(List<T> list, int batchSize);
}
