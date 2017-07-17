package cn.hn.java.summer.springnc.annotation;

import java.lang.annotation.*;

/**
 * Created by xw2sy on 2017-07-17.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcludeMapping {
}
