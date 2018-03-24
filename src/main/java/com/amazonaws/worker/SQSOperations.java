package com.amazonaws.worker;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SQSOperations {
	public static final String QUEUE_NAME = "RequestQueue";
	public static String[] sendMsgs = new String[5];

	public static void createQueue() {
		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		CreateQueueResult createResult = sqs.createQueue(QUEUE_NAME);
		System.out.println("Ceated queue");
	}

	public static void sendMsg() {
		sendMsgs[0] = "http://visa.lab.asu.edu/cifar-10/0_cat.png";
		sendMsgs[1] = "http://visa.lab.asu.edu/cifar-10/1_ship.png";
		sendMsgs[2] = "http://visa.lab.asu.edu/cifar-10/1_ship.png";
		sendMsgs[3] = "http://visa.lab.asu.edu/cifar-10/2_airplane.png";
		sendMsgs[4] = "http://visa.lab.asu.edu/cifar-10/4_frog.png";

		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
		SendMessageRequest sendMsgReq = null;
		for (int i = 0; i < 5; i++) {
			sendMsgReq = new SendMessageRequest().withQueueUrl(queueUrl).withMessageBody(sendMsgs[i]);
			sqs.sendMessage(sendMsgReq);
		}
		System.out.println("Sent all msgs");
	}

	public static Message receiveMsgFromSQS() {
		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
		ReceiveMessageRequest receiveMsgRequest = new ReceiveMessageRequest(queueUrl);
		receiveMsgRequest.setMaxNumberOfMessages(1);
		List<Message> response = sqs.receiveMessage(receiveMsgRequest).getMessages();
		if(response.size() == 0)
			return null;
		
		return response.get(0);
	}
	
	public static void deleteMessage(Message message) {
		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
		String messageReceiptHandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));
	}
}
