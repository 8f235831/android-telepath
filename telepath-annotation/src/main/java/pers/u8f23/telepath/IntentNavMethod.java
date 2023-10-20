package pers.u8f23.telepath;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于实现外部intent跳转的注解。请在对应静态方法上标记此注解，并在其参数上使用对应的注解标记。
 *
 * @author 8f23
 * @create 2022/12/17-12:22
 */
@Documented
@Retention (RetentionPolicy.SOURCE)
@Target (ElementType.METHOD)
public @interface IntentNavMethod{
	/** 导航路径或其前缀，不含<code>/path</code>部分。是否以<code>/</code>作为开头均可。 */
	String value();

	/** 是否使用前缀模糊匹配模式，默认不生效。 */
	boolean isPrefix() default false;

	/** 描述暴露接口的说明，用于生成文档。必填。使用中文即可。 */
	String description();
}
