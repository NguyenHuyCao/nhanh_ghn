package com.app84soft.check_in.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class TrackLogRequestInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = TrackLogRequestInterceptor.class.getName() + ".startTime";
    private static final String HANDLING_ATTR = TrackLogRequestInterceptor.class.getName() + ".handlingTime";
    private final long warnHandlingNanos = TimeUnit.SECONDS.toNanos(3L);

    public TrackLogRequestInterceptor() {
    }

    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            Method m = handlerMethod.getMethod();
            log.trace("[START CONTROLLER] {}.{}({})", new Object[]{m.getDeclaringClass().getSimpleName(), m.getName(), buildMethodParams(handlerMethod)});
//            log.info("IP = {} begin for request = {} url = {}", IPHelper.getIp(request), request.getSession().getId(), request.getRequestURL());
            request.setAttribute(START_ATTR, System.nanoTime());
        }
        return true;
    }

    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) {
        if (handler instanceof HandlerMethod) {
            long startTime = 0L;
            if (request.getAttribute(START_ATTR) != null) {
                startTime = (Long) request.getAttribute(START_ATTR);
            }
            long handlingTime = System.nanoTime() - startTime;
            request.removeAttribute(START_ATTR);
            request.setAttribute(HANDLING_ATTR, handlingTime);
            String formattedHandlingTime = String.format("%1$,3d", handlingTime);
            boolean isWarnHandling = handlingTime > this.warnHandlingNanos;
            if (this.isEnabledLogLevel(isWarnHandling)) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                Method m = handlerMethod.getMethod();
                Object view = null;
                Map<String, Object> model = null;
                if (modelAndView != null) {
                    view = modelAndView.getView();
                    model = modelAndView.getModel();
                    if (view == null) {
                        view = modelAndView.getViewName();
                    }
                }
                log.trace("[END CONTROLLER  ] {}.{}({})-> view={}, model={}", new Object[]{m.getDeclaringClass().getSimpleName(), m.getName(), buildMethodParams(handlerMethod), view, model});
                String handlingTimeMessage = "[HANDLING TIME   ] {}.{}({})-> {} ns";
                if (isWarnHandling) {
                    log.warn(handlingTimeMessage + " > {}", new Object[]{m.getDeclaringClass().getSimpleName(), m.getName(), buildMethodParams(handlerMethod), formattedHandlingTime, this.warnHandlingNanos});
                } else {
                    log.trace(handlingTimeMessage, new Object[]{m.getDeclaringClass().getSimpleName(), m.getName(), buildMethodParams(handlerMethod), formattedHandlingTime});
                }
            }
//            log.info("end for request = {} url = {}", request.getSession().getId(), request.getRequestURL());
        }
    }

    private boolean isEnabledLogLevel(boolean isWarnHandling) {
        if (isWarnHandling) {
            return true;
        } else return log.isTraceEnabled();
    }

    protected static String buildMethodParams(HandlerMethod handlerMethod) {
        MethodParameter[] params = handlerMethod.getMethodParameters();
        List<String> lst = new ArrayList<>(params.length);
        for (MethodParameter p : params) {
            lst.add(p.getParameterType().getSimpleName());
        }
        return StringUtils.collectionToCommaDelimitedString(lst);
    }

}
