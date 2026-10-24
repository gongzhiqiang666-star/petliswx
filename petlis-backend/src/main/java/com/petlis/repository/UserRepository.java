package com.petlis.repository;

import com.petlis.entity.business.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户Repository接口
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    /**
     * 根据微信openid查询用户
     *
     * @param openid 微信openid
     * @return 用户信息
     */
    Optional<User> findByOpenid(String openid);

    /**
     * 根据医院ID查询用户列表
     *
     * @param hospitalId 医院ID
     * @return 用户列表
     */
    @Query("SELECT u FROM User u WHERE u.hospitalId = :hospitalId AND u.status = 'normal'")
    List<User> findByHospitalId(@Param("hospitalId") Integer hospitalId);

    /**
     * 根据手机号查询用户
     *
     * @param mobile 手机号
     * @return 用户信息
     */
    Optional<User> findByMobile(String mobile);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查openid是否存在
     *
     * @param openid 微信openid
     * @return 是否存在
     */
    boolean existsByOpenid(String openid);

    /**
     * 根据医院ID和组ID查询第一个用户
     *
     * @param hospitalId 医院ID
     * @param groupId 组ID
     * @return 用户信息
     */
    Optional<User> findFirstByHospitalIdAndGroupId(Integer hospitalId, Integer groupId);

    /**
     * 检查手机号是否存在
     *
     * @param mobile 手机号
     * @return 是否存在
     */
    boolean existsByMobile(String mobile);
}
