package com.amazonaws.samples;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Cse546Project1Main {
	
	private static int cirPort = 0, rsPort = 0;
	
	public static boolean readConfig() {		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new File("./project1.config"));
			
			NodeList cirNodes = document.getElementsByTagName("CloudImageRecognitionPort");
			NodeList rsNodes = document.getElementsByTagName("ResultServicePort");

			if(cirNodes != null && cirNodes.getLength() > 0)
				cirPort = Integer.parseInt(cirNodes.item(0).getTextContent());
			if(rsNodes != null && rsNodes.getLength() > 0)
				rsPort = Integer.parseInt(rsNodes.item(0).getTextContent());            
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(String.format("CIR Port = %s || RS Port = %s", cirPort, rsPort));
		return (cirPort > 0 && rsPort > 0);
	}
	
	public static void main(String[] args) {
		
		if(readConfig()) {
			RequestController controller = RequestController.getInstance();
			CloudImageRecognition cir = new CloudImageRecognition("cloudimagerecognition", cirPort);
			cir.startHttpServer();
			ResultService rs = new ResultService("resultservice", rsPort);
			rs.startHttpServer();
			
		}
		
	}
	
}
