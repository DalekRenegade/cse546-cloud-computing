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
	
	private static int portNo = 0, factor = 0, ec2InstanceLimit = 0;
	private static String cirServiceName = "", rsServiceName = "", ec2ImageId = "", ec2InstanceType = "";
	private static String queueName = "", iamRole = "", secGroup = "";

	public static boolean readConfig() {		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File("./project1.config"));

			NodeList portNoNodes = document.getElementsByTagName("ServicePort");
			NodeList cirServiceNameNodes = document.getElementsByTagName("CloudImageRecognitionServiceName");
			NodeList rsServiceNameNodes = document.getElementsByTagName("ResultServiceName");
			NodeList ec2ImageIdNodes = document.getElementsByTagName("EC2ImageId");
			NodeList ec2InstanceTypeNodes = document.getElementsByTagName("EC2InstanceType");
			NodeList queueNameNodes = document.getElementsByTagName("SQSQueueName");
			NodeList secGroupNodes = document.getElementsByTagName("EC2SecurityGroup");
			NodeList iamRoleNodes = document.getElementsByTagName("IAMRole");
			NodeList factorNodes = document.getElementsByTagName("Factor");
			NodeList instanceLimitNodes = document.getElementsByTagName("EC2InstanceLimit");
			NodeList fileValidityCheckNodes = document.getElementsByTagName("PerformFileValidityCheck");
			
			if(portNoNodes != null && portNoNodes.getLength() > 0)
				portNo = Integer.parseInt(portNoNodes.item(0).getTextContent());
			if(cirServiceNameNodes != null && cirServiceNameNodes.getLength() > 0)
				cirServiceName = cirServiceNameNodes.item(0).getTextContent();
			if(rsServiceNameNodes != null && rsServiceNameNodes.getLength() > 0)
				rsServiceName = rsServiceNameNodes.item(0).getTextContent();
			if(ec2ImageIdNodes != null && ec2ImageIdNodes.getLength() > 0)
				ec2ImageId = ec2ImageIdNodes.item(0).getTextContent();
			if(ec2InstanceTypeNodes != null && ec2InstanceTypeNodes.getLength() > 0)
				ec2InstanceType = ec2InstanceTypeNodes.item(0).getTextContent();
			if(queueNameNodes != null && queueNameNodes.getLength() > 0)
				queueName = queueNameNodes.item(0).getTextContent();
			if(secGroupNodes != null && secGroupNodes.getLength() > 0)
				iamRole = secGroupNodes.item(0).getTextContent();
			if(iamRoleNodes != null && iamRoleNodes.getLength() > 0)
				secGroup = iamRoleNodes.item(0).getTextContent();
			if(factorNodes != null && factorNodes.getLength() > 0)
				factor = Integer.parseInt(factorNodes.item(0).getTextContent());
			if(instanceLimitNodes != null && instanceLimitNodes.getLength() > 0)
				ec2InstanceLimit = Integer.parseInt(instanceLimitNodes.item(0).getTextContent());
			if(fileValidityCheckNodes != null && fileValidityCheckNodes.getLength() > 0)
				HelperFunctions.setPerformFileValidityCheck(Boolean.parseBoolean(fileValidityCheckNodes.item(0).getTextContent()));
			Logging.addLog(LogType.Info, String.format(
					"Port=%d | CIS=%s | RS=%s | ami=%s | iType=%s | Queue=%s | secGroup= %s | iamRole=%s | Factor=%d | Limit=%d", 
					portNo, cirServiceName, rsServiceName, ec2ImageId, ec2InstanceType, queueName, secGroup, iamRole, factor, ec2InstanceLimit));
		} catch (Exception e) {
			Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
		}
		return (portNo > 0 && cirServiceName.length() > 0 && rsServiceName.length() > 0 && ec2InstanceLimit > 0);
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
				controller.setControllerProperties(ec2ImageId, ec2InstanceType, queueName, secGroup, iamRole, factor, ec2InstanceLimit);
				CloudImageRecognition cir = new CloudImageRecognition(cirServiceName, rsServiceName, portNo);
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
