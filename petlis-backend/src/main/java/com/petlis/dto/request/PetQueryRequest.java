package com.petlis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 宠物档案查询请求DTO
 *
 * @author PETLIS Team
 */
@Data
@Schema(description = "宠物档案查询请求")
public class PetQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索关键词(中文名或英文名)", example = "旺财")
    private String name;

    @Schema(description = "页码(从0开始)", example = "0")
    private Integer page = 0;

    @Schema(description = "每页数量", example = "10")
    private Integer size = 10;

    @Schema(description = "排序参数(格式: 字段名,排序方向)", example = "createtime,desc")
    private String sort;
}
