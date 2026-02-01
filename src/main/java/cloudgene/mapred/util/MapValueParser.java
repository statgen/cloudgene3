package cloudgene.mapred.util;

import java.util.*;

public final class MapValueParser {

	private MapValueParser() {}

	/**
	 * Recursively parses a nested map with {@code String} keys and {@code String}
	 * leaves into a typed map. Parses compatible leaves into {@code int},
	 * {@code float}, and {@code bool}.
	 */
	public static Map<String, Object> parseMap(Map<String, Object> map) {
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

	public static Object guessType(String value) {
		if (value == null) {
			return null;
		}
		if (isInteger(value)) {
			return Integer.parseInt(value);
		} else if (isDouble(value)) {
			return Double.parseDouble(value);
		} else if (isBoolean(value)) {
			return Boolean.parseBoolean(value);
		} else {
			return value;
		}
	}

	private static boolean isInteger(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isDouble(String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isBoolean(String value) {
		return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
	}
}
