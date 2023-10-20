package pers.u8f23.telepath;

import java.lang.annotation.*;

/**
 * 用于实现外部intent跳转的注解。请在方法参数的Controller上标记此注解。
 *
 * @author 8f23
 * @create 2022/12/17-12:22
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target (ElementType.PARAMETER)
public @interface IntentNavController{
}
