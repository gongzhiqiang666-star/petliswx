package com.petlis.entity.business;

import com.petlis.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * 对应数据库表: lin_user
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "lin_user")
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (主键)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /**
     * 组别ID
     */
    @Column(name = "group_id", nullable = false, columnDefinition = "int unsigned")
    private Integer groupId;

    /**
     * 用户名
     */
    @Column(name = "username", length = 32)
    private String username;

    /**
     * 昵称
     */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /**
     * 密码
     */
    @Column(name = "password", length = 32)
    private String password;

    /**
     * 密码盐
     */
    @Column(name = "salt", length = 30)
    private String salt;

    /**
     * 电子邮箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 手机号
     */
    @Column(name = "mobile", length = 11)
    private String mobile;

    /**
     * 头像URL
     */
    @Column(name = "avatar", length = 255)
    private String avatar;

    /**
     * 等级
     */
    @Column(name = "level", nullable = false, columnDefinition = "tinyint unsigned")
    private Integer level;

    /**
     * 性别 (0=未知, 1=男, 2=女)
     */
    @Column(name = "gender", nullable = false, columnDefinition = "tinyint unsigned")
    private Integer gender;

    /**
     * 生日
     */
    @Column(name = "birthday")
    private String birthday;

    /**
     * 格言
     */
    @Column(name = "bio", length = 100)
    private String bio;

    /**
     * 余额
     */
    @Column(name = "money", nullable = false, precision = 10, scale = 2)
    private String money;

    /**
     * 积分
     */
    @Column(name = "score", nullable = false)
    private Integer score;

    /**
     * 连续登录天数
     */
    @Column(name = "successions", nullable = false, columnDefinition = "int unsigned")
    private Integer successions;

    /**
     * 最大连续登录天数
     */
    @Column(name = "maxsuccessions", nullable = false, columnDefinition = "int unsigned")
    private Integer maxsuccessions;

    /**
     * 上次登录时间
     */
    @Column(name = "prevtime")
    private Integer prevtime;

    /**
     * 登录时间
     */
    @Column(name = "logintime")
    private Integer logintime;

    /**
     * 登录IP
     */
    @Column(name = "loginip", length = 50)
    private String loginip;

    /**
     * 失败次数
     */
    @Column(name = "loginfailure", nullable = false, columnDefinition = "tinyint unsigned")
    private Integer loginfailure;

    /**
     * 加入IP
     */
    @Column(name = "joinip", length = 50)
    private String joinip;

    /**
     * 加入时间
     */
    @Column(name = "jointime")
    private Integer jointime;

    /**
     * 创建时间(Unix时间戳)
     */
    @Column(name = "createtime")
    private Integer createtime;

    /**
     * 更新时间(Unix时间戳)
     */
    @Column(name = "updatetime")
    private Integer updatetime;

    /**
     * Token
     */
    @Column(name = "token", length = 50)
    private String token;

    /**
     * 状态 (normal=正常, hidden=隐藏)
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /**
     * 验证信息
     */
    @Column(name = "verification", length = 255)
    private String verification;

    /**
     * 微信openid (用于微信小程序登录)
     */
    @Column(name = "openid", unique = true, length = 100)
    private String openid;

    /**
     * 医院ID (关联lin_lb_infohospital表)
     */
    @Column(name = "hospital_id", columnDefinition = "int unsigned")
    private Integer hospitalId;
}
