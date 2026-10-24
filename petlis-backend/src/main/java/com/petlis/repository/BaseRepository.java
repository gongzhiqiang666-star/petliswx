package com.petlis.repository;

import com.petlis.common.security.SecurityUtils;
import com.petlis.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 基础Repository接口
 *
 * 提供数据权限过滤的通用方法,确保所有查询都带上医院ID过滤
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@NoRepositoryBean
public class BaseRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 构造函数
     */
    public BaseRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    /**
     * 获取当前医院ID
     *
     * @return 医院ID
     * @throws BusinessException 如果用户未登录
     */
    protected Integer getCurrentHospitalId() {
        try {
            return SecurityUtils.getCurrentHospitalId();
        } catch (Exception e) {
            log.warn("获取当前医院ID失败: {}", e.getMessage());
            throw new BusinessException("用户未登录或无权限访问");
        }
    }

    /**
     * 检查实体是否属于当前医院
     *
     * 注意: 此方法需要在具体实体类中实现医院ID字段的检查
     *
     * @param entity 实体对象
     * @return 是否属于当前医院
     */
    protected boolean belongsToCurrentHospital(Object entity) {
        // 默认实现,子类可以重写
        return true;
    }

    /**
     * 保存前检查医院权限
     *
     * @param entity 实体对象
     */
    protected void validateHospitalPermission(T entity) {
        // 默认实现,子类可以重写
        if (!belongsToCurrentHospital(entity)) {
            throw new BusinessException("无权限操作该数据");
        }
    }

    @Override
    @Transactional
    public <S extends T> S save(S entity) {
        // 保存前进行权限验证
        validateHospitalPermission(entity);
        return super.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        // 删除前进行权限验证
        Optional<T> entity = findById(id);
        if (entity.isPresent()) {
            validateHospitalPermission(entity.get());
        }
        super.deleteById(id);
    }
}
