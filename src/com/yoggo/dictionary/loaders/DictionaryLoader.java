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
	private boolean isNetworkEnable;
	private Cursor cursor;
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
		isNetworkEnable = true;
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
		Cursor cursor = getWordCursor();
		getResult(cursor);
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

	private Cursor getCursor(String word) {
		String where = getWhere();
		// if pause passed search full word

		cursor = context.getContentResolver().query(
				DictionaryProvider.DICTIONARY_URI, null, where, null, null);
		return cursor;
	}

	protected Cursor getWordCursor() {
		Log.d(LOG_DEBUG, " doInBackground");
		cursor = getCursor(searchWord);
		// if local DB is no current word - get it from server
		if (cursor.getCount() == 0 && fromServer) {
			// check network connection
			if (!isNetworkConnected()) {
				isNetworkEnable = false;
				return cursor;
			}
			progressCallback.setProgressVisible(true);
			String langPair = "en|ru";
			if (lang.equals(LANG_EN)) {
				langPair = "en|ru";
			} else if (lang.equals(LANG_RU)) {
				langPair = "ru|en";
			}
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
						String where = getWhere();
						setContentValues(translatedText, q);
						// update cursor for the new word in the database
						cursor = context.getContentResolver().query(
								DictionaryProvider.DICTIONARY_URI, null, where,
								null, null);
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

	private String getWhere() {
		String where = "";
		if (fromServer && !searchWord.equals("")) {
			// search words by language
			if (lang.equals(LANG_EN)) {
				where = DictionaryProvider.EN_WORD + " = " + "'" + searchWord
						+ "'";
			} else if (lang.equals(LANG_RU)) {
				where = DictionaryProvider.RU_WORD + " = " + "'" + searchWord
						+ "'";
			}
		} else {
			// search words by language
			if (lang.equals(LANG_EN)) {
				where = DictionaryProvider.EN_WORD + " LIKE " + "'"
						+ searchWord + "%'";
			} else if (lang.equals(LANG_RU)) {
				where = DictionaryProvider.RU_WORD + " LIKE " + "'"
						+ searchWord + "%'";
			}
		}
		return where;
	}

	private void setContentValues(String translatedText, String q) {
		ContentValues contentValues = new ContentValues();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String date = sdf.format(new Date());
		contentValues.put(DictionaryProvider.DATE_USE, date);
		if (lang.equals(LANG_EN)) {
			contentValues.put(DictionaryProvider.RU_WORD,
					translatedText.toLowerCase());
			contentValues.put(DictionaryProvider.EN_WORD, q.toLowerCase());
		} else if (lang.equals(LANG_RU)) {
			contentValues.put(DictionaryProvider.EN_WORD,
					translatedText.toLowerCase());
			contentValues.put(DictionaryProvider.RU_WORD, q.toLowerCase());
		}
		context.getContentResolver().insert(DictionaryProvider.DICTIONARY_URI,
				contentValues);
	}

	protected void getResult(Cursor cursor) {
		if (isNetworkEnable) {
			deliverResult(cursor);
		} else {
			Toast.makeText(context, R.string.no_network_connection,
					Toast.LENGTH_SHORT).show();
			
			deliverResult(cursor);
		}

	}

}
