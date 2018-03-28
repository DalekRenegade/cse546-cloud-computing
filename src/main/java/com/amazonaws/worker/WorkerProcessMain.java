package com.amazonaws.worker;

import java.io.IOException;
import java.util.List;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.sqs.model.Message;

public class WorkerProcessMain {
	public static final String QUEUE_NAME = "RequestQueue";
	public static final String BUCKET_NAME = "imagerecognitionresults";
	public static final String IMAGE_ID = "ami-e1131781";	// Ubuntu 17.04
	
	public static void main(String args[]) throws IOException {
		List<Instance> newInstances = EC2Operations.createInstance(IMAGE_ID);
		String instanceId = newInstances.get(0).getInstanceId();
		System.out.println("instanceId = " + instanceId);
//		String instanceId = "i-0198d25485a9f9527";
		EC2Operations.startInstance(instanceId);

		// Receive messages from SQS
		Message msgResponse = SQSOperations.receiveMsgFromSQS();
		String message = "";
		
		if (msgResponse != null)
			message = msgResponse.getBody();
		
		System.out.println("Result variable = " + message);

		while (msgResponse != null) {
			message = msgResponse.getBody();
			System.out.println("Result variable = " + message);
			
			///////////////////////
			// Call Image recog API here
			///////////////////////

			// Put results into bucket
			Bucket resultsBucket = S3BucketOperations.getBucket(BUCKET_NAME);
			if (resultsBucket == null) {
				System.out.println("Bucket doesn't exist");
				resultsBucket = S3BucketOperations.createBucket(BUCKET_NAME);
			}
			S3BucketOperations.putObject(BUCKET_NAME, message);	// Put in S3
			SQSOperations.deleteMessage(msgResponse);	// Delete from SQS
			
			// Get next message
			msgResponse = SQSOperations.receiveMsgFromSQS();
		}

//		System.out.println("Terminating the instance");
//		EC2Operations.terminateInstance(instanceId);
	}
}
