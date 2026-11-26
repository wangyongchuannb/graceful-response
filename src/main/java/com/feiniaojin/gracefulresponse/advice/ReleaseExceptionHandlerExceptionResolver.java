package com.feiniaojin.gracefulresponse.advice;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * Invokes an exception handler selected by the release strategy with the application's MVC converters.
 *
 * @author qinyujie
 */
public class ReleaseExceptionHandlerExceptionResolver extends ExceptionHandlerExceptionResolver
        implements SmartInitializingSingleton {

    public static final String RELEASE_EXCEPTION_KEY = "RELEASE_EXCEPTION_KEY";

    private boolean initialized;

    @Resource
    @Lazy
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Resource(name = "mvcContentNegotiationManager")
    @Lazy
    private ContentNegotiationManager mvcContentNegotiationManager;

    @Override
    public void afterPropertiesSet() {
        // MVC initialization also discovers our advice beans. Wait until all singletons exist
        // before obtaining its converters, otherwise the advice dependencies form a cycle.
    }

    @Override
    public void afterSingletonsInstantiated() {
        initializeIfNecessary();
    }

    private synchronized void initializeIfNecessary() {
        if (!initialized) {
            setMessageConverters(requestMappingHandlerAdapter.getMessageConverters());
            setContentNegotiationManager(mvcContentNegotiationManager);
            super.afterPropertiesSet();
            initialized = true;
        }
    }

    @Override
    protected List<HandlerMethodReturnValueHandler> getDefaultReturnValueHandlers() {
        List<HandlerMethodReturnValueHandler> handlers = super.getDefaultReturnValueHandlers();
        // Keep the original release contract: plain objects from @ControllerAdvice are JSON
        // responses too. Standard MVC handlers retain priority for ResponseEntity, views, etc.
        List<Object> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext()).stream()
                .filter(advice -> advice.getBeanType() != null
                        && ResponseBodyAdvice.class.isAssignableFrom(advice.getBeanType()))
                .map(advice -> (Object) advice)
                .toList();
        handlers.add(handlers.size() - 1, new RequestResponseBodyMethodProcessor(
                getMessageConverters(), getContentNegotiationManager(), adviceBeans) {
            @Override
            public boolean supportsReturnType(MethodParameter returnType) {
                return true;
            }
        });
        return handlers;
    }

    @Override
    public ModelAndView doResolveHandlerMethodException(HttpServletRequest request,
            HttpServletResponse response, HandlerMethod handlerMethod, Exception exception) {
        initializeIfNecessary();
        return super.doResolveHandlerMethodException(request, response, handlerMethod, exception);
    }

    @Override
    protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod,
            Exception exception, ServletWebRequest webRequest) {
        return getExceptionHandlerMethod(handlerMethod, exception);
    }

    protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod,
            Exception exception) {
        if (handlerMethod == null) {
            return null;
        }
        return new ServletInvocableHandlerMethod(handlerMethod.getBean(), handlerMethod.getMethod(),
                getApplicationContext());
    }
}
