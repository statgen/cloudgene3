package cloudgene.mapred.jobs.workspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.Download;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.S3Util;
import genepi.io.FileUtil;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Workspace implements IWorkspace {

	private static final String OUTPUT_DIRECTORY = "outputs";

	private static final String INPUT_DIRECTORY = "input";

	private static final String LOGS_DIRECTORY = "logs";

	private static final String TEMP_DIRECTORY = "temp";

	public static long EXPIRATION_MS = 1_000L * 60L * 60L;

	private static final Logger log = LoggerFactory.getLogger(S3Workspace.class);

	private final String location;

	private String job;

	public S3Workspace(String location) {
		this.location = location;
	}

	@Override
	public String getName() {
		return "Amazon S3";
	}

	@Override
	public void setJob(String job) {
		this.job = job;
	}

	@Override
	public void setup() throws IOException {

		if (job == null) {
			throw new IOException("No job id provided.");
		}

		if (location == null) {
			throw new IOException("No S3 Output URI specified.");
		}

		if (!S3Util.isValidS3Uri(location)) {
			throw new IOException("Output URI '" + location + "' is not a valid S3 URI (s3://<bucket>/<key>).");
		}

		try {
			S3Util.copyToS3(job, location + "/" + job + "/version.txt");
		} catch (Exception e) {
			log.error("Copy file to '{}/{}/version.txt' failed.", location, job, e);
			throw new IOException("Output URI '" + location + "' is not writable.", e);
		}
	}

	@Override
	public String upload(String id, File file) throws IOException {
		String target = location + "/" + job + "/" + id + "/" + file.getName();
		log.info("Copy file " + file.getAbsolutePath() + " to " + target);
		S3Util.copyToS3(file, target);
		return target;
	}

	@Override
	public String uploadInput(String id, File file) throws IOException {
		return upload(FileUtil.path(INPUT_DIRECTORY, id), file);
	}

	@Override
	public String uploadLog(File file) throws IOException {
		return upload(LOGS_DIRECTORY, file);
	}

	/**
	 * Downloads the object at the given S3 URI location.
	 */
	@Override
	public InputStream download(String uri) throws IOException {
		S3Util.UriParts uriParts = S3Util.getParts(uri);
		return S3Util.getObject(uriParts);
	}

	@Override
	public String downloadLog(String name) throws IOException {
		String fullPath = FileUtil.path(location, job, LOGS_DIRECTORY, name);
		String log = FileUtil.readFileAsString(download(fullPath));
		return log;
	}

	/**
	 * Checks if {@code uri} is a valid S3 URI for an existing object.
	 */
	public boolean exists(String uri) throws IOException {
		S3Util.UriParts uriParts = S3Util.getParts(uri);
		return S3Util.doesObjectExist(uriParts);
	}

	@Override
	public void delete(String job) throws IOException {
		if (!S3Util.isValidS3Uri(location)) {
			throw new IOException("Output Url '" + location + "' is not a valid S3 bucket.");
		}

		String url = location + "/" + job;

		try {

			log.info("Deleting {} on S3 workspace: '{}'...", job, url);

			S3Util.deleteFolder(url);

			log.info("Deleted all files on S3 for job {}.", job);

		} catch (Exception e) {
			throw new IOException("Folder '" + url + "' could not be deleted.", e);
		}
	}

	@Override
	public void cleanup(String job) throws IOException {
		if (!S3Util.isValidS3Uri(location)) {
			throw new IOException("Output Url '" + location + "' is not a valid S3 bucket.");
		}

		String temp = location + "/" + job + "/" + TEMP_DIRECTORY;
		try {
			log.info("Deleting temp directory for {} on S3 workspace: '{}'...", job, temp);
			S3Util.deleteFolder(temp);
			log.info("Deleted all files on S3 for job {}.", job);
		} catch (Exception e) {
			throw new IOException("Folder '" + temp + "' could not be deleted.", e);
		}

		String input = location + "/" + job + "/" + INPUT_DIRECTORY;
		try {
			log.info("Deleting input directory for " + input + " on S3 workspace: '" + input + "'...");
			S3Util.deleteFolder(input);
			log.info("Deleted all files on S3 for job " + job + ".");
		} catch (Exception e) {
			throw new IOException("Folder '" + input + "' could not be deleted.", e);
		}
	}

	/**
	 * Creates a pre-signed S3 URL allowing time-limited public access to the object
	 * at the provided S3 URI. Expires in {@link S3Workspace#EXPIRATION_MS}.
	 */
	@Override
	public String createPublicLink(String uri) {
		log.debug("Generating pre-signed URL for {}...", uri);
		S3Util.UriParts uriParts = S3Util.getParts(uri);
		URL publicUrl = S3Util.generatePresignedLink(uriParts, Duration.ofMillis(EXPIRATION_MS));
		log.debug("Pre-signed URL for {} generated. Link: {}", uri, publicUrl.toString());
		return publicUrl.toString();
	}

	@Override
	public String getParent(String uri) {
		if (uri.startsWith("s3://")) {
			int index = uri.lastIndexOf('/');
			if (index > 0) {
				return uri.substring(0, index);
			}
			return null;
		} else {
			return null;
		}
	}

	// TODO(Marc): Rename! No file is created (only a path string).
	@Override
	public String createFolder(String id) {
		return location + "/" + job + "/" + OUTPUT_DIRECTORY + "/" + id;
	}

	// TODO(Marc): Rename! No file is created (only a path string).
	@Override
	public String createFile(String folder, String id) {
		return location + "/" + job + "/" + OUTPUT_DIRECTORY + "/" + folder + "/" + id;
	}

	// TODO(Marc): Rename! No file is created (only a path string).
	@Override
	public String createLogFile(String id) {
		return location + "/" + job + "/" + LOGS_DIRECTORY + "/" + id;
	}

	// TODO(Marc): Rename! No file is created (only a path string).
	@Override
	public String createTempFolder(String id) {
		return location + "/" + job + "/" + TEMP_DIRECTORY + "/" + id;
	}

	@Override
	public List<Download> getDownloads(String uri) {
		List<Download> downloads = new ArrayList<>();

		S3Util.UriParts uriParts = S3Util.getParts(uri);
		List<S3Object> listing = S3Util.listObjects(uriParts);

		for (S3Object summary : listing) {
			String key = summary.key();

			if (key.endsWith("/")) {
				continue;
			}

			String filename = key.replaceAll(uriParts.key() + "/", "");
			String size = FileUtils.byteCountToDisplaySize(summary.size());
			String hash = HashUtil.getSha256(filename + size + (Math.random() * 100_000));

			if (filename.equals("cloudgene.out")) {
				continue;
			}

			Download download = new Download();
			download.setName(filename);
			download.setPath("s3://" + uriParts.bucket() + "/" + key);
			download.setSize(size);
			download.setHash(hash);
			downloads.add(download);
		}

		return downloads;
	}

	@Override
	public List<Download> getLogs() {
		String uri = location + "/" + job + "/" + LOGS_DIRECTORY;
		return getDownloads(uri);
	}
}
