package com.petlis.controller.api;

import com.petlis.common.annotation.OperationLog;
import com.petlis.common.response.Result;
import com.petlis.common.security.SecurityUtils;
import com.petlis.dto.request.PetCreateRequest;
import com.petlis.dto.request.PetQueryRequest;
import com.petlis.dto.request.PetUpdateRequest;
import com.petlis.dto.response.PetResponse;
import com.petlis.service.PetService;
import com.petlis.vo.PetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 宠物档案控制器
 *
 * 提供宠物档案的CRUD操作API接口
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("pets")
@RequiredArgsConstructor
@Tag(name = "宠物档案管理", description = "宠物档案相关接口")
public class PetController {

    private final PetService petService;
    private final com.petlis.service.impl.PetHealthService petHealthService;

    @GetMapping("/{id}/health")
    @Operation(summary = "查询宠物病史与既往送检报告")
    public Result<Map<String, Object>> health(@PathVariable("id") Integer petId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(petHealthService.health(petId, page, size));
    }

    /**
     * 创建宠物档案
     *
     * @param request 创建请求DTO
     * @return 宠物档案响应DTO
     */
    @PostMapping
    @Operation(summary = "创建宠物档案", description = "创建新的宠物档案")
    @OperationLog(module = "宠物档案管理", operation = "创建", description = "创建宠物档案")
    public Result<PetResponse> create(@Valid @RequestBody PetCreateRequest request) {
        log.info("收到创建宠物档案请求, animalName: {}, ownerName: {}",
                request.getAnimalName(), request.getOwnerName());

        try {
            // 从SecurityContext获取当前用户的医院ID
            Integer hospitalId = SecurityUtils.getCurrentHospitalId();
            PetResponse response = petService.create(request, hospitalId);

            log.info("宠物档案创建成功, id: {}", response.getId());
            return Result.success("创建成功", response);
        } catch (Exception e) {
            log.error("创建宠物档案失败: {}", e.getMessage(), e);
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新宠物档案
     *
     * @param petId 宠物档案ID
     * @param request 更新请求DTO
     * @return 宠物档案响应DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新宠物档案", description = "更新指定ID的宠物档案信息")
    @OperationLog(module = "宠物档案管理", operation = "更新", description = "更新宠物档案")
    public Result<PetResponse> update(
            @Parameter(description = "宠物档案ID", required = true)
            @PathVariable("id") Integer petId,
            @Valid @RequestBody PetUpdateRequest request) {
        log.info("收到更新宠物档案请求, id: {}", petId);

        try {
            // 从SecurityContext获取当前用户的医院ID
            Integer hospitalId = SecurityUtils.getCurrentHospitalId();
            PetResponse response = petService.update(petId, request, hospitalId);

            log.info("宠物档案更新成功, id: {}", petId);
            return Result.success("更新成功", response);
        } catch (Exception e) {
            log.error("更新宠物档案失败: {}", e.getMessage(), e);
            return Result.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除宠物档案
     *
     * @param petId 宠物档案ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除宠物档案", description = "逻辑删除指定ID的宠物档案")
    @OperationLog(module = "宠物档案管理", operation = "删除", description = "删除宠物档案")
    public Result<Void> delete(
            @Parameter(description = "宠物档案ID", required = true)
            @PathVariable("id") Integer petId) {
        log.info("收到删除宠物档案请求, id: {}", petId);

        try {
            // 从SecurityContext获取当前用户的医院ID
            Integer hospitalId = SecurityUtils.getCurrentHospitalId();
            petService.delete(petId, hospitalId);

            log.info("宠物档案删除成功, id: {}", petId);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除宠物档案失败: {}", e.getMessage(), e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 查询宠物档案详情
     *
     * @param petId 宠物档案ID
     * @return 宠物档案视图对象
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询宠物档案详情", description = "根据ID查询宠物档案详细信息")
    @OperationLog(module = "宠物档案管理", operation = "查询", description = "查询宠物档案详情")
    public Result<PetVO> queryById(
            @Parameter(description = "宠物档案ID", required = true)
            @PathVariable("id") Integer petId) {
        log.info("收到查询宠物档案详情请求, id: {}", petId);

        try {
            // 从SecurityContext获取当前用户的医院ID
            Integer hospitalId = SecurityUtils.getCurrentHospitalId();
            PetVO vo = petService.queryById(petId, hospitalId);

            log.info("查询宠物档案详情成功, id: {}", petId);
            return Result.success("查询成功", vo);
        } catch (Exception e) {
            log.error("查询宠物档案详情失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询宠物档案列表
     *
     * @param request 查询请求DTO
     * @return 宠物档案视图对象分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询宠物档案列表", description = "分页查询当前医院的宠物档案列表")
    @OperationLog(module = "宠物档案管理", operation = "查询", description = "分页查询宠物档案列表")
    public Result<Page<PetVO>> query(PetQueryRequest request) {
        log.info("收到分页查询宠物档案请求, page: {}, size: {}, sort: {}",
                request.getPage(), request.getSize(), request.getSort());

        try {
            // 从SecurityContext获取当前用户的医院ID
            Integer hospitalId = SecurityUtils.getCurrentHospitalId();
            Page<PetVO> page = petService.query(request, hospitalId);

            log.info("分页查询宠物档案成功, total: {}", page.getTotalElements());
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("分页查询宠物档案失败: {}", e.getMessage(), e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 搜索宠物档案
     *
     * @param name 搜索关键词(宠物中文名或英文名)
     * @return 宠物档案视图对象列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索宠物档案", description = "根据宠物名称模糊搜索")
    @OperationLog(module = "宠物档案管理", operation = "搜索", description = "搜索宠物档案")
    public Result<java.util.List<PetVO>> search(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam("name") String name) {
        log.info("收到搜索宠物档案请求, keyword: {}", name);

        try {
            // 从SecurityContext获取当前用户的医院ID
            Integer hospitalId = SecurityUtils.getCurrentHospitalId();
            java.util.List<PetVO> list = petService.search(name, hospitalId);

            log.info("搜索宠物档案成功, count: {}", list.size());
            return Result.success("搜索成功", list);
        } catch (Exception e) {
            log.error("搜索宠物档案失败: {}", e.getMessage(), e);
            return Result.fail("搜索失败: " + e.getMessage());
        }
    }
}
