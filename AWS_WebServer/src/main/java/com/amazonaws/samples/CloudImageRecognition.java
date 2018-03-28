package com.amazonaws.samples;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CloudImageRecognition {

	int port;
	String cloudServiceName, resultServiceName;

	public CloudImageRecognition(String cloudServiceName, String resultServiceName, int port) {
		this.port = port;
		this.cloudServiceName = "/" + cloudServiceName.trim();
		this.resultServiceName = "/" + resultServiceName.trim();
	}

	public void startHttpServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/", new MyHttpHandler());
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			Logging.addLog(LogType.Info, "HTTP service started on port no " + port);
		} catch (IOException e) {
			Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
		}
	}

	class MyHttpHandler implements HttpHandler {

		public void handle(HttpExchange t) throws IOException {
			String method = t.getRequestURI().toString();
			if(method.startsWith(cloudServiceName))
				handleCloudImageRecognitionRequests(t, method);
			else if(method.startsWith(resultServiceName))
				handleResultServiceRequests(t, method);
		}

		private void handleCloudImageRecognitionRequests(HttpExchange t, String method) {
			int urlId = 0;
			String url = "";
			String identifierString = "?input=";
			int identifierStringIndex = method.indexOf(identifierString);
			RequestController controller = RequestController.getInstance();			
			Logging.addLog(LogType.Info, "Request = " + method);

			if(identifierStringIndex >= 0) {
				url = method.substring(identifierStringIndex + identifierString.length());
				if(HelperFunctions.isValidRequestUrl(url)) {
					System.out.println("123456");
					//System.out.println(controller.getRequestCount() + url);
					controller.incrementAndGetRequestCount();
					System.out.println(controller.getRequestCount() + url);
					System.out.println(Thread.currentThread().getId());
					urlId = controller.enqueueUrl(url);
					Logging.addLog(LogType.Info, String.format("URL Id %d assigned to request URL", urlId));
				}
				else {
					System.out.println("1235645");
					Logging.addLog(LogType.Warning, String.format("Invalid request URL (Id=%d): %s", urlId, url));
				}
			}
			System.out.println("sdjfhkj");
			if(urlId > 0) {
				ConcurrentHashMap<Integer, RequestResult> dictRequestDetails = controller.getDictRequestDetails();
				RequestResult reqRes = dictRequestDetails.get(urlId);
				
				while(reqRes.getState() == false);
				Logging.addLog(LogType.Info, String.format("DL result(Id=%d | URL=%s): %s", urlId, url, reqRes.getAnswer()));
				HelperFunctions.sendResponse(t, HttpURLConnection.HTTP_OK, reqRes.getAnswer());
				dictRequestDetails.remove(urlId);
			}
			else
				HelperFunctions.sendResponse(t, HttpURLConnection.HTTP_BAD_REQUEST, "");
		}

		private void handleResultServiceRequests(HttpExchange t, String method) {
			int uid = 0;
			String iid = "", result = "";
			try {
				String[] urlParts = method.split("\\?");
				if (urlParts.length > 1) {
					for(String param : urlParts[1].split("&")) {
						String[] pair = param.split("=");
						String key, value;
						if(pair.length > 1) {
							key = URLDecoder.decode(pair[0], "UTF-8");
							value = URLDecoder.decode(pair[1], "UTF-8");
							if(key.equals("uid"))
								uid = Integer.parseInt(value);
							else if(key.equals("iid"))
								iid = value;
							else if(key.equals("result"))
								result = value;
							
						}
					}
				}
				
				RequestController controller = RequestController.getInstance();
				ConcurrentHashMap<String, Instance> dictDeepLearningInstances = controller.getDeepLearningInstances();
				if(dictDeepLearningInstances.containsKey(iid)) {
					ConcurrentHashMap<Integer, RequestResult> dictRequestDetails = controller.getDictRequestDetails();
					if(dictRequestDetails.containsKey(uid)) {
						controller.incrementAndGetResponseCount();
						RequestResult reqRes = dictRequestDetails.get(uid);
						reqRes.setAnswer(result);
						reqRes.setState(true);
						Logging.addLog(LogType.Info, "Valid params in result url: " + method);
					}
					else
						Logging.addLog(LogType.Warning, "Invalid params in result url: " + method);
					boolean continueSqs = controller.continueSQSProcessing();
					HelperFunctions.sendResponse(t, HttpURLConnection.HTTP_OK, String.format("continue=", continueSqs));
					if(continueSqs == false)
						controller.terminateInstance(iid);
				}
				else
					HelperFunctions.sendResponse(t, HttpURLConnection.HTTP_NOT_ACCEPTABLE, "406 NOT ACCEPTABLE");
			}
			catch (Exception e) {
				Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
				HelperFunctions.sendResponse(t, HttpURLConnection.HTTP_BAD_REQUEST, "400 BAD REQUEST");
			}
		}

	}

}