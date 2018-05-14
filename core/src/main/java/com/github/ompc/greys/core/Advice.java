package com.github.ompc.greys.core;

import com.github.ompc.greys.core.util.AliEagleEyeUtils;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.LazyGet;
import com.github.ompc.greys.core.util.QtraceUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 通知点
 */
public final class Advice {

    public final ClassLoader loader;
    private final LazyGet<Class<?>> clazzRef;
    private final LazyGet<GaMethod> methodRef;
    private final LazyGet<String> traceIdRef;
    public final Object target;
    public final Object[] params;
    public final Object returnObj;
    public final Throwable throwExp;

    private final static int ACCESS_BEFORE = 1;
    private final static int ACCESS_AFTER_RETUNING = 1 << 1;
    private final static int ACCESS_AFTER_THROWING = 1 << 2;

    private static final String ILLEGAL_TRACE_ID = "-1";

    public final boolean isBefore;
    public final boolean isThrow;
    public final boolean isReturn;
    public final boolean isThrowing;
    public final boolean isReturning;

    // 回放过程processId
    // use for TimeTunnelCommand.doPlay()
    // public final Integer playIndex;

    /**
     * for finish
     *
     * @param loader    类加载器
     * @param clazzRef  类
     * @param methodRef 方法
     * @param target    目标类
     * @param params    调用参数
     * @param returnObj 返回值
     * @param throwExp  抛出异常
     * @param access    进入场景
     */
    private Advice(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params,
            Object returnObj,
            Throwable throwExp,
            int access) {
        this.loader = loader;
        this.clazzRef = clazzRef;
        this.methodRef = methodRef;
        this.traceIdRef = lazyGetTraceId(loader);
        this.target = target;
        this.params = params;
        this.returnObj = returnObj;
        this.throwExp = throwExp;
        isBefore = (access & ACCESS_BEFORE) == ACCESS_BEFORE;
        isThrow = (access & ACCESS_AFTER_THROWING) == ACCESS_AFTER_THROWING;
        isReturn = (access & ACCESS_AFTER_RETUNING) == ACCESS_AFTER_RETUNING;

        this.isReturning = isReturn;
        this.isThrowing = isThrow;

        // playIndex = PlayIndexHolder.getInstance().get();
    }

    // 获取阿里巴巴中间件鹰眼ID
    private LazyGet<String> lazyGetTraceId(final ClassLoader loader) {
        return new LazyGet<String>() {
            @Override
            protected String initialValue() throws Throwable {
                final String eagleEyeTraceId = AliEagleEyeUtils.getTraceId(loader);
                if (!ILLEGAL_TRACE_ID.equals(eagleEyeTraceId)) {
                    return eagleEyeTraceId;
                }
                final String qtraceId = QtraceUtils.getTraceId(loader);
                if (!ILLEGAL_TRACE_ID.equals(qtraceId)) {
                    return qtraceId;
                }
                return ILLEGAL_TRACE_ID;
            }
        };
    }

    /**
     * 构建Before通知点
     */
    public static Advice newForBefore(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params) {
        return new Advice(
                loader,
                clazzRef,
                methodRef,
                target,
                params,
                null, //returnObj
                null, //throwExp
                ACCESS_BEFORE
        );
    }

    /**
     * 构建正常返回通知点
     */
    public static Advice newForAfterRetuning(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params,
            Object returnObj) {
        return new Advice(
                loader,
                clazzRef,
                methodRef,
                target,
                params,
                returnObj,
                null, //throwExp
                ACCESS_AFTER_RETUNING
        );
    }

    /**
     * 构建抛异常返回通知点
     */
    public static Advice newForAfterThrowing(
            ClassLoader loader,
            LazyGet<Class<?>> clazzRef,
            LazyGet<GaMethod> methodRef,
            Object target,
            Object[] params,
            Throwable throwExp) {
        return new Advice(
                loader,
                clazzRef,
                methodRef,
                target,
                params,
                null, //returnObj
                throwExp,
                ACCESS_AFTER_THROWING
        );
    }

    /**
     * 获取Java类
     *
     * @return Java Class
     */
    public Class<?> getClazz() {
        return clazzRef.get();
    }

    /**
     * 获取Java方法
     *
     * @return Java Method
     */
    public GaMethod getMethod() {
        return methodRef.get();
    }

    /**
     * 本次调用是否支持中间件跟踪<br/>
     * 在很多大公司中,会有比较多的中间件调用链路渲染技术用来记录和支撑分布式调用场景下的系统串联<br/>
     * 用于串联各个系统调用的一般是一个全局唯一的跟踪号,如果当前调用支持被跟踪,则返回true;<br/>
     * <p>
     * 在阿里中,进行跟踪的调用号被称为EagleEye
     *
     * @return true:支持被跟踪;false:不支持
     */
    public boolean isTraceSupport() {
        return GlobalOptions.isEnableTraceId
                && (AliEagleEyeUtils.isEagleEyeSupport(traceIdRef.get())
                || QtraceUtils.isQtraceEnabled(traceIdRef.get()));
    }

    /**
     *
     * @return 本次调用的跟踪号
     */
    public String getTraceId() {
        return traceIdRef.get();
    }

}
