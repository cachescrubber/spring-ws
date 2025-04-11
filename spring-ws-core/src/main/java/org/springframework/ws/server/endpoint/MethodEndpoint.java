/*
 * Copyright 2005-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.server.endpoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Represents a bean method that will be invoked as part of an incoming Web service
 * message.
 * <p>
 * Consists of a {@link Method}, and a bean {@link Object}.
 *
 * @author Arjen Poutsma
 * @since 1.0.0
 */
public final class MethodEndpoint {

	private final Object bean;

	private final Method method;

	private final BeanFactory beanFactory;

	private final MethodParameter[] parameters;

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	@Nullable
	private volatile List<Annotation[][]> interfaceParameterAnnotations;

	/**
	 * Constructs a new method endpoint with the given bean and method.
	 * @param bean the object bean
	 * @param method the method
	 */
	public MethodEndpoint(Object bean, Method method) {
		Assert.notNull(bean, "bean must not be null");
		Assert.notNull(method, "method must not be null");
		this.bean = bean;
		this.method = method;
		this.beanFactory = null;
		this.parameters = initMethodParameters();
	}

	/**
	 * Constructs a new method endpoint with the given bean, method name and parameters.
	 * @param bean the object bean
	 * @param methodName the method name
	 * @param parameterTypes the method parameter types
	 * @throws NoSuchMethodException when the method cannot be found
	 */
	public MethodEndpoint(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
		Assert.notNull(bean, "bean must not be null");
		Assert.notNull(methodName, "method must not be null");
		this.bean = bean;
		this.method = bean.getClass().getMethod(methodName, parameterTypes);
		this.beanFactory = null;
		this.parameters = initMethodParameters();
	}

	/**
	 * Constructs a new method endpoint with the given bean name and method. The bean name
	 * will be lazily initialized when {@link #invoke(Object...)} is called.
	 * @param beanName the bean name
	 * @param beanFactory the bean factory to use for bean initialization
	 * @param method the method
	 */
	public MethodEndpoint(String beanName, BeanFactory beanFactory, Method method) {
		Assert.hasText(beanName, "'beanName' must not be null");
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(method, "'method' must not be null");
		Assert.isTrue(beanFactory.containsBean(beanName),
				"Bean factory [" + beanFactory + "] does not contain bean " + "with name [" + beanName + "]");
		this.bean = beanName;
		this.beanFactory = beanFactory;
		this.method = method;
		this.parameters = initMethodParameters();
	}

	/** Returns the object bean for this method endpoint. */
	public Object getBean() {
		if (this.beanFactory != null && this.bean instanceof String beanName) {
			return this.beanFactory.getBean(beanName);
		}
		else {
			return this.bean;
		}
	}

	/** Returns the method for this method endpoint. */
	public Method getMethod() {
		return this.method;
	}

	/** Returns the method parameters for this method endpoint. */
	public MethodParameter[] getMethodParameters() {
		return this.parameters;
	}

	/** Returns the method parameters for this method endpoint. */
	protected MethodParameter[] initMethodParameters() {
		int parameterCount = getMethod().getParameterTypes().length;
		MethodParameter[] parameters = new MethodParameter[parameterCount];
		for (int i = 0; i < parameterCount; i++) {
			parameters[i] = new EndpointMethodParameter(i);
		}
		return parameters;
	}

	/** Returns the method return type, as {@code MethodParameter}. */
	public MethodParameter getReturnType() {
		return new EndpointMethodParameter(-1);
	}

	private List<Annotation[][]> getInterfaceParameterAnnotations() {
		List<Annotation[][]> parameterAnnotations = this.interfaceParameterAnnotations;
		if (parameterAnnotations == null) {
			parameterAnnotations = new ArrayList<>();
			for (Class<?> ifc : this.method.getDeclaringClass().getInterfaces()) {
				for (Method candidate : ifc.getMethods()) {
					if (isOverrideFor(candidate)) {
						parameterAnnotations.add(candidate.getParameterAnnotations());
					}
				}
			}
			this.interfaceParameterAnnotations = parameterAnnotations;
		}
		return parameterAnnotations;
	}

