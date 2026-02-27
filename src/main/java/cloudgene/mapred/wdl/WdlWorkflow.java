package cloudgene.mapred.wdl;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.ArrayList;
import java.util.List;

@JsonClassDescription
public class WdlWorkflow {

	private List<WdlStep> steps = new ArrayList<>();

	private List<WdlParameterInput> inputs = new ArrayList<>();

	private List<WdlParameterOutput> outputs = new ArrayList<>();

	private String type = "sequence";

	private WdlStep setup = null;

	private List<WdlStep> setups = new ArrayList<>();

	private WdlStep onFailure = null;

	public List<WdlParameterInput> getInputs() {
		return inputs;
	}

	public void setInputs(List<WdlParameterInput> inputs) {
		this.inputs = inputs;
	}

	public List<WdlParameterOutput> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<WdlParameterOutput> outputs) {
		this.outputs = outputs;
	}

	public List<WdlStep> getSteps() {
		return steps;
	}

	public void setSteps(List<WdlStep> steps) {
		this.steps = steps;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public WdlStep getSetup() {
		return setup;
	}

	public void setSetup(WdlStep setup) {
		this.setup = setup;
	}

	public List<WdlStep> getSetups() {
		return setups;
	}

	public void setSetups(List<WdlStep> setups) {
		this.setups = setups;
	}

	public WdlStep getOnFailure() {
		return onFailure;
	}

	public void setOnFailure(WdlStep onFailure) {
		this.onFailure = onFailure;
	}
}
