package cloudgene.mapred.plugins.nextflow;

import cloudgene.mapred.jobs.Step;

public class NextflowProcessConfig {

	private static final String DEFAULT_VIEW = "list";

	private String view = DEFAULT_VIEW;
	private String label = null;
	private String group = null;
	private Step step = null;

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public Step getStep() {
		return step;
	}

	public void setStep(Step step) {
		this.step = step;
	}
}
