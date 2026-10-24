package com.petlis.repository;

import com.petlis.entity.dict.DictSex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 性别字典Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface DictSexRepository extends JpaRepository<DictSex, Integer>, JpaSpecificationExecutor<DictSex> {

    /**
     * 查询所有启用的性别选项
     *
     * @return 性别列表
     */
    @Query("SELECT ds FROM DictSex ds WHERE ds.isActive = 1 ORDER BY ds.dictSexId")
    List<DictSex> findAllActive();
}
