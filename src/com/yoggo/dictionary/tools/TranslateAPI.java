package com.yoggo.dictionary.tools;

import java.util.List;

import com.yoggo.dictionary.json.MyMemory;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface TranslateAPI {
	@GET("/get")
	public void get(@Query("q") String q,
			@Query("langpair") String langpair, Callback<MyMemory> response);
}
