package org.springframework.assisted;

import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Service class to create assisted factory instances.
 */
@Service
public class AssistedFactoryProvider {

	private @Inject ApplicationContext context;

	/**
	 * Gets an instance of the specified factory interface that creates instanceClass instances.
	 * Parameters marked with {@link Assisted} are provided at runtime.
	 * @param factoryInterface
	 * @param instanceClass
	 * @return instance of factoryInterface
	 */
	public <F, T> F provide(Class<F> factoryInterface, Class<T> instanceClass) {
		return new AssistedFactoryBuilder<F, T>(context)
			.creates(instanceClass)
			.build(factoryInterface);
	}
}
