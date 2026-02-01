package cloudgene.mapred.wdl;

public enum WdlParameterOutputType {
	LOCAL_FOLDER("local_folder"),
	LOCAL_FILE("local_file");

	private final String value;

	WdlParameterOutputType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.getValue();
	}

	public static WdlParameterOutputType getEnum(String value) {
		for (WdlParameterOutputType v : values())
			if (v.getValue().equalsIgnoreCase(value.replaceAll("-", "_")))
				return v;
		return LOCAL_FOLDER;
	}
}
