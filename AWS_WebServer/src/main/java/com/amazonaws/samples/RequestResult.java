package com.amazonaws.samples;

public class RequestResult {

	private int id;
	private String answer;
	private String requestUrl;
	private volatile boolean completed;

	public int getId() {
		return this.id;
	}

	public String getRequestUrl() {
		return this.requestUrl;
	}

	public boolean getCompleted() {
		return this.completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getAnswer() {
		return this.answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public RequestResult(int id, String requestUrl) {
		this.id = id;
		this.requestUrl = requestUrl;
		this.answer = "";
	}

}