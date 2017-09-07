package com.mxixm.fastboot.weixin.controller.invoker.annotation;

import com.mxixm.fastboot.weixin.controller.invoker.WxInvokerProxyFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 标记一个类为代理调用类
 * @see WxInvokerProxyFactory
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface WxApiRequest {

	/**
	 * 要调用的主机地址
	 * @return dummy
	 */
	String host() default ValueConstants.DEFAULT_NONE;

	/**
	 * 如果以方法名为属性名，通过SPEL表达式获得对应的地址，则prefix需要设置为参数的前缀
	 * @return dummy
	 */
	String prefix() default ValueConstants.DEFAULT_NONE;

	/**
	 * 方法上，如果有path，则优先取path，否则按上面的方式拼接
	 * @return dummy
	 */
	String path() default ValueConstants.DEFAULT_NONE;

	/**
	 * 调用方法，判断是什么类型的方法
	 * @return dummy
	 */
	Method method() default Method.GET;

	enum Method {

		GET(HttpMethod.GET),
		JSON(HttpMethod.POST),
		XML(HttpMethod.POST),
		FORM(HttpMethod.POST),
		PUT(HttpMethod.POST),
		PATCH(HttpMethod.PATCH),
		DELETE(HttpMethod.DELETE),
		OPTIONS(HttpMethod.OPTIONS),
		TRACE(HttpMethod.TRACE);

		private HttpMethod httpMethod;

		Method(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		public HttpMethod getHttpMethod() {
			return httpMethod;
		}
	}


}
