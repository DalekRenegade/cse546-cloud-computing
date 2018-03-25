package com.amazonaws.samples;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

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
		} catch (IOException e) {
			Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
		}
	}

	class MyHttpHandler implements HttpHandler {

		private boolean fileExists(String imageUrl){
			try {
				HttpURLConnection.setFollowRedirects(false);
				HttpURLConnection con = (HttpURLConnection) new URL(imageUrl).openConnection();
				con.setRequestMethod("HEAD");
				return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
			}
			catch (Exception e) {
				Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
			}
			return false;
		}

		private boolean isValidRequestUrl(String url) {
			boolean valid = false;
			String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
			for(String extension : allowedExtensions) {
				if(url.endsWith(extension)) {
					valid = true;
					break;
				}
			}
			return valid && fileExists(url);
		}

		private void sendResponse(HttpExchange t, int responseCode, String response) {
			OutputStream os = t.getResponseBody();
			try {
				byte [] responseBytes = response.getBytes();
				t.sendResponseHeaders(responseCode, responseBytes.length);
				os.write(responseBytes);
			}
			catch (IOException e) {
				Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
			}
			finally {
				try { if(os!=null) os.close();} catch (IOException e) {/*Close quietly*/}
			}
		}

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
				if(isValidRequestUrl(url)) {
					urlId = controller.enqueueUrl(url);
					Logging.addLog(LogType.Info, String.format("URL Id %d assigned to URL: %s", urlId, url));
				}
				else
					Logging.addLog(LogType.Warning, String.format("Invalid request URL (Id=%d): %s", urlId, url));
			}
			if(urlId > 0) {
				ConcurrentHashMap<Integer, RequestResult> dictRequestDetails = controller.getDictRequestDetails();
				RequestResult reqRes = dictRequestDetails.get(urlId);
				while(reqRes.getCompleted() == false);
				Logging.addLog(LogType.Info, String.format("DL result(Id=%d | URL=%s): %s", urlId, url, reqRes.getAnswer()));
				sendResponse(t, HttpURLConnection.HTTP_OK, reqRes.getAnswer());
				dictRequestDetails.remove(urlId);
			}
			else
				sendResponse(t, HttpURLConnection.HTTP_BAD_REQUEST, "");
		}

		private void handleResultServiceRequests(HttpExchange t, String method) {
			int id = 0;
			boolean state = false;
			String result = "";

			int responseCode = HttpURLConnection.HTTP_BAD_REQUEST;
			String responseMessage = "400 BAD REQUEST";
			try {
				String[] urlParts = method.split("\\?");
				if (urlParts.length > 1) {
					for(String param : urlParts[1].split("&")) {
						String[] pair = param.split("=");
						String key, value;
						if(pair.length > 1) {
							key = URLDecoder.decode(pair[0], "UTF-8");
							value = URLDecoder.decode(pair[1], "UTF-8");
							if(key.equals("id"))
								id = Integer.parseInt(value);
							else if(key.equals("state"))
								state = Boolean.parseBoolean(value);
							else if(key.equals("result"))
								result = value;
						}
					}
				}
				if(id > 0) {
					RequestController controller = RequestController.getInstance();
					ConcurrentHashMap<Integer, RequestResult> dictRequestDetails = controller.getDictRequestDetails();
					if(dictRequestDetails.containsKey(id)) {
						RequestResult reqRes = dictRequestDetails.get(id);
						reqRes.setAnswer(result);
						responseMessage = "200 OK";
						reqRes.setCompleted(state);
						responseCode = HttpURLConnection.HTTP_OK;
						Logging.addLog(LogType.Info, "Valid params in result url: " + method);
					}
					else
						Logging.addLog(LogType.Warning, "Invalid params in result url: " + method);
				}
			}
			catch (Exception e) {
				Logging.addLog(LogType.Error, Arrays.toString(e.getStackTrace()));
			}
			sendResponse(t, responseCode, responseMessage);
		}

	}

}