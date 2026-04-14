package cloudgene.mapred.util;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

import java.util.*;

public final class MapValueParser {

	private MapValueParser() {}

	/**
	 * Recursively parses a nested map with {@code String} keys and {@code String}
	 * leaves into a typed map. Parses compatible leaves into {@code int},
	 * {@code float}, and {@code bool}.
	 */
	public static @NonNull Map<String, Object> parseMap(@NonNull Map<String, Object> map) {
		Map<String, Object> parsedMap = new HashMap<>();

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof String) {
				parsedMap.put(key, guessType((String) value));
			} else if (value instanceof Map) {
				parsedMap.put(key, parseMap((Map<String, Object>) value));
			} else {
				parsedMap.put(key, value);
			}
		}

		return parsedMap;
	}

	public static @Nullable Object guessType(@Nullable String value) {
		if (value == null) {
			return null;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// pass
		}

		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			// pass
		}

		if (isBoolean(value)) {
			return Boolean.parseBoolean(value);
		}

		return value;
	}

	private static boolean isBoolean(String value) {
		return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
	}
}