	private boolean isOverrideFor(Method candidate) {
		if (!candidate.getName().equals(this.method.getName())
				|| candidate.getParameterCount() != this.method.getParameterCount()) {
			return false;
		}
		Class<?>[] paramTypes = this.method.getParameterTypes();
		if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
			return true;
		}
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i] != ResolvableType.forMethodParameter(candidate, i, this.method.getDeclaringClass())
				.resolve()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return a single annotation on the underlying method traversing its super methods if
	 * no annotation can be found on the given method itself.
	 * <p>
	 * Also supports <em>merged</em> composed annotations with attribute overrides as of
	 * Spring Framework 4.2.2.
	 * @param annotationType the type of annotation to introspect the method for
	 * @return the annotation, or {@code null} if none found
	 * @see AnnotatedElementUtils#findMergedAnnotation
	 */
	@Nullable
	public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
	}

	/**
	 * Return whether the parameter is declared with the given annotation type.
	 * @param annotationType the annotation type to look for
	 * @since 4.3
	 * @see AnnotatedElementUtils#hasAnnotation
	 */
	public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
		return AnnotatedElementUtils.hasAnnotation(this.method, annotationType);
	}

	/**
	 * Invokes this method endpoint with the given arguments.
	 * @param args the arguments
	 * @return the invocation result
	 * @throws Exception when the method invocation results in an exception
	 */
	public Object invoke(Object... args) throws Exception {
		Object endpoint = getBean();
		ReflectionUtils.makeAccessible(this.method);
		try {
			return this.method.invoke(endpoint, args);
		}
		catch (InvocationTargetException ex) {
			handleInvocationTargetException(ex);
			throw new IllegalStateException("Unexpected exception thrown by method - "
					+ ex.getTargetException().getClass().getName() + ": " + ex.getTargetException().getMessage());
		}
	}

	private void handleInvocationTargetException(InvocationTargetException ex) throws Exception {
		Throwable targetException = ex.getTargetException();
		if (targetException instanceof RuntimeException) {
			throw (RuntimeException) targetException;
		}
		if (targetException instanceof Error) {
			throw (Error) targetException;
		}
		if (targetException instanceof Exception) {
			throw (Exception) targetException;
		}

	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof MethodEndpoint other) {
			return this.bean.equals(other.bean) && this.method.equals(other.method);
		}
		return false;
	}

	public int hashCode() {
		return 31 * this.bean.hashCode() + this.method.hashCode();
	}

	public String toString() {
		return this.method.toGenericString();
	}

	/**
	 * A MethodParameter with EndpointMethod-specific behavior.
	 */
	protected class EndpointMethodParameter extends SynthesizingMethodParameter {

		@Nullable
		private volatile Annotation[] combinedAnnotations;

		public EndpointMethodParameter(int index) {
			super(MethodEndpoint.this.getMethod(), index);
			initParameterNameDiscovery(MethodEndpoint.this.parameterNameDiscoverer);
		}

		protected EndpointMethodParameter(MethodEndpoint.EndpointMethodParameter original) {
			super(original);
		}

		@Override
		public Class<?> getContainingClass() {
			return ClassUtils.getUserClass(MethodEndpoint.this.getBean());
		}

		@Override
		public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
			return MethodEndpoint.this.getMethodAnnotation(annotationType);
		}

		@Override
		public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
			return MethodEndpoint.this.hasMethodAnnotation(annotationType);
		}

		@Override
		public Annotation[] getParameterAnnotations() {
			Annotation[] anns = this.combinedAnnotations;
			if (anns == null) {
				anns = super.getParameterAnnotations();
				int index = getParameterIndex();
				if (index >= 0) {
					for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
						if (index < ifcAnns.length) {
							Annotation[] paramAnns = ifcAnns[index];
							if (paramAnns.length > 0) {
								List<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
								merged.addAll(Arrays.asList(anns));
								for (Annotation paramAnn : paramAnns) {
									boolean existingType = false;
									for (Annotation ann : anns) {
										if (ann.annotationType() == paramAnn.annotationType()) {
											existingType = true;
											break;
										}
									}
									if (!existingType) {
										merged.add(adaptAnnotation(paramAnn));
									}
								}
								anns = merged.toArray(new Annotation[0]);
							}
						}
					}
				}
				this.combinedAnnotations = anns;
			}
			return anns;
		}


		@Override
		public MethodEndpoint.EndpointMethodParameter clone() {
			return new MethodEndpoint.EndpointMethodParameter(this);
		}

	}

}
