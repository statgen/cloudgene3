package cloudgene.mapred.util.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.exceptions.BeanInstantiationException;
import jakarta.inject.Singleton;

import java.io.IOException;

@Factory
public class SettingsFactory {
	@Singleton
	public Settings settings() {
		try {
			return Settings.load();
		} catch (IOException e) {
			throw new BeanInstantiationException("Failed to load settings", e);
		}
	}
}
