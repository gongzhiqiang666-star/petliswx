package com.petlis.controller.api;

import com.petlis.common.response.Result;
import com.petlis.dto.response.*;
import com.petlis.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据字典控制器
 *
 * 提供系统中所有字典数据的查询API接口
 * 所有接口都只返回启用状态(isActive=1)的数据
 * 使用Caffeine缓存提高查询性能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("dictionary")
@RequiredArgsConstructor
@Tag(name = "数据字典管理", description = "数据字典相关接口")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    /**
     * 获取所有动物物种列表
     *
     * @return 动物物种响应列表
     */
    @GetMapping("/animals")
    @Operation(summary = "获取动物物种列表", description = "获取所有启用的动物物种列表")
    public Result<List<AnimalResponse>> getAnimals() {
        log.info("收到获取动物物种列表请求");

        try {
            List<AnimalResponse> list = dictionaryService.getAnimals();
            log.info("获取动物物种列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取动物物种列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取品种列表
     *
     * 支持根据动物物种ID筛选对应的品种
     *
     * @param animalId 动物物种ID,可选参数
     * @return 品种响应列表
     */
    @GetMapping("/varieties")
    @Operation(summary = "获取品种列表", description = "获取品种列表,支持按物种筛选")
    public Result<List<VarietyResponse>> getVarieties(
            @Parameter(description = "动物物种ID(可选)")
            @RequestParam(value = "animalId", required = false) Integer animalId) {
        log.info("收到获取品种列表请求, animalId: {}", animalId);

        try {
            List<VarietyResponse> list = dictionaryService.getVarieties(animalId);
            log.info("获取品种列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取品种列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有性别列表
     *
     * @return 性别响应列表
     */
    @GetMapping("/sexes")
    @Operation(summary = "获取性别列表", description = "获取所有启用的性别列表")
    public Result<List<SexResponse>> getSexes() {
        log.info("收到获取性别列表请求");

        try {
            List<SexResponse> list = dictionaryService.getSexes();
            log.info("获取性别列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取性别列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有样本类型列表
     *
     * @return 样本类型响应列表
     */
    @GetMapping("/sample-types")
    @Operation(summary = "获取样本类型列表", description = "获取所有启用的样本类型列表")
    public Result<List<SampleTypeResponse>> getSampleTypes() {
        log.info("收到获取样本类型列表请求");

        try {
            List<SampleTypeResponse> list = dictionaryService.getSampleTypes();
            log.info("获取样本类型列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取样本类型列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有检测项目列表
     *
     * @return 检测项目响应列表
     */
    @GetMapping("/test-types")
    @Operation(summary = "获取检测项目列表", description = "获取所有启用的检测项目列表")
    public Result<List<TestTypeResponse>> getTestTypes() {
        log.info("收到获取检测项目列表请求");

        try {
            List<TestTypeResponse> list = dictionaryService.getTestTypes();
            log.info("获取检测项目列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取检测项目列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询检测项目（支持关键词搜索）
     *
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始，默认0）
     * @param size 每页大小（默认20）
     * @return 分页结果
     */
    @GetMapping("/test-types/paginated")
    @Operation(summary = "分页查询检测项目", description = "分页查询检测项目，支持关键词搜索")
    public Result<?> getTestTypesPaginated(
            @Parameter(description = "搜索关键词（可选）")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "页码（从0开始，默认0）")
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Parameter(description = "每页大小（默认20）")
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        log.info("收到分页查询检测项目请求, keyword: {}, page: {}, size: {}", keyword, page, size);

        try {
            var pageResult = dictionaryService.getTestTypesPaginated(keyword, page, size);

            // 将Spring Data JPA的Page对象转换为Map，避免序列化问题
            var result = Map.of(
                    "content", pageResult.getContent(),
                    "totalElements", pageResult.getTotalElements(),
                    "totalPages", pageResult.getTotalPages(),
                    "size", pageResult.getSize(),
                    "number", pageResult.getNumber(),
                    "first", pageResult.isFirst(),
                    "last", pageResult.isLast(),
                    "empty", pageResult.isEmpty()
            );

            log.info("分页查询检测项目成功, total: {}, current: {}",
                    pageResult.getTotalElements(), pageResult.getContent().size());
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("分页查询检测项目失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取检测项目价格
     *
     * 从token中获取城市ID，查询对应城市的检测项目价格
     *
     * @param testTypeId 检测项目ID
     * @return 检测项目价格响应
     */
    @GetMapping("/test-types/{testTypeId}/price")
    @Operation(summary = "获取检测项目价格", description = "根据检测项目ID和当前用户城市ID获取价格")
    public Result<TestTypePriceResponse> getTestTypePrice(
            @Parameter(description = "检测项目ID", required = true)
            @PathVariable("testTypeId") Integer testTypeId) {

        // 从token中获取城市ID
        Integer cityId = com.petlis.common.security.SecurityUtils.getCurrentCityId();

        log.info("收到获取检测项目价格请求, testTypeId: {}, cityId: {}", testTypeId, cityId);

        try {
            TestTypePriceResponse response = dictionaryService.getTestTypePrice(testTypeId, cityId);
            if (response == null) {
                log.warn("未找到检测项目价格, testTypeId: {}, cityId: {}", testTypeId, cityId);
                return Result.fail("未找到对应的价格信息");
            }
            log.info("获取检测项目价格成功, price: {}", response.getPrice());
            return Result.success("查询成功", response);
        } catch (Exception e) {
            log.error("获取检测项目价格失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有城市列表
     *
     * @return 城市响应列表
     */
    @GetMapping("/cities")
    @Operation(summary = "获取城市列表", description = "获取所有启用的城市列表")
    public Result<List<CityResponse>> getCities() {
        log.info("收到获取城市列表请求");

        try {
            List<CityResponse> list = dictionaryService.getCities();
            log.info("获取城市列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取城市列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有医院列表
     *
     * 注意: 此接口主要用于管理后台
     *
     * @return 医院响应列表
     */
    @GetMapping("/hospitals")
    @Operation(summary = "获取医院列表", description = "获取所有启用的医院列表(主要用于管理后台)")
    public Result<List<HospitalResponse>> getHospitals() {
        log.info("收到获取医院列表请求");

        try {
            List<HospitalResponse> list = dictionaryService.getHospitals();
            log.info("获取医院列表成功, count: {}", list.size());
            return Result.success("查询成功", list);
        } catch (Exception e) {
            log.error("获取医院列表失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取单个医院信息
     *
     * 按需获取医院信息，避免一次性返回所有医院数据
     * 主要用于小程序端查询当前用户所属的医院信息
     *
     * @param id 医院ID
     * @return 医院响应信息
     */
    @GetMapping("/hospitals/{id}")
    @Operation(summary = "根据ID获取医院信息", description = "根据医院ID获取单个医院信息(按需查询)")
    public Result<HospitalResponse> getHospitalById(
            @Parameter(description = "医院ID", required = true)
            @PathVariable("id") Integer id) {
        log.info("收到根据ID获取医院信息请求, hospitalId: {}", id);

        try {
            HospitalResponse response = dictionaryService.getHospitalById(id);

            if (response == null) {
                log.warn("未找到医院信息或医院未启用, hospitalId: {}", id);
                return Result.fail("未找到对应医院信息");
            }

            log.info("根据ID获取医院信息成功, hospitalName: {}", response.getHospitalName());
            return Result.success("查询成功", response);
        } catch (Exception e) {
            log.error("根据ID获取医院信息失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }
}
