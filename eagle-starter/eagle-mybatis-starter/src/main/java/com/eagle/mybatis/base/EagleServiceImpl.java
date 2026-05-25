package com.eagle.mybatis.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.codes.CommonErrorCode;
import com.eagle.mybatis.model.EaglePageQuery;
import com.eagle.mybatis.model.EaglePageResult;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Eagle 通用 Service 实现基类。
 *
 * <p>继承 MyBatis-Plus 的 {@link ServiceImpl}，实现 {@link IEagleService} 中定义的
 * 项目级扩展方法。所有业务 Service 实现类均应继承此类，而非直接继承 {@link ServiceImpl}：
 *
 * <pre>{@code
 * @Service
 * public class UserServiceImpl extends EagleServiceImpl<UserMapper, User>
 *         implements UserService {
 *     // 业务特定方法...
 * }
 * }</pre>
 *
 * @param <M> Mapper 类型，必须继承 {@link BaseMapper}
 * @param <T> 实体类型
 * @author eagle
 */
public class EagleServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T>
        implements IEagleService<T> {

    /**
     * 分页查询，自动处理排序参数并返回统一分页响应。
     *
     * <p>排序处理逻辑：
     * <ol>
     *   <li>若 {@code EaglePageQuery.getOrderBy()} 非空，按指定字段和方向排序</li>
     *   <li>排序字段为驼峰命名（如 {@code createTime}），由 MyBatis-Plus 自动转换为下划线</li>
     *   <li>{@code EaglePageQuery.getOrderDirection()} 不区分大小写，{@code "asc"} 为升序，其余均为降序</li>
     * </ol>
     *
     * @param query   分页入参；不能为 {@code null}
     * @param wrapper 查询条件；为 {@code null} 时查询所有记录
     * @return 封装分页元信息和数据列表的 {@link EaglePageResult}
     */
    @Override
    public EaglePageResult<T> pageQuery(EaglePageQuery query, Wrapper<T> wrapper) {
        Page<T> page = query.toPage();

        if (StringUtils.hasText(query.getOrderBy())) {
            if ("asc".equalsIgnoreCase(query.getOrderDirection())) {
                page.addOrder(OrderItem.asc(query.getOrderBy()));
            } else {
                page.addOrder(OrderItem.desc(query.getOrderBy()));
            }
        }

        return EaglePageResult.of(baseMapper.selectPage(page, wrapper));
    }

    /**
     * 根据 ID 查询实体，不存在时抛出 {@link NotFoundException}（HTTP 404）。
     *
     * <p>错误码使用 {@link CommonErrorCode#NOT_FOUND}，错误消息使用传入的 {@code errorMsg}
     * 作为 {@link com.eagle.common.exception.ErrorCode#getDefaultMessage()} 的覆盖文本。
     *
     * @param id       主键 ID；不能为 {@code null}
     * @param errorMsg 实体不存在时的错误提示信息
     * @return 查询到的实体对象；保证非 {@code null}
     * @throws NotFoundException 当 ID 对应记录不存在时抛出
     */
    @Override
    public T getByIdOrThrow(Serializable id, String errorMsg) {
        T entity = getById(id);
        if (entity == null) {
            throw new NotFoundException(new SimpleErrorCode(errorMsg));
        }
        return entity;
    }

    /**
     * 批量保存或更新，自动分批提交以提升大批量写入性能。
     *
     * @param list      待批量写入的实体列表；为空时直接返回 {@code true}
     * @param batchSize 每批次提交的记录数
     * @return 操作是否全部成功
     */
    @Override
    public boolean saveOrUpdateBatchOptimized(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return saveOrUpdateBatch(list, batchSize);
    }

}
