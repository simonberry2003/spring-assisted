package org.springframework.assisted;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;

import com.google.common.base.Preconditions;

/**
 * Spring implementation of assisted injection similar to that provided by Guice.
 * @param <FactoryClass> the class type of the factory interface
 */
public class AssistedFactoryBuilder<FactoryClass, T> {

	private final ApplicationContext context;
	private Constructor<T> constructor;
	private Class<T> clazz;

	/**
	 * @param context the application context
	 */
	public AssistedFactoryBuilder(ApplicationContext context) {
		this.context = Preconditions.checkNotNull(context);
	}

	/**
	 * Specifies the concrete type that the factory will create. The factory interface can return a subtype of the concrete type.
	 * @param clazz the concrete type that the factory returns
	 * @return this
	 * @throws IllegalStateException if there is not exactly one constructor on clazz type that is marked with {@link @Inject}
	 */
	@SuppressWarnings("unchecked")
	public AssistedFactoryBuilder<FactoryClass, T> creates(Class<T> clazz) {
		this.clazz = Preconditions.checkNotNull(clazz);

		// Find the one and only constructor marked with @Inject
		for (Constructor<?> constructor : clazz.getConstructors()) {
			if (constructor.getAnnotationsByType(Inject.class).length > 0) {
				Preconditions.checkState(this.constructor == null, "Found more than one public constructor with @Inject on class: " + clazz.getName());
				this.constructor = (Constructor<T>) constructor;
			}
		}
		Preconditions.checkState(this.constructor != null, "Could not find public constructor with @Inject on class: " + clazz.getName());
		return this;
	}

	/**
	 * Builds the {@link FactoryClass} instance that will create instances of the class type specified in {@link #creates(Class)}
	 * @param factoryInterface the factory interface class
	 * @return an instance of {@link FactoryClass}
	 * @throws IllegalStateException if creates has not been called
	 * @see AssistedHandler#AssistedHandler
	 */
	@SuppressWarnings("unchecked")
	public FactoryClass build(Class<FactoryClass> factoryInterface) {
		Preconditions.checkState(clazz != null, "You must call creates(...) method before build");
		return (FactoryClass) Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class[] {factoryInterface},
			new AssistedHandler(context, constructor, clazz, factoryInterface));
	}
}
