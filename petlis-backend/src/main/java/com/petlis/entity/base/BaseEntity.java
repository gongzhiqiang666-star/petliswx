package com.petlis.entity.base;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

/**
 * 基础实体类
 * 包含所有实体的公共字段: ID、创建时间、更新时间
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    protected Integer id;

    /**
     * 创建时间(Unix时间戳,秒)
     */
    @Column(name = "createtime")
    protected Integer createtime;

    /**
     * 更新时间(Unix时间戳,秒)
     */
    @Column(name = "updatetime")
    protected Integer updatetime;
}
