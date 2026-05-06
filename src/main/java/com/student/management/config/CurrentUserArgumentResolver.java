package com.student.management.config;

import com.student.management.common.ApiException;
import com.student.management.security.AuthInterceptor;
import com.student.management.security.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(SessionUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ApiException(401, "未登录");
        }
        Object user = request.getAttribute(AuthInterceptor.CURRENT_USER);
        if (!(user instanceof SessionUser sessionUser)) {
            throw new ApiException(401, "未登录");
        }
        return sessionUser;
    }
}
