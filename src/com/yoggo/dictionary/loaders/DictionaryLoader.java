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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.yoggo.dictionary.MainActivity;
import com.yoggo.dictionary.R;
import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.interfaces.ProgressCallback;
import com.yoggo.dictionary.json.MyMemory;
import com.yoggo.dictionary.json.ResponseData;
import com.yoggo.dictionary.tools.DictionarySelector;
import com.yoggo.dictionary.tools.TranslateAPI;

public class DictionaryLoader extends Loader<Cursor> {

	final String LOG_DEBUG = "DICTIONARY_TAG";
	public static final String ENDPOINT = "http://api.mymemory.translated.net";
	public final static String ARGS_SEARCH_WORD = "search_word";
	public final static String ARGS_FULL_WORD = "full_word";
	public final static String ARGS_LANG = "lang";

	public final static String LANG_EN = "lang_en";
	public final static String LANG_RU = "lang_ru";

	private Context context;
	private String searchWord;
	private String lang;
	private Bundle args;
	private boolean fromServer;
	private Cursor cursor;
	DictionarySelector dictionarySelector;
	private ProgressCallback progressCallback;

	public DictionaryLoader(Context context, Bundle args, ProgressCallback progressCallback) {
		super(context);
		this.context = context;
		this.progressCallback = progressCallback;
		this.args = args;
		if (args != null) {
			searchWord = args.getString(ARGS_SEARCH_WORD);
			lang = args.getString(ARGS_LANG);
			fromServer = args.getBoolean(ARGS_FULL_WORD);

		}
		if (TextUtils.isEmpty(searchWord)) {
			searchWord = "";
		}
		if (TextUtils.isEmpty(lang)) {
			lang = LANG_EN;
		}
		dictionarySelector = new DictionarySelector(context);
	}

	public Bundle getBundleArgs() {
		return args;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		Log.d(LOG_DEBUG, "onStartLoading");
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		Log.d(LOG_DEBUG, "onStopLoading");
	}

	@Override
	protected void onForceLoad() {
		super.onForceLoad();
		Log.d(LOG_DEBUG, "onForceLoad");
		getResult(checkWord());
	}

	@Override
	protected void onAbandon() {
		super.onAbandon();
		Log.d(LOG_DEBUG, "onAbandom");
	}

	@Override
	protected void onReset() {
		super.onReset();
		Log.d(LOG_DEBUG, "onReset");
	}
	
	private String getLangPair(){
		String langPair = "en|ru";
		if (lang.equals(LANG_EN)) {
			langPair = "en|ru";
		} else if (lang.equals(LANG_RU)) {
			langPair = "ru|en";
		}
		return langPair;
	}
	public Cursor getCursor(String searchWord,
			boolean isFullWord, String lang) {
		String where = getWhere(isFullWord, searchWord, lang);

		Cursor cursor = context.getContentResolver().query(
				DictionaryProvider.DICTIONARY_URI, null, where, null, null);
		return cursor;
	}

	private String getWhere(boolean isFullWord, String searchWord, String lang) {
		String where = "";
		// if pause passed search full word
		if (isFullWord && !searchWord.equals("")) {
			// search words by language
			if (lang.equals(DictionaryLoader.LANG_EN)) {
				where = DictionaryProvider.EN_WORD + " = " + "'" + searchWord
						+ "'";
			} else if (lang.equals(DictionaryLoader.LANG_RU)) {
				where = DictionaryProvider.RU_WORD + " = " + "'" + searchWord
						+ "'";
			}
		} else {
			// search words by language
			if (lang.equals(DictionaryLoader.LANG_EN)) {
				where = DictionaryProvider.EN_WORD + " LIKE " + "'"
						+ searchWord + "%'";
			} else if (lang.equals(DictionaryLoader.LANG_RU)) {
				where = DictionaryProvider.RU_WORD + " LIKE " + "'"
						+ searchWord + "%'";
			}
		}
		return where;
	}

	private Cursor checkWord() {
		Log.d(LOG_DEBUG, " doInBackground");
		cursor = dictionarySelector.getCursor(searchWord,
                fromServer,lang);
		// if local DB is no current word - get it from server
		if (cursor.getCount() == 0 && fromServer) {
			// check network connection
			if (!isNetworkConnected()) {
				return cursor;
			}
			progressCallback.setProgressVisible(true);
			String langPair = getLangPair();
			
			requestData(searchWord, langPair);
		}
		return cursor;
	}

	private boolean isNetworkConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null) {
			return false;
		} else {
			return true;
		}
	}

	private void requestData(final String q, final String langPair) {
		
		RestAdapter adapter = new RestAdapter.Builder().setEndpoint(ENDPOINT)
				.build();
		TranslateAPI api = adapter.create(TranslateAPI.class);
		api.get(q, langPair, new Callback<MyMemory>() {
			@Override
			public void success(MyMemory args0, Response args1) {
				if (args0 != null) {
					ResponseData responseData = args0.responseData;
					String translatedText = responseData.translatedText;
					
					// check the word
					if (translatedText != null
							&& !translatedText.equals("")
							&& !translatedText.toLowerCase().equals(
									q.toLowerCase())) {
						dictionarySelector.insertWord(translatedText, q, lang);
						// update cursor for the new word in the database
						cursor = dictionarySelector.getCursor(q,
				                fromServer,lang);
						deliverResult(cursor);
					}
					progressCallback.setProgressVisible(false);
				}
			}

			@Override
			public void failure(RetrofitError arg0) {
				Log.d("ERROR", arg0.getMessage());
				progressCallback.setProgressVisible(false);
			}
		});

	}

	protected void getResult(Cursor cursor) {
		if (isNetworkConnected()) {
			deliverResult(cursor);
		} else {
			Toast.makeText(context, R.string.no_network_connection,
					Toast.LENGTH_SHORT).show();
			deliverResult(cursor);
		}
	}

}
