package com.petlis.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可激活实体基类
 * 继承BaseEntity,添加IsActive字段用于逻辑删除
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class ActivableEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 启用状态
     * 0 = 禁用/删除
     * 1 = 启用/正常
     */
    @Column(name = "IsActive", nullable = false)
    protected Integer isActive;
}
