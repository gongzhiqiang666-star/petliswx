package com.petlis.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.text.StringEscapeUtils;

/**
 * XSS攻击防护请求包装器
 * 对请求参数中的HTML标签进行转义,防止XSS攻击
 *
 * @author PETLIS
 * @since 1.0.0
 */
public class XssWrapper extends HttpServletRequestWrapper {

    /**
     * 构造函数
     *
     * @param request 原始请求对象
     */
    public XssWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 获取参数值并进行XSS过滤
     *
     * @param name 参数名
     * @return 过滤后的参数值
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return cleanXss(value);
    }

    /**
     * 获取参数值数组并进行XSS过滤
     *
     * @param name 参数名
     * @return 过滤后的参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }

        String[] cleanValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            cleanValues[i] = cleanXss(values[i]);
        }
        return cleanValues;
    }

    /**
     * 获取请求头并进行XSS过滤
     *
     * @param name 请求头名称
     * @return 过滤后的请求头值
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return cleanXss(value);
    }

    /**
     * 清理XSS攻击代码
     * 对HTML特殊字符进行转义
     *
     * @param value 原始值
     * @return 清理后的值
     */
    private String cleanXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 使用Apache Commons Text进行HTML转义
        // 转义: < > & " '
        String cleanValue = StringEscapeUtils.escapeHtml4(value);

        // 额外处理:转义JavaScript特殊字符
        cleanValue = cleanValue
                .replaceAll("javascript:", "")
                .replaceAll("vbscript:", "")
                .replaceAll("onload=", "")
                .replaceAll("onerror=", "")
                .replaceAll("onclick=", "")
                .replaceAll("onmouseover=", "")
                .replaceAll("onfocus=", "")
                .replaceAll("onblur=", "");

        return cleanValue;
    }
}
