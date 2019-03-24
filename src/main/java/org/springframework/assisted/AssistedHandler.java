package org.springframework.assisted;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * {@link InvocationHandler} to handle calls to an assisted factory instance. Parameters marked with
 * {@link Assisted} are provided at runtime and any other parameters required for construction are
 * obtained from the {@link ApplicationContext}
 */
public class AssistedHandler implements InvocationHandler {

	private final ApplicationContext context;
	private final Constructor<?> constructor;
	private final Class<?> clazz;
	private final Class<?> factoryInterface;

	/**
	 * @param context the application context
	 * @param constructor the constructor class for creating instances
	 * @param clazz the type of returned instances
	 * @param factoryInterface the factory interface type used for error reporting
	 * @throws IllegalStateException if the factory interface does not have a single method
	 * @throws IllegalArgumentException if the return type on the factory method is not assignable from the clazz type
	 */
	public AssistedHandler(ApplicationContext context, Constructor<?> constructor, Class<?> clazz, Class<?> factoryInterface) {
		this.context = Preconditions.checkNotNull(context);
		this.constructor = Preconditions.checkNotNull(constructor);
		this.clazz = Preconditions.checkNotNull(clazz);
		this.factoryInterface = Preconditions.checkNotNull(factoryInterface);
		Method[] methods = factoryInterface.getMethods();
		Preconditions.checkState(methods.length == 1, "Only one method allowed on factory: " + factoryInterface.getName());
		Method method = methods[0];
		Preconditions.checkArgument(method.getReturnType().isAssignableFrom(clazz),
			"Return type on method " + method.getName() + "(...) is invalid. Clazz type " + clazz.getName() + " must be assignable from " + method.getReturnType());
	}

	/**
	 * Creates an instance of clazz type using the specified arguments. The arguments specified must
	 * match with {@link Assisted} params on the constructor of the type being creating. Any other arguments
	 * on the constructor are obtains from the application context.
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		// Store method args by type in the order they are provided
		Multimap<Class<?>, Object> argsByType = LinkedHashMultimap.create();
		int parameterNumber = 0;
		for (Parameter methodParameter : method.getParameters()) {
			argsByType.put(methodParameter.getType(), args[parameterNumber++]);
		}

		// Process each constructor argument. If it is not @Assisted, use the application context to get an instance
		// of the argument. Otherwise, take the next argument in the method call that matches the type.
		Parameter[] constructorParameters = constructor.getParameters();
		Object[] constructorArguments = new Object[constructorParameters.length];
		int argumentNumber = 0;
		for (Parameter parameter : constructorParameters) {
			Class<?> type = parameter.getType();
			if (parameter.getAnnotation(Assisted.class) == null) {
				try {
					// Unassisted argument so get instance from application context
					constructorArguments[argumentNumber] = context.getBean(type);
				} catch (NoSuchBeanDefinitionException e) {
					throw new IllegalStateException("Could not resolve a bean for type " + type.getName() +
						" required for construction of class " + clazz.getName() + ". Did you mean this to be marked @" + Assisted.class.getSimpleName() + "?");
				}
			} else {
				Collection<Object> argumentsForType = argsByType.get(parameter.getType());
				if (argumentsForType.isEmpty()) {
					throw new IllegalStateException("Not enough arguments on " + method.getName() + "(...) method on factory class " + factoryInterface.getName() + "." +
						" There must be an argument for each @" + Assisted.class.getSimpleName() + " constructor parameter on class " + clazz.getName());
				}
				// Take the next argument for the type and remove it so we don't use it again
				constructorArguments[argumentNumber] = argumentsForType.iterator().next();
				argsByType.remove(parameter.getType(), constructorArguments[argumentNumber]);
			}
			argumentNumber++;
		}
		if (!argsByType.isEmpty()) {
			throw new IllegalStateException("Not all arguments were matched with the constructor of class " + clazz.getName() +
				". There must be an argument for each constructor parameter marked with @" + Assisted.class.getSimpleName() + ".");
		}
		return constructor.newInstance(constructorArguments);
	}
}
