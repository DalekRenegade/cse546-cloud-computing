package com.amazonaws.samples;

import java.util.concurrent.atomic.AtomicBoolean;

public class RequestResult {

	private int uid;
	private volatile String iid;
	private String answer;
	private String requestUrl;
	private AtomicBoolean state;

	public int getUid() {
		return this.uid;
	}
	
	public String getIid() {
		return this.iid;
	}
	
	public void setIid(String iid) {
		this.iid = iid;
	}

	public String getRequestUrl() {
		return this.requestUrl;
	}

	public boolean getState() {
		return this.state.get();
	}

	public void setState(boolean state) {
		this.state.set(state);
	}

	public String getAnswer() {
		return this.answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public RequestResult(int uid, String requestUrl) {
		this.uid = uid;
		this.iid = "";
		this.requestUrl = requestUrl;
		this.state = new AtomicBoolean(false);
		this.answer = "";
	}

}