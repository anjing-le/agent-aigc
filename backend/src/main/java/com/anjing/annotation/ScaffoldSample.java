package com.anjing.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记脚手架中的示例或自检代码。
 *
 * <p>复制为业务项目时，带有此标记的类型或包需要明确决定保留、删除或替换。</p>
 */
@Documented
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.SOURCE)
public @interface ScaffoldSample {

    /**
     * 示例存在的原因。
     */
    String value();
}
