package com.yoggo.dictionary.json;

import com.google.gson.annotations.SerializedName;

public class ResponseData {
	@SerializedName("translatedText")
	public String translatedText;
	
	@SerializedName("match")
	public String match;
}
