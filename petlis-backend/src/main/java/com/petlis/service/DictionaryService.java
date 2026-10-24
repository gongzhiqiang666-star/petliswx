package com.petlis.service;

import com.petlis.dto.response.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数据字典服务接口
 *
 * 提供系统中所有字典数据的查询功能
 * 所有字典查询都只返回启用状态(isActive=1)的数据
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
public interface DictionaryService {

    /**
     * 获取所有动物物种列表
     *
     * @return 动物物种响应列表
     */
    List<AnimalResponse> getAnimals();

    /**
     * 获取品种列表
     *
     * 支持根据动物物种ID筛选对应的品种
     *
     * @param infoAnimalId 动物物种ID,可选参数
     * @return 品种响应列表
     */
    List<VarietyResponse> getVarieties(Integer infoAnimalId);

    /**
     * 获取所有性别列表
     *
     * @return 性别响应列表
     */
    List<SexResponse> getSexes();

    /**
     * 获取所有样本类型列表
     *
     * @return 样本类型响应列表
     */
    List<SampleTypeResponse> getSampleTypes();

    /**
     * 获取所有检测项目列表
     *
     * @return 检测项目响应列表
     */
    List<TestTypeResponse> getTestTypes();

    /**
     * 分页查询检测项目（支持关键词搜索）
     *
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    Page<TestTypeResponse> getTestTypesPaginated(String keyword, int page, int size);

    /**
     * 获取检测项目价格
     *
     * 支持根据城市ID获取对应城市的价格
     *
     * @param testTypeId 检测项目ID
     * @param cityId 城市ID,默认为0(基础价格)
     * @return 检测项目价格响应
     */
    TestTypePriceResponse getTestTypePrice(Integer testTypeId, Integer cityId);

    /**
     * 获取所有城市列表
     *
     * @return 城市响应列表
     */
    List<CityResponse> getCities();

    /**
     * 获取所有医院列表
     *
     * 注意: 此接口主要用于管理后台,小程序端可能不需要
     *
     * @return 医院响应列表
     */
    List<HospitalResponse> getHospitals();

    /**
     * 根据ID获取单个医院信息
     *
     * 按需获取医院信息，避免一次性返回所有医院数据
     *
     * @param hospitalId 医院ID
     * @return 医院响应信息，如果不存在或未启用则返回null
     */
    HospitalResponse getHospitalById(Integer hospitalId);
}
