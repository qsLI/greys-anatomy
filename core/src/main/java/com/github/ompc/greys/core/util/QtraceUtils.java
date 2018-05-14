/*
 *  * Copyright (c) 2018 Qunar.com. All Rights Reserved.
 */

package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.GlobalOptions;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

/**
 * Qunar Trace Support
 * @author qisheng.li
 * @email qisheng.li@qunar.com
 * @date 18-5-14 下午4:19
 */
public class QtraceUtils {

    private static final String ILLEGAL_QTRACE_ID = "-1";
    private static final String QTRACE_GETTER_CLASS_NAME = "qunar.tc.qtracer.impl.QTraceClientGetter";
    private static final String QTRACE_CLIENT_CLASS_NAME = "qunar.tc.qtracer.QTraceClient";
    private static final String GET_CLIENT_NAME = "getClient";
    private static final String GET_TRACE_ID_NAME = "getCurrentTraceId";

    /**
     * 获取qtraceId
     *
     * @param loader 目标ClassLoader
     * @return qtraceId
     */
    public static String getTraceId(final ClassLoader loader) {
        if (!GlobalOptions.isEnableTraceId) {
            return ILLEGAL_QTRACE_ID;
        }
        final Thread currentThread = Thread.currentThread();
        final ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(loader);
        try {
            final Class<?> classOfQtraceClientGetter = loader.loadClass(QTRACE_GETTER_CLASS_NAME);
            final Class<?> classOfQtraceClient = loader.loadClass(QTRACE_CLIENT_CLASS_NAME);
            final Method methodOfGetClient = classOfQtraceClientGetter.getMethod(GET_CLIENT_NAME);
            final Method methodOfGetTraceId = classOfQtraceClient.getMethod(GET_TRACE_ID_NAME);
            final Object qtraceClient = methodOfGetClient.invoke(null);
            if (null != qtraceClient && classOfQtraceClient.isAssignableFrom(qtraceClient.getClass())) {
                final Object qtraceId = methodOfGetTraceId.invoke(qtraceClient);
                if (qtraceId instanceof String
                        && StringUtils.isNoneBlank((String) qtraceId)) {
                    return (String) qtraceId;
                }
                return ILLEGAL_QTRACE_ID;
            } else {
                return ILLEGAL_QTRACE_ID;
            }
        } catch (Throwable t) {
            return ILLEGAL_QTRACE_ID;
        } finally {
            currentThread.setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * 判断是否支持EagleEye
     *
     * @param traceId
     * @return true:支持traceId;false:不支持;
     */
    public static boolean isQtraceEnabled(final String traceId) {
        return !StringUtils.equals(ILLEGAL_QTRACE_ID, traceId);
    }

}

