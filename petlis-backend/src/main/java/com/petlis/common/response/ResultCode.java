package com.petlis.common.response;

/**
 * 响应码接口
 *
 * @author PETLIS Team
 */
public interface ResultCode {

    /**
     * 获取响应码
     */
    Integer getCode();

    /**
     * 获取响应消息
     */
    String getMessage();
}
