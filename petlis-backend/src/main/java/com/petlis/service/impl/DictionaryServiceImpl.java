package com.petlis.service.impl;

import com.petlis.dto.response.*;
import com.petlis.entity.dict.*;
import com.petlis.repository.*;
import com.petlis.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 数据字典服务实现类
 *
 * 提供系统中所有字典数据的查询功能
 * 使用Caffeine缓存提高查询性能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {

    private final InfoAnimalRepository animalRepository;
    private final InfoVarietyRepository varietyRepository;
    private final DictSexRepository sexRepository;
    private final InfoSampleTypeRepository sampleTypeRepository;
    private final InfoTestTypeRepository testTypeRepository;
    private final InfoTestTypePriceRepository testTypePriceRepository;
    private final CityRepository cityRepository;
    private final InfoHospitalRepository hospitalRepository;

    /**
     * 获取所有动物物种列表
     *
     * 只返回启用状态(isActive=1)的物种
     * 使用缓存提高查询性能
     *
     * @return 动物物种响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary", key = "'getAnimals'")
    public List<AnimalResponse> getAnimals() {
        log.debug("查询动物物种列表");
        return animalRepository.findAllActive().stream()
                .map(this::toAnimalResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取品种列表
     *
     * 支持根据动物物种ID筛选对应的品种
     * 只返回启用状态的品种
     * 使用缓存提高查询性能
     *
     * @param infoAnimalId 动物物种ID,可选参数
     * @return 品种响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary",
               key = "'getVarieties_' + (#infoAnimalId != null ? #infoAnimalId : 'all')")
    public List<VarietyResponse> getVarieties(Integer infoAnimalId) {
        log.debug("查询品种列表, 动物物种ID: {}", infoAnimalId);

        // 注意: InfoVariety继承自BaseEntity,没有IsActive字段
        // 如果需要按物种筛选,需要在数据库表中有相关字段或关联关系
        // 这里暂时返回所有品种,后续可根据实际需求调整
        List<InfoVariety> varieties;

        if (infoAnimalId != null && infoAnimalId > 0) {
            // 如果有物种ID筛选需求,需要确认数据库表结构
            // 暂时返回所有品种
            varieties = varietyRepository.findAllOrderByVarietyCN();
            log.warn("品种按物种筛选功能待实现,当前返回所有品种");
        } else {
            varieties = varietyRepository.findAllOrderByVarietyCN();
        }

        return varieties.stream()
                .map(this::toVarietyResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有性别列表
     *
     * 只返回启用状态(isActive=1)的性别
     * 使用缓存提高查询性能
     *
     * @return 性别响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary", key = "'getSexes'")
    public List<SexResponse> getSexes() {
        log.debug("查询性别列表");
        return sexRepository.findAllActive().stream()
                .map(this::toSexResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有样本类型列表
     *
     * 只返回启用状态(isActive=1)的样本类型
     * 使用缓存提高查询性能
     *
     * @return 样本类型响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary", key = "'getSampleTypes'")
    public List<SampleTypeResponse> getSampleTypes() {
        log.debug("查询样本类型列表");
        return sampleTypeRepository.findAllActive().stream()
                .map(this::toSampleTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有检测项目列表
     *
     * 只返回启用状态(isActive=1)的检测项目
     * 使用缓存提高查询性能
     *
     * @return 检测项目响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary", key = "'getTestTypes'")
    public List<TestTypeResponse> getTestTypes() {
        log.debug("查询检测项目列表");
        return testTypeRepository.findAllActive().stream()
                .map(this::toTestTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询检测项目（支持关键词搜索）
     *
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TestTypeResponse> getTestTypesPaginated(String keyword, int page, int size) {
        log.debug("分页查询检测项目, 关键词: {}, 页码: {}, 每页: {}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("infoTestTypeId").ascending());
        Page<InfoTestType> testTypePage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // 有搜索关键词
            testTypePage = testTypeRepository.findByKeyword(keyword.trim(), pageable);
        } else {
            // 无搜索关键词
            testTypePage = testTypeRepository.findAllActive(pageable);
        }

        return testTypePage.map(this::toTestTypeResponse);
    }

    /**
     * 获取检测项目价格
     *
     * 支持根据城市ID获取对应城市的价格
     * 默认cityId=0表示基础价格
     * 使用缓存提高查询性能
     *
     * @param testTypeId 检测项目ID
     * @param cityId 城市ID,默认为0(基础价格)
     * @return 检测项目价格响应
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary",
               key = "'getTestTypePrice_' + #testTypeId + '_' + (#cityId != null ? #cityId : 0)")
    public TestTypePriceResponse getTestTypePrice(Integer testTypeId, Integer cityId) {
        log.debug("查询检测项目价格, 检测项目ID: {}, 城市ID: {}", testTypeId, cityId);

        if (testTypeId == null || testTypeId <= 0) {
            throw new IllegalArgumentException("检测项目ID不能为空");
        }

        // 默认城市ID为0
        Integer actualCityId = (cityId != null && cityId > 0) ? cityId : 0;

        // 查询检测项目价格
        // 注意: 当前价格表没有城市ID字段,这里查询基础价格
        // 后续如果需要支持城市价格,需要扩展表结构
        Optional<InfoTestTypePrice> priceOptional = testTypePriceRepository
                .findActivePriceByTestTypeId(testTypeId.longValue());

        if (priceOptional.isEmpty()) {
            log.warn("未找到检测项目价格, 检测项目ID: {}", testTypeId);
            return null;
        }

        return toTestTypePriceResponse(priceOptional.get());
    }

    /**
     * 获取所有城市列表
     *
     * 只返回启用状态(isActive=1)的城市
     * 使用缓存提高查询性能
     *
     * @return 城市响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "dictionary", key = "'getCities'")
    public List<CityResponse> getCities() {
        log.debug("查询城市列表");
        return cityRepository.findAllActive().stream()
                .map(this::toCityResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有医院列表
     *
     * 注意: 此接口主要用于管理后台
     * 只返回启用状态(isActive=1)的医院
     * 使用缓存提高查询性能
     *
     * @return 医院响应列表
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hospitals", key = "'getHospitals'")
    public List<HospitalResponse> getHospitals() {
        log.debug("查询医院列表");
        return hospitalRepository.findAllActive().stream()
                .map(this::toHospitalResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取单个医院信息
     *
     * 只返回启用状态(isActive=1)的医院
     * 使用缓存提高查询性能
     *
     * @param hospitalId 医院ID
     * @return 医院响应信息，如果不存在或未启用则返回null
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hospitals", key = "'getHospitalById_' + #hospitalId")
    public HospitalResponse getHospitalById(Integer hospitalId) {
        log.debug("根据ID查询医院信息, hospitalId: {}", hospitalId);

        if (hospitalId == null || hospitalId <= 0) {
            log.warn("医院ID无效: {}", hospitalId);
            return null;
        }

        // 使用JPA的标准findById方法
        Optional<InfoHospital> hospitalOptional = hospitalRepository.findById(hospitalId);

        if (hospitalOptional.isEmpty()) {
            log.warn("未找到医院信息, hospitalId: {}", hospitalId);
            return null;
        }

        InfoHospital hospital = hospitalOptional.get();

        // 检查医院是否启用
        if (hospital.getIsActive() == null || hospital.getIsActive() != 1) {
            log.warn("医院未启用, hospitalId: {}, isActive: {}", hospitalId, hospital.getIsActive());
            return null;
        }

        return toHospitalResponse(hospital);
    }

    // ==================== 实体转换方法 ====================

    /**
     * 将动物物种实体转换为响应DTO
     *
     * @param entity 动物物种实体
     * @return 动物物种响应DTO
     */
    private AnimalResponse toAnimalResponse(InfoAnimal entity) {
        AnimalResponse response = new AnimalResponse();
        response.setInfoAnimalId(entity.getInfoAnimalId());
        response.setAnimalName(entity.getAnimalName());
        response.setAnimalNameEn(entity.getAnimalNameEn());
        response.setIsActive(entity.getIsActive());
        response.setCreatetime(entity.getCreatetime());
        response.setUpdatetime(entity.getUpdatetime());
        return response;
    }

    /**
     * 将品种实体转换为响应DTO
     *
     * @param entity 品种实体
     * @return 品种响应DTO
     */
    private VarietyResponse toVarietyResponse(InfoVariety entity) {
        VarietyResponse response = new VarietyResponse();
        response.setIid(entity.getIid());
        response.setVarietyCn(entity.getVarietyCn());
        response.setVarietyEn(entity.getVarietyEn());
        response.setCreatetime(entity.getCreatetime());
        response.setUpdatetime(entity.getUpdatetime());
        return response;
    }

    /**
     * 将性别实体转换为响应DTO
     *
     * @param entity 性别实体
     * @return 性别响应DTO
     */
    private SexResponse toSexResponse(DictSex entity) {
        SexResponse response = new SexResponse();
        response.setDictSexId(entity.getDictSexId());
        response.setSexName(entity.getSexName());
        response.setSexNameEn(entity.getSexNameEn());
        response.setIsActive(entity.getIsActive());
        response.setCreatetime(entity.getCreatetime());
        response.setUpdatetime(entity.getUpdatetime());
        return response;
    }

    /**
     * 将样本类型实体转换为响应DTO
     *
     * @param entity 样本类型实体
     * @return 样本类型响应DTO
     */
    private SampleTypeResponse toSampleTypeResponse(InfoSampleType entity) {
        SampleTypeResponse response = new SampleTypeResponse();
        response.setInfoSampleTypeId(entity.getInfoSampleTypeId());
        response.setSampleName(entity.getSampleName());
        response.setSampleNameEn(entity.getSampleNameEn());
        response.setIsActive(entity.getIsActive());
        response.setCreatetime(entity.getCreatetime());
        response.setUpdatetime(entity.getUpdatetime());
        return response;
    }

    /**
     * 将检测项目实体转换为响应DTO
     *
     * @param entity 检测项目实体
     * @return 检测项目响应DTO
     */
    private TestTypeResponse toTestTypeResponse(InfoTestType entity) {
        TestTypeResponse response = new TestTypeResponse();
        response.setInfoTestTypeId(entity.getInfoTestTypeId());
        response.setTestName(entity.getTestName());
        response.setTestNO(entity.getTestNO());
        response.setPrice(entity.getPrice());
        response.setCtoG(entity.getCtoG());
        response.setGtoC(entity.getGtoC());
        response.setDiscountReal(entity.getDiscountReal());
        response.setDiscountNext(entity.getDiscountNext());
        response.setCreateDt(entity.getCreateDt());
        response.setIsActive(entity.getIsActive());
        response.setType(entity.getType());
        response.setUpdatetime(entity.getUpdatetime());
        response.setCreatetime(entity.getCreatetime());
        return response;
    }

    /**
     * 将检测项目价格实体转换为响应DTO
     *
     * @param entity 检测项目价格实体
     * @return 检测项目价格响应DTO
     */
    private TestTypePriceResponse toTestTypePriceResponse(InfoTestTypePrice entity) {
        TestTypePriceResponse response = new TestTypePriceResponse();
        response.setIid(entity.getIid() != null ? entity.getIid().longValue() : null);
        response.setInfoTestTypeid(entity.getInfoTestTypeId() != null ? entity.getInfoTestTypeId().longValue() : null);
        response.setPrice(entity.getPrice() != null ? java.math.BigDecimal.valueOf(entity.getPrice()) : null);
        response.setIsActive(entity.getIsActive());
        response.setCreatetime(entity.getCreatetime());
        response.setUpdatetime(entity.getUpdatetime());
        return response;
    }

    /**
     * 将城市实体转换为响应DTO
     *
     * @param entity 城市实体
     * @return 城市响应DTO
     */
    private CityResponse toCityResponse(City entity) {
        CityResponse response = new CityResponse();
        response.setCityId(entity.getCityId());
        response.setCityName(entity.getCityName());
        response.setAreaCode(entity.getAreaCode());
        response.setGrade(entity.getGrade());
        response.setZipCode(entity.getZipCode());
        response.setBelongCityCityId(entity.getBelongCityCityId());
        response.setIsActive(entity.getIsActive());
        return response;
    }

    /**
     * 将医院实体转换为响应DTO
     *
     * @param entity 医院实体
     * @return 医院响应DTO
     */
    private HospitalResponse toHospitalResponse(InfoHospital entity) {
        HospitalResponse response = new HospitalResponse();
        response.setInfoHospitalId(entity.getInfoHospitalId());
        response.setHospitalName(entity.getHospitalName());
        response.setHospitalNameEn(entity.getHospitalNameEn());
        response.setInvoice(entity.getInvoice());
        response.setCityId(entity.getCityId());
        response.setPlace(entity.getPlace());
        response.setAdress(entity.getAdress());
        response.setTel(entity.getTel());
        response.setFax(entity.getFax());
        response.setInfoHospitalContactId(entity.getInfoHospitalContactId());
        response.setCreateDt(entity.getCreateDt());
        response.setIsActive(entity.getIsActive());
        response.setLoginName(entity.getLoginName());
        response.setAmount(entity.getAmount());
        response.setPayType(entity.getPayType());
        return response;
    }
}
