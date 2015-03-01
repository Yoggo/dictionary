package com.yoggo.dictionary.json;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class MyMemory {
	
	@SerializedName("responseData")
	public ResponseData responseData;
	
	@SerializedName("responseDetails")
	public String responseDetails;
	
	@SerializedName("responseStatus")
	public String responseStatus;
	
	@SerializedName("responderId")
	public String responderId;
	
	@SerializedName("matches")
	public List<Matches> matches;
	
	
}
