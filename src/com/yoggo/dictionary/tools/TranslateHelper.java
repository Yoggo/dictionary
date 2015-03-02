package com.yoggo.dictionary.tools;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public class TranslateHelper {
	private static final String URL = "http://api.mymemory.translated.net";
	
	public static String getReqestUrl(String langPair, String word){
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair("q", word));
		params.add(new BasicNameValuePair("langpair", langPair));
		String paramsString = URLEncodedUtils.format(params, "utf-8");
		String reqestUrl = URL + "/get?" + paramsString;
		return reqestUrl;
	}
}
