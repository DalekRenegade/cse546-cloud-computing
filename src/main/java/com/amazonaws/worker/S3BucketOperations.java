package com.amazonaws.worker;

import java.nio.file.Paths;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class S3BucketOperations {
	
	public static Bucket getBucket(String bucketName) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		Bucket named_bucket = null;
		List<Bucket> buckets = s3.listBuckets();

		for (Bucket b : buckets) {
			if (b.getName().equals(bucketName))
				named_bucket = b;
		}
		return named_bucket;
	}

	public static Bucket createBucket(String bucketName) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		Bucket newBucket = null;
		System.out.println("Creating the bucket");
		if (s3.doesBucketExist(bucketName)) {
			System.out.println("Bucket already exists");
			newBucket = getBucket(bucketName);
		} else
			newBucket = s3.createBucket(bucketName);

		return newBucket;
	}

	public static void putObject(String bucketName, String filePath) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		String keyName = Paths.get(filePath).getFileName().toString();
		System.out.println("Putting the object");
		s3.putObject(new PutObjectRequest(bucketName, keyName, filePath));
	}

	public static void deleteObject(String bucketName, String keyName) {
		final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
		System.out.println("Deleting the object");
		s3.deleteObject(bucketName, keyName);
	}
}
