package com.petlis.mapper;

import com.petlis.dto.response.NotificationResponse;
import com.petlis.entity.business.NotificationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 通知记录MapStruct映射器
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    /**
     * 将通知记录实体转换为响应DTO
     *
     * @param record 通知记录实体
     * @return 通知响应DTO
     */
    @Mapping(target = "hospitalName", ignore = true)
    @Mapping(target = "oldStatusLabel", ignore = true)
    @Mapping(target = "newStatusLabel", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "wechatSentStatusLabel", ignore = true)
    NotificationResponse toResponse(NotificationRecord record);
}
