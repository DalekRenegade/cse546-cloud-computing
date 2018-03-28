package com.amazonaws.samples;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import com.sun.net.httpserver.HttpExchange;

public class HelperFunctions {
	
	private static boolean performFileValidityCheck = false;
	
	public static void setPerformFileValidityCheck(boolean check) {
		performFileValidityCheck = check;
	}
	
	public static boolean fileExists(String imageUrl){
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

	public static boolean isValidRequestUrl(String url) {
		
		if(!performFileValidityCheck)
			return true;
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

	public static void sendResponse(HttpExchange t, int responseCode, String response) {
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
}
