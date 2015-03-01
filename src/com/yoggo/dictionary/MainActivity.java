package com.yoggo.dictionary;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.loaders.DictionaryLoader;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor>{
	
	static final int LOADER_DICTIONARY_ID = 1;
	final String LOG_DEBUG = "DICTIONARY_LOG";

	
	
	private ListView dictionaryListView;
	private EditText searchEditText;
	private LoaderCallbacks<Cursor> loaderCallbacks;
	private String lang;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViews();
		loaderCallbacks = this;
		setSearchTextWatcher(searchEditText);
		Cursor cursor = getContentResolver().query(DictionaryProvider.DICTIONARY_URI, null,
				null, null, null);
		String[] from = {"ru_word", "en_word"};
		int to[] = { android.R.id.text1, android.R.id.text2 };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
				android.R.layout.simple_list_item_2, cursor, from, to,0);
		dictionaryListView.setAdapter(adapter);
		//set bundle for loader
		Bundle bundle = new Bundle();
		bundle.putString(DictionaryLoader.ARGS_SEARCH_WORD, searchEditText.getText().toString());
		getLoaderManager().initLoader(LOADER_DICTIONARY_ID, bundle, this);
	}
	
	private void setSearchTextWatcher(EditText editText){
		editText.addTextChangedListener(new TextWatcher(){

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void afterTextChanged(final Editable s) {
				
				Handler handler = new Handler();
				final String currentString = s.toString();
				lang = getLang(s.toString());
				
				//add a delay of one second while loading a word from server
				handler.postDelayed(new Runnable() {
					
				    @Override
				    public void run() {
				    	if (searchEditText.getText().toString().equals(currentString)) {
				    		
							Loader<Cursor> loader = getLoaderManager().getLoader(LOADER_DICTIONARY_ID);
							Bundle bundle = new Bundle();
							bundle.putString(DictionaryLoader.ARGS_SEARCH_WORD, searchEditText.getText().toString());
							if(lang != null){
								bundle.putString(DictionaryLoader.ARGS_LANG, lang);
							}
							bundle.putBoolean(DictionaryLoader.ARGS_FROM_SERVER, true);
							
							loader = getLoaderManager().restartLoader(LOADER_DICTIONARY_ID, bundle,
						    		loaderCallbacks);
						    loader.forceLoad();
						}
				    }
				}, 1000);
				
				//without delay if loading a word from the local database
				Loader<Cursor> loader = getLoaderManager().getLoader(LOADER_DICTIONARY_ID);
				Bundle bundle = new Bundle();
				bundle.putString(DictionaryLoader.ARGS_SEARCH_WORD, searchEditText.getText().toString());
				if(lang != null){
					bundle.putString(DictionaryLoader.ARGS_LANG, lang);
				}
				
				loader = getLoaderManager().restartLoader(LOADER_DICTIONARY_ID, bundle,
			    		loaderCallbacks);
			    loader.forceLoad();

			}
			
		});
	}

	private void findViews(){
		dictionaryListView = (ListView) findViewById(R.id.dictionary_list_view);
		searchEditText = (EditText) findViewById(R.id.search_word_edit_text);
	}
	
	//method for determination of the language
	private String getLang(String string){
		if(string.matches("[a-zA-Z\\s]+")){
			Log.d(LOG_DEBUG, "ENGLISH");
			return DictionaryLoader.LANG_EN;
		}else if(string.matches("[à-ÿÀ-ß\\s]+")){
			Log.d(LOG_DEBUG, "RUSSIAN");
			return DictionaryLoader.LANG_RU;
		}else{
			//default value
			return DictionaryLoader.LANG_RU;
		}
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args){
		Loader<Cursor> loader = null;
		if(id == LOADER_DICTIONARY_ID){
			loader = new DictionaryLoader(this, args, dictionaryListView);
			Log.d(LOG_DEBUG, "onCreateLoader");
		}
		return loader;
	}
	
	public void updateDictionaryList(Cursor cursor, String lang){
		int to[] = { android.R.id.text1, android.R.id.text2 };
		SimpleCursorAdapter adapter = null;
		//refresh a list at the specified language
		if(lang.equals(DictionaryLoader.LANG_EN)){
			String[] from = {DictionaryProvider.EN_WORD, DictionaryProvider.RU_WORD};
			adapter = new SimpleCursorAdapter(getApplicationContext(), 
					android.R.layout.simple_list_item_2, cursor, from, to,0);
		}else if(lang.equals(DictionaryLoader.LANG_RU)){
			String[] from = {DictionaryProvider.RU_WORD, DictionaryProvider.EN_WORD};
			adapter = new SimpleCursorAdapter(getApplicationContext(), 
					android.R.layout.simple_list_item_2, cursor, from, to,0);
		}
		if(adapter != null){
			dictionaryListView.setAdapter(adapter);
		}
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result){
		DictionaryLoader dictionaryLoader = (DictionaryLoader) loader;
		Bundle args = dictionaryLoader.getBundleArgs();
		String lang = args.getString(DictionaryLoader.ARGS_LANG);
		if(lang !=null){
			updateDictionaryList(result, lang);
		}
	}
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader){
		Log.d(LOG_DEBUG, "onLoaderReset");
	}

}
