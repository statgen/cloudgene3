package cloudgene.mapred.wdl;

public enum WdlParameterInputType {
	LOCAL_FOLDER("local_folder"),

	LOCAL_FILE("local_file"),

	/**
	 * Simple text input (maps to HTML {@code <input type="text">}).
	 */
	TEXT("text"),

	/**
	 * Same as {@link #TEXT}?
	 */
	STRING("string"),

	/**
	 * Optional checkbox. Defaults to {@code value}; use {@code values} to map
	 * {@code true} and {@code false} to strings.
	 */
	CHECKBOX("checkbox"),

	/**
	 * Base behavior: displays a drop-down with one entry for each listed value.
	 * <p>
	 * Bound behavior: if {@code values} has keys {@code bind}, {@code property},
	 * and {@code category}, the input is interpreted as a "binded" (bound) list. In
	 * that case, its type is changed to {@code "binded_list"} in API responses, and
	 * its values are taken from all installed Cloudgene apps that have the same
	 * {@code category} (in particular, we get the values they list under the given
	 * {@code property}). It's expected that another drop-down in this form has ID
	 * {@code bind}, and its currently selected value is used to determine which app
	 * inputs are listed.
	 */
	LIST("list"),

	/**
	 * Bound list containing its own option data. {@code value} indicates which
	 * other dropdown it is bound to. {@code groups} maps each option from the
	 * master dropdown to the options that should be displayed in the child
	 * dropdown.
	 */
	BINDED_LIST("binded_list"),

	/**
	 * Radio button group.
	 */
	RADIO("radio"),

	/**
	 * Same as {@link #TEXT}?
	 */
	NUMBER("number"),

	/**
	 * Non-interactive text displayed on the form's right side (where the inputs
	 * are).
	 */
	LABEL("label"),

	/**
	 * Raw HTML display that takes the full form width.
	 */
	INFO("info"),

	/**
	 * Same as {@link #TERMS_CHECKBOX}?
	 */
	AGBCHECKBOX("agbcheckbox"),

	/**
	 * A checkbox that must be accepted in order to be able to submit.
	 */
	TERMS_CHECKBOX("terms_checkbox"),

	/**
	 * Unused?
	 */
	GROUP("group"),

	/**
	 * Displays a drop-down with one entry for each installed Cloudgene app that has
	 * the requested {@code category}
	 */
	APP_LIST("app_list"),

	/**
	 * Render a horizontal rule.
	 */
	SEPARATOR("separator"),

	/**
	 * Expanded text input (maps to HTML {@code <textarea>}).
	 */
	TEXTAREA("textarea");

	private final String value;

	WdlParameterInputType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static WdlParameterInputType getEnum(String value) {
		String cleanValue = value.replace("-", "_");
		for (WdlParameterInputType parameterInputType : values())
			if (parameterInputType.getValue().equalsIgnoreCase(cleanValue))
				return parameterInputType;

		if (cleanValue.equalsIgnoreCase("file")) {
			return LOCAL_FILE;
		}

		if (cleanValue.equalsIgnoreCase("folder")) {
			return LOCAL_FOLDER;
		}

		if (cleanValue.equalsIgnoreCase("dataset")) {
			return APP_LIST;
		}

		throw new IllegalArgumentException("Value '" + value + "' is not a valid type.");
	}
}
