package com.yoggo.dictionary.loaders;

import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.json.MyMemory;
import com.yoggo.dictionary.json.ResponseData;
import com.yoggo.dictionary.tools.TranslateAPI;

public class DictionaryLoader extends Loader<Cursor>{
	
	final String LOG_DEBUG = "DICTIONARY_TAG";
	public static final String ENDPOINT = "http://api.mymemory.translated.net";
	public final static String ARGS_SEARCH_WORD = "search_word";
	public final static String ARGS_FROM_SERVER = "from_server";
	public final static String ARGS_LANG        = "lang";
	
	public final static String LANG_EN = "lang_en";
	public final static String LANG_RU = "lang_ru";
	
	private Context context;
	private GetWord getWord;
	private String searchWord;
	private String lang;
	private Bundle args;
	private boolean fromServer;
	private ListView dictionaryListView;
	
	public DictionaryLoader(Context context, Bundle args, ListView dictionaryListView){
		super(context);
		this.context = context;
		this.args = args;
		this.dictionaryListView = dictionaryListView;
		if(args != null){
			searchWord = args.getString(ARGS_SEARCH_WORD);
			lang = args.getString(ARGS_LANG);
			fromServer = args.getBoolean(ARGS_FROM_SERVER);
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
		
		private Cursor cursor;
		private String word;
		
		private Cursor getCursor(String word){
			String WHERE = "";
			//if pause passed search full word
			if(fromServer && !searchWord.equals("")){
				//search words by language
				if(lang.equals(LANG_EN)){
					WHERE = DictionaryProvider.EN_WORD + " = " + "'" + word + "'";
				}else if(lang.equals(LANG_RU)){
					WHERE = DictionaryProvider.RU_WORD + " = " + "'" + word + "'";
				}
			}else{
				//search words by language
				if(lang.equals(LANG_EN)){
					WHERE = DictionaryProvider.EN_WORD + " LIKE " + "'" + word + "%'";
				}else if(lang.equals(LANG_RU)){
					WHERE = DictionaryProvider.RU_WORD + " LIKE " + "'" + word + "%'";
				}
			}
			
			Log.d("WHERE", WHERE);
			cursor = context.getContentResolver()
					.query(DictionaryProvider.DICTIONARY_URI, null,
					WHERE, null, null);
			return cursor;
		}
		
		@Override
		protected Cursor doInBackground(String... params){
			Log.d(LOG_DEBUG, " doInBackground");
			this.word = params[0];
			cursor = getCursor(word);
			//if local DB is no current word - get it from server
			if(cursor.getCount() == 0 && fromServer){
				Log.d(LOG_DEBUG, "not found");
				String langPair = "en|ru";
				if(lang.equals(LANG_EN)){
					langPair = "en|ru";
				}else if(lang.equals(LANG_RU)){
					langPair = "ru|en";
				}
				requestData(params[0], langPair);
			}
			return cursor;
		}
		
		private void requestData(final String q, final String langPair){
			RestAdapter adapter = new RestAdapter.Builder()
			            .setEndpoint(ENDPOINT)
			            .build();
			TranslateAPI api = adapter.create(TranslateAPI.class);
			api.get(q,langPair,new Callback<MyMemory>(){
				@Override
				public void success(MyMemory args0, Response args1){
					if(args0 != null){
						ResponseData responseData = args0.responseData;
						String translatedText = responseData.translatedText;
						//check the word
						if(translatedText != null
								&& !translatedText.equals("")
								&& !translatedText.toLowerCase().equals(q.toLowerCase())){
							String WHERE = "";
					        ContentValues contentValues = new ContentValues();
					        if(lang.equals(LANG_EN)){
					        	 contentValues.put(DictionaryProvider.RU_WORD, translatedText.toLowerCase());
					        	 contentValues.put(DictionaryProvider.EN_WORD, q.toLowerCase());
					        	 WHERE = DictionaryProvider.EN_WORD + " LIKE " + "'" + word + "%'";
							}else if(lang.equals(LANG_RU)){
					        	 contentValues.put(DictionaryProvider.EN_WORD, translatedText.toLowerCase());
					        	 contentValues.put(DictionaryProvider.RU_WORD, q.toLowerCase());
					        	 WHERE = DictionaryProvider.RU_WORD + " LIKE " + "'" + word + "%'";
							}
					        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					        String date = sdf.format(new Date());
					        contentValues.put(DictionaryProvider.DATE_USE, date);
					        context.getContentResolver().insert(DictionaryProvider.DICTIONARY_URI, contentValues);
							//update cursor for the new word in the database
							cursor = context.getContentResolver()
									.query(DictionaryProvider.DICTIONARY_URI, null,
									WHERE, null, null);
							//set an adapter to the list of words
							int to[] = { android.R.id.text1, android.R.id.text2 };
							SimpleCursorAdapter adapter = null;
							//refresh a list at the specified language
							if(lang.equals(DictionaryLoader.LANG_EN)){
								String[] from = {"en_word", "ru_word"};
								adapter = new SimpleCursorAdapter(context, 
										android.R.layout.simple_list_item_2, cursor, from, to,0);
							}else if(lang.equals(DictionaryLoader.LANG_RU)){
								String[] from = {"ru_word", "en_word"};
								adapter = new SimpleCursorAdapter(context, 
										android.R.layout.simple_list_item_2, cursor, from, to,0);
							}
							if(adapter != null){
								dictionaryListView.setAdapter(adapter);
							}
						}
					}
					
				}
				
				@Override
				public void failure(RetrofitError arg0){
					Log.d("ERROR", arg0.getMessage());
				}
			});
			
		}
		
		@Override
		protected void onPostExecute(Cursor cursor){
				deliverResult(cursor);
		}
		
		
	}
}
