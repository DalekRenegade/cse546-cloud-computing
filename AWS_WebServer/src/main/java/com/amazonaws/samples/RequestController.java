package com.amazonaws.samples;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

	AtomicInteger uniqueIdCounter;
	int dlModelsTotal, requestTotal, requestProcessing, requestDone;

	private final AmazonSQS sqs;
	private final AmazonEC2 ec2;
	private static RequestController controller;
	private String ec2ImageId, ec2InstanceType, queueName;

	public HashMap<String, Instance> getDeepLearningInstances() {
		return dictDeepLearningInstances;
	}

	public ConcurrentHashMap<Integer, RequestResult> getDictRequestDetails() {
		return dictRequestDetails;
	}

	public int getUniqueId() {
		return uniqueIdCounter.incrementAndGet();
	}

	public void setControllerProperties(String ec2ImageId, String ec2InstanceType, String queueName) {
		if(ec2ImageId != null && ec2ImageId.length() > 0)
			this.ec2ImageId = ec2ImageId;
		if(ec2InstanceType != null && ec2InstanceType.length() > 0)
			this.ec2InstanceType = ec2InstanceType;
		if(queueName != null && queueName.length() > 0)
			this.queueName = queueName;
	}

	public static RequestController getInstance() {
		if(controller == null)
			controller = new RequestController();
		return controller;
	}

	private RequestController() {
		ec2ImageId = "ami-07585467";
		ec2InstanceType = "t2.micro";
		queueName = "cse546-project1-sqsUserRequests";
		uniqueIdCounter = new AtomicInteger(0);
		dlModelsTotal = 20;
		requestTotal = requestProcessing = requestDone = 0;

		this.queueName = queueName;
		dictDeepLearningInstances = new HashMap<String, Instance>(20);
		dictRequestDetails = new ConcurrentHashMap<Integer, RequestResult>();
		ec2 = AmazonEC2ClientBuilder.defaultClient();
		sqs=AmazonSQSClientBuilder.defaultClient();
		CreateQueueResult qResult = sqs.createQueue(queueName);
		Logging.addLog(LogType.Info, "Initializing SQS: " + queueName);
	}

	public int enqueueUrl(String url) {

		RequestResult reqRes = new RequestResult(this.getUniqueId(), url);
		dictRequestDetails.put(reqRes.getId(), reqRes);
		//		Instance instance = createInstance();
		//		dictDeepLearningInstances.put(instance.getInstanceId(), instance);

		String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
		String queueMessage = String.format("%s,%s", reqRes.getId(), url);
		SendMessageRequest msgReq = new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(queueMessage);
		sqs.sendMessage(msgReq);
		Logging.addLog(LogType.Info, String.format("Queued to SQS. ID=%d | URL=%s", reqRes.getId(), url));		
		return reqRes.getId();
	}

	public Instance createInstance() {
		int instanceCount = 1;
		RunInstancesRequest riReq = new RunInstancesRequest(ec2ImageId, instanceCount, instanceCount);
		riReq.setInstanceType(ec2InstanceType);
		RunInstancesResult riRes = ec2.runInstances(riReq);
		Instance resultInstance = riRes.getReservation().getInstances().get(0);
		Logging.addLog(LogType.Info, 
				String.format("%s type EC2 instance (instance id = %s) created with image id %s", 
						ec2InstanceType, resultInstance.getInstanceId(), ec2ImageId));
		return resultInstance;
	}

}
