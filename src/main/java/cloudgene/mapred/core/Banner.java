package cloudgene.mapred.core;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

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
				throw new IllegalArgumentException("Value must be a non-blank string.");
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

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Banner))
			return false;

		Banner that = (Banner) other;

		return this.position == that.position
				&& this.id == that.id
				&& this.type == that.type
				&& Objects.equals(this.message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, message, position, id);
	}
}
