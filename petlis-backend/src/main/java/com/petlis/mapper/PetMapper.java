package com.petlis.mapper;

import com.petlis.dto.request.PetCreateRequest;
import com.petlis.dto.request.PetUpdateRequest;
import com.petlis.dto.response.PetResponse;
import com.petlis.entity.business.Pet;
import com.petlis.vo.PetVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * 宠物档案MapStruct映射器
 *
 * 用于Entity和DTO之间的相互转换
 *
 * @author PETLIS Team
 */
@Mapper(componentModel = "spring")
public interface PetMapper {

    /**
     * 将创建请求转换为Pet实体
     *
     * @param request 创建请求DTO
     * @return Pet实体
     */
    Pet toEntity(PetCreateRequest request);

    /**
     * 将更新请求的字段复制到现有实体
     *
     * @param request 更新请求DTO
     * @param entity 现有实体
     */
    void updateEntity(PetUpdateRequest request, @MappingTarget Pet entity);

    /**
     * 将Pet实体转换为响应DTO
     *
     * @param entity Pet实体
     * @return PetResponse响应DTO
     */
    PetResponse toResponse(Pet entity);

    /**
     * 将Pet实体转换为视图对象
     *
     * @param entity Pet实体
     * @return PetVO视图对象
     */
    PetVO toVO(Pet entity);
}
