package cloudgene.mapred.server.responses;

import java.io.File;
import java.util.List;

import cloudgene.mapred.jobs.state.JobState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import genepi.io.FileUtil;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonClassDescription
public class JobAdminResponse {

	private List<JobResponse> data;
	private int count;
	private int success;
	private int failed;
	private int pending;
	private int waiting;
	private int running;
	private int canceled;

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}

	public int getFailed() {
		return failed;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}

	public int getPending() {
		return pending;
	}

	public void setPending(int pending) {
		this.pending = pending;
	}

	public int getWaiting() {
		return waiting;
	}

	public void setWaiting(int waiting) {
		this.waiting = waiting;
	}

	public int getRunning() {
		return running;
	}

	public void setRunning(int running) {
		this.running = running;
	}

	public int getCanceled() {
		return canceled;
	}

	public void setCanceled(int canceled) {
		this.canceled = canceled;
	}

	public static JobAdminResponse build(List<JobResponse> responses, String workspace) {
		int success = 0;
		int failed = 0;
		int pending = 0;
		int waiting = 0;
		int canceled = 0;
		int running = 0;

		JobAdminResponse response = new JobAdminResponse();
		response.setData(responses);
		response.setCount(responses.size());

		for (JobResponse job : responses) {
			String folder = FileUtil.path(workspace, job.getId());
			File file = new File(folder);

			if (file.exists()) {
				long size = FileUtils.sizeOfDirectory(file);
				job.setWorkspaceSize(FileUtils.byteCountToDisplaySize(size));
			}

			if (job.getState() == JobState.STATE_EXPORTING.getValue()
					|| job.getState() == JobState.STATE_RUNNING.getValue()) {
				running++;
			}

			if (job.getState() == JobState.STATE_SUCCESS.getValue()
					|| job.getState() == JobState.STATE_SUCCESS_AND_NOTIFICATION_SEND.getValue()) {
				success++;
			}

			if (job.getState() == JobState.STATE_FAILED.getValue()
					|| job.getState() == JobState.STATE_FAILED_AND_NOTIFICATION_SEND.getValue()) {
				failed++;
			}

			if (job.getState() == JobState.STATE_DEAD.getValue()) {
				pending++;
			}

			if (job.getState() == JobState.STATE_WAITING.getValue()) {
				waiting++;
			}

			if (job.getState() == JobState.STATE_CANCELED.getValue()) {
				canceled++;
			}
		}

		response.setSuccess(success);
		response.setFailed(failed);
		response.setPending(pending);
		response.setWaiting(waiting);
		response.setCanceled(canceled);
		response.setRunning(running);

		return response;
	}

	public List<JobResponse> getData() {
		return data;
	}

	public void setData(List<JobResponse> data) {
		this.data = data;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
