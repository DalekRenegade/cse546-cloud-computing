package com.amazonaws.samples;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.services.batch.model.KeyValuePair;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ResultService {
	
	int port;
	String serviceName;
	RequestController controller;
	
	public ResultService(String serviceName, int port) {
		this.port = port;
		this.serviceName = "/" + serviceName.trim();
		this.controller = RequestController.getInstance();
	}

	public void startHttpServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext(serviceName , new ResultServiceHandler());
			server.setExecutor(null);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	class ResultServiceHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			String urlIdIdentifier = "?urlId=";
			String resultIdentifier = "&result=";
			int responseCode = 400;
			String responseMessage = "400 BAD REQUEST";
			
			String method = t.getRequestURI().toString();
			System.out.println(method);
			int urlIdIdentifierIndex = method.indexOf(urlIdIdentifier);
			int resultIdentifierIndex = method.indexOf(resultIdentifier);
			
			if(urlIdIdentifierIndex >= 0 && resultIdentifierIndex > urlIdIdentifierIndex) {
				String urlIdString = method.substring(urlIdIdentifierIndex + urlIdIdentifier.length(), resultIdentifierIndex);
				String result = method.substring(resultIdentifierIndex + resultIdentifier.length());
				int urlId = Integer.parseInt(urlIdString);
				System.out.println("" + urlId + ", " + result);
				
				ConcurrentHashMap<Integer, RequestResult> dictRequestDetails = controller.getDictRequestDetails();
				if(dictRequestDetails.containsKey(urlId)) {
					RequestResult reqRes = dictRequestDetails.get(urlId);
					reqRes.setAnswer(result);
					responseMessage = "200 OK";
					responseCode = 200;
				}
			}
			
			byte [] responseBytes = responseMessage.getBytes();
			t.sendResponseHeaders(responseCode, responseBytes.length);
			OutputStream os = t.getResponseBody();
			os.write(responseBytes);
			os.close();
		}
	}

}