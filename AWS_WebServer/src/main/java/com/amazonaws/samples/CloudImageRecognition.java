package com.amazonaws.samples;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CloudImageRecognition {

	int port;
	String serviceName;

	public CloudImageRecognition(String serviceName, int port) {
		this.port = port;
		this.serviceName = "/" + serviceName.trim();
	}

	public boolean fileExists(String imageUrl){
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) new URL(imageUrl).openConnection();
			con.setRequestMethod("HEAD");
			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void startHttpServer() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext(serviceName, new CloudImageRecognitionHandler());
			server.setExecutor(null);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class CloudImageRecognitionHandler implements HttpHandler {

		public void handle(HttpExchange t) throws IOException {

			RequestController controller = RequestController.getInstance();

			String identifierString = "?input=";
			String method = t.getRequestURI().toString();
			int identifierStringIndex = method.indexOf(identifierString);

			int urlId = 0;
			String url = "";
			if(identifierStringIndex >= 0) {
				url = method.substring(identifierStringIndex + identifierString.length());
				System.out.println(url);

				String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
				for(String extension : allowedExtensions) {
					if(url.endsWith(extension) && fileExists(url)) {
						urlId = controller.enqueueUrl(url);
						break;
					}
				}
			}
			if(urlId > 0) {
				ConcurrentHashMap<Integer, RequestResult> dictRequestDetails = controller.getDictRequestDetails();
				while(true) {
					String ans = dictRequestDetails.get(urlId).getAnswer();
					if(ans.length() > 0) {
						System.out.println("Got an answer...");
						byte [] responseBytes = ans.getBytes();
						t.sendResponseHeaders(200, responseBytes.length);
						OutputStream os = t.getResponseBody();
						os.write(responseBytes);
						os.close();
						dictRequestDetails.remove(urlId);
						break;
					}
				}
			}
		}

	}

}