package com.amazonaws.samples;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class RequestController {

	//List<String> listInstances = Collections.synchronizedList(new ArrayList<String>());

	ConcurrentHashMap<String, Instance> dictDeepLearningInstances;
	ConcurrentHashMap<Integer, RequestResult> dictRequestDetails;

	AtomicInteger requestCount, responseCount, runningInstancesCount;
	AtomicInteger uniqueIdCounter;
	final AtomicInteger dlModelsTotal;

	private final AmazonSQS sqs;
	private final AmazonEC2 ec2;
	private static RequestController controller;
	private String ec2ImageId, ec2InstanceType, queueName, securityGroup, iamRole;
	private int factor, ec2InstanceLimit;
	private final List<String> securityGroupIds;
	private final IamInstanceProfileSpecification iamSpec;
	
	
	public ConcurrentHashMap<String, Instance> getDeepLearningInstances() {
		return dictDeepLearningInstances;
	}

	public ConcurrentHashMap<Integer, RequestResult> getDictRequestDetails() {
		return dictRequestDetails;
	}

	public int getUniqueId() {
		return uniqueIdCounter.incrementAndGet();
	}

	public int incrementAndGetRequestCount() {
		return requestCount.incrementAndGet();
	}

	public int incrementAndGetResponseCount() {
		return responseCount.incrementAndGet();
	}

	public int addAndGetRunningInstancesCount(int delta) {
		return runningInstancesCount.addAndGet(delta);
	}

	public int getRequestCount() {
		return requestCount.get();
	}

	public int getResponseCount() {
		return responseCount.get();
	}

	public int getRunningInstancesCount() {
		return runningInstancesCount.get();
	}

	public int getEc2InstanceLimit() {
		return ec2InstanceLimit;
	}

	public boolean continueSQSProcessing() {
		return (getRequestCount() - getResponseCount() - getRunningInstancesCount()) > 0;
	}

	public void setControllerProperties(String ec2ImageId, String ec2InstanceType, String queueName, 
			String securityGroup, String iamRole, int factor, int ec2InstanceLimit) {
		if(ec2ImageId != null && ec2ImageId.length() > 0)
			this.ec2ImageId = ec2ImageId;
		if(ec2InstanceType != null && ec2InstanceType.length() > 0)
			this.ec2InstanceType = ec2InstanceType;
		if(queueName != null && queueName.length() > 0)
			this.queueName = queueName;
		if(factor > 0)
			this.factor = factor;
		if(ec2InstanceLimit > 0)
			this.ec2InstanceLimit = ec2InstanceLimit;
		if(securityGroup.length() > 0)
			this.securityGroup = securityGroup;
		if(iamRole.length() > 0)
			this.iamRole = iamRole;
		securityGroupIds.add(this.securityGroup);
		iamSpec.withName(this.iamRole);		
	}

	public static RequestController getInstance() {
		if(controller == null)
			controller = new RequestController();
		return controller;
	}

	private RequestController() {
		ec2ImageId = "ami-f6bcaa96";
		ec2InstanceType = "t2.micro";
		queueName = "cse546-project1-sqsUserRequests";
		securityGroup = "sg-3d9ac344";
		iamRole = "cse546-project1-role";
		factor = 2;
		ec2InstanceLimit = 10;
		uniqueIdCounter = new AtomicInteger(0);
		dlModelsTotal = new AtomicInteger(20);
		requestCount = new AtomicInteger(0);
		responseCount =  new AtomicInteger(0);
		runningInstancesCount = new AtomicInteger(0);
		dictDeepLearningInstances = new ConcurrentHashMap<String, Instance>(20);
		dictRequestDetails = new ConcurrentHashMap<Integer, RequestResult>();
		ec2 = AmazonEC2ClientBuilder.defaultClient();
		securityGroupIds = new LinkedList<String>();
		iamSpec = new IamInstanceProfileSpecification();
		sqs = AmazonSQSClientBuilder.defaultClient();
		sqs.createQueue(queueName);
		Logging.addLog(LogType.Info, "Initializing SQS: " + queueName);
	}

	public int enqueueUrl(String url) {
		RequestResult reqRes = new RequestResult(this.getUniqueId(), url);
		dictRequestDetails.put(reqRes.getUid(), reqRes);
		String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
		String queueMessage = String.format("%s,%s", reqRes.getUid(), url);
		SendMessageRequest msgReq = new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(queueMessage);
		sqs.sendMessage(msgReq);
		Logging.addLog(LogType.Info, String.format("Queued to SQS. URL ID=%d", reqRes.getUid()));
		loadBalancer();
		return reqRes.getUid();
	}

	public synchronized int createInstance(int minInstanceCount, int maxInstanceCount) {
		RunInstancesRequest riReq = new RunInstancesRequest(ec2ImageId, minInstanceCount, maxInstanceCount);
		riReq.setSecurityGroupIds(securityGroupIds);
		riReq.setIamInstanceProfile(iamSpec);
		riReq.setInstanceType(ec2InstanceType);
		System.out.println("ci1");
		RunInstancesResult riRes = ec2.runInstances(riReq);
		System.out.println(riRes == null);
		System.out.println("ci2");
		List<Instance> resultInstances = riRes.getReservation().getInstances();
		System.out.println("ci3");
		Logging.addLog(LogType.Info, String.format("Created %d instances of %s type with image id %s", 
				resultInstances.size(), ec2InstanceType, ec2ImageId));
		for(Instance ins : resultInstances)
			Logging.addLog(LogType.Info, String.format("Instance id = %s ", ins.getInstanceId()));
		System.out.println("ci4");
		return resultInstances.size();
	}

	public synchronized void loadBalancer() {
		System.out.println("lb....");
		if(runningInstancesCount.intValue() < ec2InstanceLimit) {
			int newInstanceCount = getRequestCount() - getResponseCount() - (factor * getRunningInstancesCount());
			System.out.println(newInstanceCount + "...");
			int instanceCreatedCount = createInstance(1, newInstanceCount);
			System.out.println("..." + instanceCreatedCount);
			addAndGetRunningInstancesCount(instanceCreatedCount);
		}
	}

	public synchronized void terminateInstance(String instanceId) {
		if(dictDeepLearningInstances.containsKey(instanceId)) {
			TerminateInstancesRequest tiReq = new TerminateInstancesRequest().withInstanceIds(instanceId);
			ec2.terminateInstances(tiReq);
			dictDeepLearningInstances.remove(instanceId);
		}
	}
}
