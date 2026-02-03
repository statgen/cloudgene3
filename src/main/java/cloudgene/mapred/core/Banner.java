package cloudgene.mapred.core;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonClassDescription
public class Banner {

	@JsonClassDescription
	public enum Type {
		WARNING("warning"),
		DANGER("danger");

		private final String value;

		Type(String value) {
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString() {
			return value;
		}

		public static Type of(String value) {
			if (value == null || value.isBlank()) {
				throw new IllegalArgumentException("value must be a non-blank string.");
			}

			value = value.strip().toLowerCase();

			for (Type type : values()) {
				if (value.equals(type.toString())) {
					return type;
				}
			}

			throw new IllegalArgumentException("Unrecognized type: " + value);
		}
	}

	private Type type;
	private String message;

	@JsonIgnore
	private int position;

	private int id;

	public Banner(Type type, String message, int position, int id) {
		this.type = type;
		this.message = message;
		this.position = position;
		this.id = id;
	}

	public Banner(Type type, String message) {
		this.type = type;
		this.message = message;
		this.position = -1; // Unassigned.
		this.id = -1; // Unassigned.
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
