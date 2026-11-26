package com.feiniaojin.gracefulresponse.advice;

import com.feiniaojin.gracefulresponse.GracefulResponseRejectException;
import com.feiniaojin.gracefulresponse.advice.lifecycle.exception.RejectStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.HandlerMapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Finds a handler outside Graceful Response for excluded exceptions.
 *
 * @author qinyujie
 */
public class DefaultRejectStrategyImpl implements RejectStrategy {

    private final Map<ControllerAdviceBean, ExceptionHandlerMethodResolver> handlers = new LinkedHashMap<>();

    @Resource
    private AdviceSupport adviceSupport;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private ReleaseExceptionHandlerExceptionResolver releaseExceptionHandlerExceptionResolver;

    @Resource(name = "mvcContentNegotiationManager")
    @Lazy
    private ContentNegotiationManager contentNegotiationManager;

    @Override
    public Object call(HttpServletRequest request, HttpServletResponse response, @Nullable Object handler,
            Exception exception) {
        if (adviceSupport.isMatchExcludeException(exception)) {
            Object originalHandler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
            Class<?> controllerType = originalHandler instanceof HandlerMethod method ? method.getBeanType() : null;
            List<MediaType> mediaTypes;
            try {
                mediaTypes = contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
            } catch (HttpMediaTypeNotAcceptableException negotiationException) {
                throw new GracefulResponseRejectException("无法协商异常响应类型", negotiationException);
            }
            Optional<HandlerMethod> handlerMethod = findHandlerMethod(exception, controllerType, mediaTypes, request);
            if (handlerMethod.isPresent()) {
                return releaseExceptionHandlerExceptionResolver.doResolveHandlerMethodException(
                        request, response, handlerMethod.get(), exception);
            }
        }
        throw new GracefulResponseRejectException("异常不匹配，由ErrorController进行错误处理", exception);
    }

    @PostConstruct
    public void collectAllExceptionHandlers() {
        handlers.clear();
        for (ControllerAdviceBean advice : ControllerAdviceBean.findAnnotatedBeans(applicationContext)) {
            Class<?> beanType = advice.getBeanType();
            if (beanType != null && !AbstractControllerAdvice.class.isAssignableFrom(beanType)) {
                handlers.put(advice, new ExceptionHandlerMethodResolver(beanType));
            }
        }
    }

    private Optional<HandlerMethod> findHandlerMethod(Exception exception, Class<?> controllerType,
            List<MediaType> mediaTypes, HttpServletRequest request) {
        for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : handlers.entrySet()) {
            if (!entry.getKey().isApplicableToBeanType(controllerType)) {
                continue;
            }
            for (MediaType mediaType : mediaTypes) {
                var mapping = entry.getValue().resolveExceptionMapping(exception, mediaType);
                if (mapping != null) {
                    if (!mapping.getProducibleTypes().isEmpty()) {
                        request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE,
                                mapping.getProducibleTypes());
                    }
                    return Optional.of(new HandlerMethod(entry.getKey().resolveBean(), mapping.getHandlerMethod()));
                }
            }
        }
        return Optional.empty();
    }
}
