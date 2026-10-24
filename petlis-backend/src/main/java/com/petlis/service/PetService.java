package com.petlis.service;

import com.petlis.dto.request.PetCreateRequest;
import com.petlis.dto.request.PetQueryRequest;
import com.petlis.dto.request.PetUpdateRequest;
import com.petlis.dto.response.PetResponse;
import com.petlis.vo.PetVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 宠物档案服务接口
 *
 * 提供宠物档案的CRUD操作、搜索查询等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public interface PetService {

    /**
     * 创建宠物档案
     *
     * @param request 创建请求DTO
     * @param hospitalId 当前用户所属医院ID
     * @return 宠物档案响应DTO
     */
    PetResponse create(PetCreateRequest request, Integer hospitalId);

    /**
     * 更新宠物档案
     *
     * @param petId 宠物档案ID
     * @param request 更新请求DTO
     * @param hospitalId 当前用户所属医院ID(用于权限验证)
     * @return 宠物档案响应DTO
     */
    PetResponse update(Integer petId, PetUpdateRequest request, Integer hospitalId);

    /**
     * 删除宠物档案(逻辑删除)
     *
     * @param petId 宠物档案ID
     * @param hospitalId 当前用户所属医院ID(用于权限验证)
     */
    void delete(Integer petId, Integer hospitalId);

    /**
     * 根据ID查询宠物档案
     *
     * @param petId 宠物档案ID
     * @param hospitalId 当前用户所属医院ID(用于权限验证)
     * @return 宠物档案视图对象
     */
    PetVO queryById(Integer petId, Integer hospitalId);

    /**
     * 分页查询宠物档案
     *
     * @param request 查询请求DTO
     * @param hospitalId 当前用户所属医院ID(用于数据隔离)
     * @return 宠物档案视图对象分页结果
     */
    Page<PetVO> query(PetQueryRequest request, Integer hospitalId);

    /**
     * 搜索宠物档案
     *
     * 根据宠物中文名或英文名进行模糊搜索
     *
     * @param name 搜索关键词
     * @param hospitalId 当前用户所属医院ID(用于数据隔离)
     * @return 宠物档案视图对象列表
     */
    List<PetVO> search(String name, Integer hospitalId);
}
