package com.petlis.common.exception;

import lombok.Getter;

/**
 * 资源未找到异常
 *
 * 当请求的资源不存在时抛出此异常
 *
 * @author PETLIS Team
 */
@Getter
public class ResourceNotFoundException extends BusinessException {

    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(404, String.format("%s not found with id: %s", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(404, message);
        this.resourceName = null;
        this.resourceId = null;
    }
}
