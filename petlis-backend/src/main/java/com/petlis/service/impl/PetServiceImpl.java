package com.petlis.service.impl;

import com.petlis.common.exception.BusinessException;
import com.petlis.common.exception.ResourceNotFoundException;
import com.petlis.dto.request.PetCreateRequest;
import com.petlis.dto.request.PetQueryRequest;
import com.petlis.dto.request.PetUpdateRequest;
import com.petlis.dto.response.PetResponse;
import com.petlis.entity.business.Pet;
import com.petlis.entity.dict.DictSex;
import com.petlis.entity.dict.InfoAnimal;
import com.petlis.entity.dict.InfoVariety;
import com.petlis.mapper.PetMapper;
import com.petlis.repository.DictSexRepository;
import com.petlis.repository.InfoAnimalRepository;
import com.petlis.repository.InfoVarietyRepository;
import com.petlis.repository.PetRepository;
import com.petlis.service.PetService;
import com.petlis.vo.PetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 宠物档案服务实现类
 *
 * 实现宠物档案的CRUD操作、搜索查询等功能
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final InfoAnimalRepository infoAnimalRepository;
    private final InfoVarietyRepository infoVarietyRepository;
    private final DictSexRepository dictSexRepository;
    private final PetMapper petMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetResponse create(PetCreateRequest request, Integer hospitalId) {
        log.info("创建宠物档案, hospitalId: {}, animalName: {}", hospitalId, request.getAnimalName());

        // 1. 验证物种是否存在
        Optional<InfoAnimal> animal = infoAnimalRepository.findById(request.getInfoAnimalId());
        if (animal.isEmpty() || animal.get().getIsActive() == 0) {
            throw new BusinessException("指定的物种不存在或已禁用");
        }

        // 2. 验证品种是否存在(如果提供了品种ID)
        if (request.getInfoVarietyId() != null) {
            Optional<InfoVariety> variety = infoVarietyRepository.findById(request.getInfoVarietyId().longValue());
            if (variety.isEmpty()) {
                throw new BusinessException("指定的品种不存在");
            }
        }

        // 3. 验证性别是否存在
        Optional<DictSex> sex = dictSexRepository.findById(request.getDictSexId());
        if (sex.isEmpty() || sex.get().getIsActive() == 0) {
            throw new BusinessException("指定的性别不存在或已禁用");
        }

        // 4. 转换为实体并设置额外字段
        Pet pet = petMapper.toEntity(request);
        int currentTime = (int) Instant.now().getEpochSecond();

        pet.setInfoHospitalId(hospitalId);
        pet.setIsActive(1);
        pet.setCreatetime(currentTime);
        pet.setUpdatetime(currentTime);

        // 5. 保存宠物档案
        Pet savedPet = petRepository.save(pet);
        log.info("宠物档案创建成功, id: {}", savedPet.getId());

        // 6. 转换为响应DTO并返回
        return petMapper.toResponse(savedPet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetResponse update(Integer petId, PetUpdateRequest request, Integer hospitalId) {
        log.info("更新宠物档案, id: {}, hospitalId: {}", petId, hospitalId);

        // 1. 查询宠物档案是否存在
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("宠物档案", petId));

        // 2. 验证权限(只能更新本医院的宠物档案)
        if (!pet.getInfoHospitalId().equals(hospitalId)) {
            throw new BusinessException("无权限更新此宠物档案");
        }

        // 3. 验证物种是否存在(如果提供了物种ID)
        if (request.getInfoAnimalId() != null) {
            Optional<InfoAnimal> animal = infoAnimalRepository.findById(request.getInfoAnimalId());
            if (animal.isEmpty() || animal.get().getIsActive() == 0) {
                throw new BusinessException("指定的物种不存在或已禁用");
            }
        }

        // 4. 验证品种是否存在(如果提供了品种ID)
        if (request.getInfoVarietyId() != null) {
            Optional<InfoVariety> variety = infoVarietyRepository.findById(request.getInfoVarietyId().longValue());
            if (variety.isEmpty()) {
                throw new BusinessException("指定的品种不存在");
            }
        }

        // 5. 验证性别是否存在(如果提供了性别ID)
        if (request.getDictSexId() != null) {
            Optional<DictSex> sex = dictSexRepository.findById(request.getDictSexId());
            if (sex.isEmpty() || sex.get().getIsActive() == 0) {
                throw new BusinessException("指定的性别不存在或已禁用");
            }
        }

        // 6. 更新实体字段
        petMapper.updateEntity(request, pet);
        pet.setUpdatetime((int) Instant.now().getEpochSecond());

        // 7. 保存更新
        Pet updatedPet = petRepository.save(pet);
        log.info("宠物档案更新成功, id: {}", petId);

        // 8. 转换为响应DTO并返回
        return petMapper.toResponse(updatedPet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer petId, Integer hospitalId) {
        log.info("删除宠物档案, id: {}, hospitalId: {}", petId, hospitalId);

        // 1. 查询宠物档案是否存在
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("宠物档案", petId));

        // 2. 验证权限(只能删除本医院的宠物档案)
        if (!pet.getInfoHospitalId().equals(hospitalId)) {
            throw new BusinessException("无权限删除此宠物档案");
        }

        // 3. 逻辑删除
        pet.setIsActive(0);
        pet.setUpdatetime((int) Instant.now().getEpochSecond());
        petRepository.save(pet);

        log.info("宠物档案删除成功, id: {}", petId);
    }

    @Override
    public PetVO queryById(Integer petId, Integer hospitalId) {
        log.info("查询宠物档案详情, id: {}, hospitalId: {}", petId, hospitalId);

        // 1. 查询宠物档案
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("宠物档案", petId));

        // 2. 验证权限(只能查看本医院的宠物档案)
        if (!pet.getInfoHospitalId().equals(hospitalId)) {
            throw new BusinessException("无权限查看此宠物档案");
        }

        // 3. 转换为VO并填充关联数据
        return enrichPetVO(pet);
    }

    @Override
    public Page<PetVO> query(PetQueryRequest request, Integer hospitalId) {
        log.info("分页查询宠物档案, hospitalId: {}, page: {}, size: {}, sort: {}",
                hospitalId, request.getPage(), request.getSize(), request.getSort());

        // 1. 构建分页参数，包含排序
        Pageable pageable;
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            // 解析排序参数: "createtime,desc" -> Sort.by(Sort.Direction.DESC, "createtime")
            String[] sortParams = request.getSort().split(",");
            Sort sort = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
            pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        } else {
            // 默认按创建时间倒序
            Sort sort = Sort.by(Sort.Direction.DESC, "createtime");
            pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        }

        // 2. 分页查询
        Page<Pet> petPage = petRepository.findByInfoHospitalId(hospitalId, pageable);

        log.info("查询到 {} 条宠物档案，当前页返回 {} 条",
                petPage.getTotalElements(), petPage.getNumberOfElements());

        // 3. 转换为VO并填充关联数据
        return petPage.map(this::enrichPetVO);
    }

    @Override
    public List<PetVO> search(String name, Integer hospitalId) {
        log.info("搜索宠物档案, hospitalId: {}, keyword: {}", hospitalId, name);

        // 1. 模糊搜索
        List<Pet> pets = petRepository.findByInfoHospitalIdAndAnimalNameContaining(hospitalId, name);

        // 2. 转换为VO并填充关联数据
        return pets.stream()
                .map(this::enrichPetVO)
                .collect(Collectors.toList());
    }

    /**
     * 填充宠物VO的关联数据
     *
     * @param pet 宠物实体
     * @return 包含关联数据的宠物VO
     */
    private PetVO enrichPetVO(Pet pet) {
        PetVO vo = petMapper.toVO(pet);

        // 填充物种信息
        if (pet.getInfoAnimalId() != null) {
            infoAnimalRepository.findById(pet.getInfoAnimalId())
                    .ifPresent(animal -> {
                        vo.setInfoAnimalName(animal.getAnimalName());
                        vo.setInfoAnimalNameEn(animal.getAnimalNameEn());
                    });
        }

        // 填充品种信息
        if (pet.getInfoVarietyId() != null) {
            infoVarietyRepository.findById(pet.getInfoVarietyId().longValue())
                    .ifPresent(variety -> {
                        vo.setInfoVarietyName(variety.getVarietyCn());
                        vo.setInfoVarietyNameEn(variety.getVarietyEn());
                    });
        }

        // 填充性别信息
        if (pet.getDictSexId() != null) {
            dictSexRepository.findById(pet.getDictSexId())
                    .ifPresent(sex -> {
                        vo.setDictSexName(sex.getSexName());
                        vo.setDictSexNameEn(sex.getSexNameEn());
                    });
        }

        return vo;
    }
}
