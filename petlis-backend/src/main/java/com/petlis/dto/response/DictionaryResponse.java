package com.petlis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用字典响应DTO
 *
 * 提供统一的字典数据响应格式,包含ID和名称字段
 * 使用泛型支持不同数据类型的ID字段
 *
 * @param <T> ID字段的数据类型
 * @author PETLIS Team
 * @since 1.0.0
 */
@Data
@Schema(description = "通用字典响应")
public class DictionaryResponse<T extends Number> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典项ID")
    private T id;

    @Schema(description = "字典项名称(中文)")
    private String name;

    @Schema(description = "字典项名称(英文)")
    private String nameEn;

    /**
     * 默认构造函数
     */
    public DictionaryResponse() {
    }

    /**
     * 全参构造函数
     *
     * @param id 字典项ID
     * @param name 字典项名称(中文)
     * @param nameEn 字典项名称(英文)
     */
    public DictionaryResponse(T id, String name, String nameEn) {
        this.id = id;
        this.name = name;
        this.nameEn = nameEn;
    }

    /**
     * 静态工厂方法 - 创建响应对象
     *
     * @param id 字典项ID
     * @param name 字典项名称(中文)
     * @param nameEn 字典项名称(英文)
     * @param <T> ID字段的数据类型
     * @return DictionaryResponse实例
     */
    public static <T extends Number> DictionaryResponse<T> of(T id, String name, String nameEn) {
        return new DictionaryResponse<>(id, name, nameEn);
    }
}
