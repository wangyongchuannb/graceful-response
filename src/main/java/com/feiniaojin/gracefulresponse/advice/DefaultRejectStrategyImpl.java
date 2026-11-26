package com.feiniaojin.gracefulresponse.advice;

import com.feiniaojin.gracefulresponse.GracefulResponseRejectException;
import com.feiniaojin.gracefulresponse.advice.lifecycle.exception.RejectStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

    @Override
    public Object call(HttpServletRequest request, HttpServletResponse response, @Nullable Object handler,
            Exception exception) {
        if (adviceSupport.isMatchExcludeException(exception)) {
            Optional<HandlerMethod> handlerMethod = findHandlerMethod(exception);
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

    private Optional<HandlerMethod> findHandlerMethod(Exception exception) {
        for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : handlers.entrySet()) {
            Method method = entry.getValue().resolveMethod(exception);
            if (method != null) {
                return Optional.of(new HandlerMethod(entry.getKey().resolveBean(), method));
            }
        }
        return Optional.empty();
    }
}
