package com.amazonaws.samples;

import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class RequestController {

	HashMap<String, Instance> dictDeepLearningInstances;
	ConcurrentHashMap<Integer, RequestResult> dictRequestDetails;
	
	int uniqueUrlIdCounter;
	int dlModelsTotal, requestTotal, requestProcessing, requestDone;
	
	private String queueName;
	private final AmazonSQS sqs;
	private final AmazonEC2 ec2;
	private static RequestController controller;
	
	public HashMap<String, Instance> getDeepLearningInstances() {
		return dictDeepLearningInstances;
	}
	
	public ConcurrentHashMap<Integer, RequestResult> getDictRequestDetails() {
		return dictRequestDetails;
	}
	
	public int getUniqueUrlId() {
		return ++uniqueUrlIdCounter;
	}
	
	public static RequestController getInstance() {
		if(controller == null)
			controller = new RequestController("cse546-project1-sqsUserRequests");
		return controller;
	}
	
	private RequestController(String queueName) {
		uniqueUrlIdCounter = 0;
		dlModelsTotal = 20;
		requestTotal = requestProcessing = requestDone = 0;
		
		this.queueName = queueName;
		dictDeepLearningInstances = new HashMap<String, Instance>(20);
		dictRequestDetails = new ConcurrentHashMap<Integer, RequestResult>();
		ec2 = AmazonEC2ClientBuilder.defaultClient();
		sqs=AmazonSQSClientBuilder.defaultClient();
		CreateQueueResult qResult = sqs.createQueue(queueName);
	}

	public int enqueueUrl(String url) {
		
		RequestResult reqRes = new RequestResult(this.getUniqueUrlId(), url);
		dictRequestDetails.put(reqRes.getUrlId(), reqRes);
		Instance instance = createInstance();
		dictDeepLearningInstances.put(instance.getInstanceId(), instance);
		
		String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
		String queueMessage = String.format("%s,%s", reqRes.getUrlId(), url);
		SendMessageRequest msgReq = new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(queueMessage);
		sqs.sendMessage(msgReq);
		System.out.println("URL queued. Id = " + reqRes.getUrlId());
		
		return reqRes.getUrlId();
	}
	
	public Instance createInstance() {
		System.out.println("Creating instance...");
		String imageId = "ami-07585467";
		int instanceCount = 1;
		RunInstancesRequest riReq = new RunInstancesRequest(imageId, instanceCount, instanceCount);
		riReq.setInstanceType("t2.micro");
		
		RunInstancesResult riRes = ec2.runInstances(riReq);
		Instance resultInstance = riRes.getReservation().getInstances().get(0);
		System.out.println("Instance created: " + resultInstance.getInstanceId());
		return resultInstance;
	}

}
