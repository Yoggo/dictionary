package com.yoggo.dictionary.json;

import com.google.gson.annotations.SerializedName;

public class Matches {
	@SerializedName("id")
	public String id;
	
	@SerializedName("segment")
	public String segment;
	
	@SerializedName("translation")
	public String translation;
	
	@SerializedName("quality")
	public String quality;
	
	@SerializedName("reference")
	public String reference;
	
	@SerializedName("usage-count")
	public String usageCount;
	
	@SerializedName("subject")
	public String subject;
	
	@SerializedName("created-by")
	public String createdBy;
	
	@SerializedName("last-update-by")
	public String lastUpdateBy;
	
	@SerializedName("create-date")
	public String createDate;
	
	@SerializedName("last-update-date")
	public String lastUpdateDate;
	
	@SerializedName("tm-properties")
	public String tmProperties;
	
	@SerializedName("match")
	public String match;
}
