package com.yoggo.dictionary.loaders;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.tools.TranslateHelper;

public class DictionaryLoader extends Loader<Cursor>{
	
	final String LOG_DEBUG = "DICTIONARY_TAG";
	public final static String ARGS_SEARCH_WORD = "search_word";
	public final static String ARGS_LANG        = "lang";
	
	public final static String LANG_EN = "lang_en";
	public final static String LANG_RU = "lang_ru";
	
	private Context context;
	private GetWord getWord;
	private String searchWord;
	private String lang;
	private Bundle args;
	
	public DictionaryLoader(Context context, Bundle args){
		super(context);
		this.context = context;
		this.args = args;
		if(args != null){
			searchWord = args.getString(ARGS_SEARCH_WORD);
			lang = args.getString(ARGS_LANG);
		}
		if(TextUtils.isEmpty(searchWord)){
			searchWord = "";
		}
		if(TextUtils.isEmpty(lang)){
			lang = LANG_EN;
		}
		
	}
	
	public Bundle getBundleArgs(){
		return args;
	}
	
	@Override
	protected void onStartLoading(){
		super.onStartLoading();
		Log.d(LOG_DEBUG, "onStartLoading");
	}
	
	@Override
	protected void onStopLoading(){
		super.onStopLoading();
		Log.d(LOG_DEBUG, "onStopLoading");
	}
	
	@Override
	protected void onForceLoad(){
		super.onForceLoad();
		Log.d(LOG_DEBUG, "onForceLoad");
		getWord = new GetWord();
		getWord.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchWord);
	}
	
	@Override
	protected void onAbandon(){
		super.onAbandon();
		Log.d(LOG_DEBUG, "onAbandom");
	}
	
	@Override
	protected void onReset(){
		super.onReset();
		Log.d(LOG_DEBUG, "onReset");
	}
	
	class GetWord extends AsyncTask<String, Void, Cursor>{
		
		@Override
		protected Cursor doInBackground(String... params){
			Log.d(LOG_DEBUG, " doInBackground");
			String WHERE;
			//search words by language
			if(lang.equals(LANG_EN)){
				WHERE = DictionaryProvider.EN_WORD + " LIKE " + "'" + params[0] + "%'";
			}else if(lang.equals(LANG_RU)){
				WHERE = DictionaryProvider.RU_WORD + " LIKE " + "'" + params[0] + "%'";
			}else{
				WHERE = "";
			}

			Cursor cursor = context.getContentResolver()
					.query(DictionaryProvider.DICTIONARY_URI, null,
					WHERE, null, null);
			//if local DB is no current word - get it from server
			if(cursor.getCount() == 0){
				Log.d(LOG_DEBUG, "not found");
				HttpClient httpclient = new DefaultHttpClient();
				//set langpair
				String langPair = "en|ru";
				if(lang.equals(LANG_EN)){
					langPair = "en|ru";
				}else if(lang.equals(LANG_RU)){
					langPair = "ru|en";
				}
				//get url from class-helper
				String reqestUrl = TranslateHelper.getReqestUrl(langPair, params[0]);
				Log.d("REQEST_URL", reqestUrl);
				try {
					HttpResponse response = httpclient.execute(new HttpGet(reqestUrl));
					 StatusLine statusLine = response.getStatusLine();
					 if(statusLine.getStatusCode() == HttpStatus.SC_OK){
					        HttpEntity entity = response.getEntity();
					        String responseString = EntityUtils.toString(entity,"UTF-8");
					        Log.d("RESPONSE", responseString);
					        
					    } else{
					        response.getEntity().getContent().close();
					        throw new IOException(statusLine.getReasonPhrase());
					    }
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return cursor;
		}
		
		@Override
		protected void onPostExecute(Cursor cursor){
			deliverResult(cursor);
		}
		
		
	}
}
