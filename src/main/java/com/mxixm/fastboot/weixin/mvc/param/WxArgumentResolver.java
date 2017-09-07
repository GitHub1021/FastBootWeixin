package com.mxixm.fastboot.weixin.mvc.param;

import com.mxixm.fastboot.weixin.annotation.WxMapping;
import com.mxixm.fastboot.weixin.module.web.WxRequest;
import com.mxixm.fastboot.weixin.module.web.WxRequestBody;
import com.mxixm.fastboot.weixin.module.web.session.WxSession;
import com.mxixm.fastboot.weixin.mvc.WxWebUtils;
import com.mxixm.fastboot.weixin.support.WxUserProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

import javax.servlet.http.HttpServletRequest;

/**
 * FastBootWeixin  WxArgumentResolver
 *
 * @author Guangshan
 * @summary FastBootWeixin  WxArgumentResolver
 * @Copyright (c) 2017, Guangshan Group All Rights Reserved
 * @since 2017/8/12 22:51
 */
public class WxArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

    // 是否有更好的方式？有空参看源码
    private WxUserProvider wxUserProvider;

    /**
     * 从谁发的，也不要了，用一个处理
     */
    @Deprecated
    public static final String WX_FROM_USER = "fromUser";

    // 默认同上，另外一个参数名
    public static final String WX_USER = "wxUser";

    /**
     * 发给谁的，不需要了，用ResponseBodyAdvice处理这个
     */
    @Deprecated
    public static final String WX_TO_USER = "toUser";

    public WxArgumentResolver(WxUserProvider wxUserProvider) {
        super();
        this.wxUserProvider = wxUserProvider;
    }

    /**
     * @param beanFactory a bean factory used for resolving  ${...} placeholder
     *                    and #{...} SpEL expressions in default values, or {@code null} if default
     *                    values are not expected to contain expressions
     */
    public WxArgumentResolver(ConfigurableBeanFactory beanFactory) {
        super(beanFactory);
        this.wxUserProvider = beanFactory.getBean(WxUserProvider.class);
    }


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 只有method上有注解WxMapping时才支持解析
        return AnnotatedElementUtils.hasAnnotation(parameter.getMethod(), WxMapping.class);
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        return new RequestParamNamedValueInfo();
    }

    /**
     * 所有的转换都卸载了一起，有空可以分离为多个，抽象一层
     * @param name
     * @param parameter
     * @param request
     * @return dummy
     * @throws Exception
     */
    @Override
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        WxRequest wxRequest = WxWebUtils.getWxRequestFromRequestAttribute(servletRequest);
        // 类型匹配，直接返回
        if (parameter.getParameterType() == WxRequest.class) {
            return wxRequest;
        }
        if (parameter.getParameterType() == WxRequest.Body.class) {
            return wxRequest.getBody();
        }
        if (WxSession.class.isAssignableFrom(parameter.getParameterType())) {
            return wxRequest.getWxSession();
        }
        if (WxRequestBody.class.isAssignableFrom(parameter.getParameterType())) {
            return WxRequestBody.of((Class)parameter.getParameterType(), wxRequest.getBody());
        }

        // 如果可以获取用户则返回用户
        Object user = getUser(parameter, wxRequest);
        if (user != null) {
            return user;
        }
        return wxRequest.getParameter(name);
    }

    protected Object getUser(MethodParameter parameter, WxRequest wxRequest) {
        // 类型不匹配直接返回
        if (!wxUserProvider.isMatch(parameter.getParameterType())) {
            return null;
        } else if (WX_USER.equals(parameter.getParameterName()) || !BeanUtils.isSimpleProperty(parameter.getParameterType())) {
            // 两个都转换失败时，判断是否是简单属性，如果不是，则尝试转换为用户
            // 因为此时无法得知是要获取to还是from，所以取对于用户来说更需要的from
            return wxUserProvider.getUser(wxRequest.getBody().getFromUserName());
        }
        return null;
    }

    /*
        if (WX_TO_USER.equals(parameter.getParameterName())) {
            // 尝试转换toUser
            return wxUserProvider.getToUser(wxRequest.getToUserName());
        } else if (WX_FROM_USER.equals(parameter.getParameterName())) {
            // 尝试转换fromUser
            return wxUserProvider.getFromUser(wxRequest.getFromUserName());
        }
     */

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter, NativeWebRequest request)
            throws Exception {

        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
            if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
                throw new MultipartException("Current request is not a multipart request");
            } else {
                throw new MissingServletRequestPartException(name);
            }
        } else {
            throw new MissingServletRequestParameterException(name,
                    parameter.getNestedParameterType().getSimpleName());
        }
    }

    private static class RequestParamNamedValueInfo extends NamedValueInfo {

        public RequestParamNamedValueInfo() {
            super("", false, ValueConstants.DEFAULT_NONE);
        }

        public RequestParamNamedValueInfo(RequestParam annotation) {
            super(annotation.name(), annotation.required(), annotation.defaultValue());
        }
    }

}
