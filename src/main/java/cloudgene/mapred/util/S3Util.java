package cloudgene.mapred.util;

import java.io.File;
import java.io.IOException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public final class S3Util {

	private S3Util() {
	}

	// TODO(Marc): This nomenclature is incorrect. S3 paths starting with s3:// are
	//             URIs, not URLs.
	/**
	 * Separates an S3 URI into its bucket and key parts.
	 *
	 * @param bucket The S3 bucket this URI points at.
	 * @param key    The key / path inside the bucket pointing at this item.
	 */
	public record UrlParts(String bucket, String key) {
	};

	private static AmazonS3 s3;

	private static TransferManager tm;

	/**
	 * Returns a singleton {@link AmazonS3} client.
	 */
	public static AmazonS3 getAmazonS3() {
		if (s3 == null) {
			s3 = AmazonS3ClientBuilder.defaultClient();
		}
		return s3;
	}

	private static TransferManager getTransferManager() {
		if (tm == null) {
			s3 = getAmazonS3();
			tm = TransferManagerBuilder.standard().withS3Client(s3).build();
		}
		return tm;
	}

	/**
	 * Separates the given S3 URI of form {@code s3://<bucket>/<key>} into the
	 * bucket and key, returned as a {@link UrlParts} instance.
	 *
	 * @param uri S3 URI of form {@code s3://<bucket>/<key>}.
	 * @return The bucket and key as fields in a {@link UrlParts} instance.
	 */
	public static UrlParts getParts(String uri) {
		if (!uri.startsWith("s3://")) {
			throw new IllegalArgumentException("S3 URLs must start with 's3://'; found: '" + uri + "'");
		}
		uri = uri.replaceAll("s3://", "");

		String[] rawParts = uri.split("/", 2);

		if (rawParts.length != 2) {
			throw new IllegalArgumentException(
					"S3 URLs must contain at least one forward slash (/) separating the bucket from the key; found: '"
							+ uri + "'");
		}

		return new UrlParts(rawParts[0], rawParts[1]);
	}

	/**
	 * Returns {@code true} if {@code uri} is a valid S3 URI of form
	 * {@code s3://<bucket>/<key>};
	 * returns {@code false} otherwise.
	 */
	public static boolean isValidS3Url(String uri) {
		try {
			getParts(uri);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Downloads the S3 object in {@code uri} into the given local {@code file}
	 * path. Blocking operation.
	 *
	 * @param uri  S3 URI pointing to a downloadable object.
	 * @param file Local path that the file should be downloaded into.
	 * @throws IOException If the download fails.ss
	 */
	public static void copyToFile(String uri, File file) throws IOException {
		UrlParts urlParts = getParts(uri);
		copyToFile(urlParts.bucket(), urlParts.key(), file);
	}

	/**
	 * Downloads the S3 object in {@code s3://<bucket>/<key>} into the given local
	 * {@code file} path. Blocking operation.
	 *
	 * @param bucket S3 bucket to download from.
	 * @param key    Path within the S3 bucket identifying the object to download.
	 * @param file   Local path that the file should be downloaded into.
	 * @throws IOException If the download fails.
	 */
	public static void copyToFile(String bucket, String key, File file) throws IOException {
		TransferManager tm = getTransferManager();
		Download download = tm.download(bucket, key, file);

		try {
			download.waitForCompletion();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Uploads the contents of the provided {@code file} to the S3 bucket and key
	 * specified by the provided {@code uri}, of form {@code s3://<bucket>/<key>}.
	 * Blocking operation.
	 *
	 * @param file Local file to upload into S3.
	 * @param uri  Destination within S3 to upload the file to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(File file, String uri) throws IOException {
		UrlParts urlParts = getParts(uri);
		copyToS3(file, urlParts.bucket(), urlParts.key());
	}

	/**
	 * Uploads the provided {@code contents} to a file in the S3 bucket and key
	 * specified by the provided {@code uri}, of form {@code s3://<bucket>/<key>}.
	 * Blocking operation.
	 *
	 * @param content Data to upload into S3.
	 * @param uri     Destination within S3 to upload the contents to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(String content, String uri) throws IOException {
		UrlParts urlParts = getParts(uri);
		copyToS3(content, urlParts.bucket(), urlParts.key());
	}

	/**
	 * Uploads the contents of the provided {@code file} to the provided S3
	 * {@code bucket}, at the path specified by the given {@code key}.
	 * Blocking operation.
	 *
	 * @param file   Local file to upload into S3.
	 * @param bucket S3 bucket the file will be uploaded to.
	 * @param key    Path within the S3 bucket the file will be uploaded to.
	 * @throws IOException If the upload fails.
	 */
	public static void copyToS3(File file, String bucket, String key) throws IOException {
		TransferManager tm = getTransferManager();
		Upload upload = tm.upload(bucket, key, file);

		try {
			upload.waitForCompletion();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public static void copyToS3(String content, String bucket, String key) throws IOException {
		AmazonS3 s3 = getAmazonS3();
		s3.putObject(bucket, key, content);
	}

	public static ObjectListing listObjects(String url) throws IOException {
		UrlParts urlParts = getParts(url);
		AmazonS3 s3 = getAmazonS3();
		ObjectListing objects = s3.listObjects(urlParts.bucket(), urlParts.key());
		return objects;
	}

	public static void deleteFolder(String url) {
		UrlParts urlParts = getParts(url);
		AmazonS3 s3 = S3Util.getAmazonS3();

		ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
				.withBucketName(urlParts.bucket())
				.withPrefix(urlParts.key());

		ObjectListing objectListing = s3.listObjects(listObjectsRequest);

		while (true) {
			for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
				s3.deleteObject(urlParts.bucket(), objectSummary.getKey());
			}
			if (objectListing.isTruncated()) {
				objectListing = s3.listNextBatchOfObjects(objectListing);
			} else {
				break;
			}
		}
	}
}
