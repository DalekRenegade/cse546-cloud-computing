package com.amazonaws.samples;

import com.amazonaws.services.ec2.model.Instance;

public class RequestResult {
	
	private int urlId;
	private volatile String answer;
	private String requestUrl;
	
	public int getUrlId() {
		return urlId;
	}
	
	public String getRequestUrl() {
		return requestUrl;
	}
	
	public String getAnswer() {
		return answer;
	}
	
	public void setAnswer(String answer) {
		this.answer = answer;
	}
	
	public RequestResult(int urlId, String requestUrl) {
		this.urlId = urlId;
		this.requestUrl = requestUrl;
		answer = "";
	}
	
}
