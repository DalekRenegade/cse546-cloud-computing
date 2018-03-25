package com.amazonaws.samples;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Cse546Project1Main {

	private static int portNo = 0;
	private static String ec2ImageId = "", ec2InstanceType = "", queueName = "";

	public static boolean readConfig() {		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File("./project1.config"));

			NodeList cirNodes = document.getElementsByTagName("CloudImageRecognitionPort");
			NodeList ec2ImageIdNodes = document.getElementsByTagName("EC2ImageId");
			NodeList ec2InstanceTypeNodes = document.getElementsByTagName("EC2InstanceType");
			NodeList queueNameNodes = document.getElementsByTagName("SQSQueueName");

			if(cirNodes != null && cirNodes.getLength() > 0)
				portNo = Integer.parseInt(cirNodes.item(0).getTextContent());
			if(ec2ImageIdNodes != null && ec2ImageIdNodes.getLength() > 0)
				ec2ImageId = ec2ImageIdNodes.item(0).getTextContent();
			if(ec2InstanceTypeNodes != null && ec2InstanceTypeNodes.getLength() > 0)
				ec2InstanceType = ec2InstanceTypeNodes.item(0).getTextContent();
			if(queueNameNodes != null && queueNameNodes.getLength() > 0)
				queueName = queueNameNodes.item(0).getTextContent();
		} catch (Exception e) {
			Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
		}
		return (portNo > 0);
	}

	public static void main(String[] args) {
		try {
			String logsFolder = "./logs";
			File directory = new File(logsFolder);
			if(!directory.exists())
				directory.mkdirs();

			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			String filePath = String.format("%s/log_%s.log", logsFolder, sdf.format(timestamp));
			FileHandler fileHandler = new FileHandler(filePath);

			Logging.initializeLogging(fileHandler);
			fileHandler.setFormatter(new SimpleFormatter());

			if(readConfig()) {
				RequestController controller = RequestController.getInstance();
				controller.setControllerProperties(ec2ImageId, ec2InstanceType, queueName);
				CloudImageRecognition cir = new CloudImageRecognition("cloudimagerecognition", "resultservice", portNo);
				Logging.addLog(LogType.Info, "Starting HTTP service on port no " + portNo);
				cir.startHttpServer();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error : " + e);
		}

	}

}
