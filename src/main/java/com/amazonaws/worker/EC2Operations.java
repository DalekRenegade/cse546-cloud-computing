package com.amazonaws.worker;

import java.util.List;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class EC2Operations {
	
	// Create instance
	public static List<Instance> createInstance(String imageId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		int minInstanceCount = 1; // Create only one instance
		int maxInstanceCount = 1;
		RunInstancesRequest rir = new RunInstancesRequest(imageId, minInstanceCount, maxInstanceCount);
		rir.setInstanceType("t2.micro");
		Region usWest1 = Region.getRegion(Regions.US_WEST_1);
		RunInstancesResult result = ec2.runInstances(rir);

		List<Instance> resultInstances = result.getReservation().getInstances();
		System.out.println(resultInstances.toString());
		for (Instance ins : resultInstances) {
			System.out.println("New instance has been created -> " + ins.getInstanceId()); // print instance ID
		}
		return resultInstances;
	}

	// Start instance
	public static void startInstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceId);
		ec2.startInstances(request);
	}

	// Stop instance
	public static void stopInstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceId);
		ec2.stopInstances(request);
	}

	// Terminate instance
	public static void terminateInstance(String instanceId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();
		TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(instanceId);
		ec2.terminateInstances(request);
	}
}
