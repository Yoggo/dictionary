package com.yoggo.dictionary;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.interfaces.ProgressCallback;
import com.yoggo.dictionary.loaders.DictionaryLoader;

public class MainActivity extends Activity implements LoaderCallbacks<Cursor>,
		MenuItem.OnMenuItemClickListener, ProgressCallback,
		AdapterView.OnItemClickListener {

	static final int LOADER_DICTIONARY_ID = 1;
	final String STATE_LIST_VIEW = "state_list_view";
	final String LOG_DEBUG = "DICTIONARY_LOG";

	private ListView dictionaryListView;
	private EditText searchEditText;
	private SimpleCursorAdapter adapter;
	private LoaderCallbacks<Cursor> loaderCallbacks;
	private String lang;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		findViews();
		lang = getLang("");
		loaderCallbacks = this;
		setSearchTextWatcher(searchEditText);
		Cursor cursor = getContentResolver().query(
				DictionaryProvider.DICTIONARY_URI, null, null, null, null);
		updateDictionaryList(cursor, lang);
		dictionaryListView.setOnItemClickListener(this);
		dictionaryListView.getCheckedItemPosition();
		// set bundle for loader
		Bundle bundle = new Bundle();
		bundle.putString(DictionaryLoader.ARGS_SEARCH_WORD, searchEditText
				.getText().toString());
		getLoaderManager().initLoader(LOADER_DICTIONARY_ID, bundle, this);
	}

	private void setSearchTextWatcher(EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {

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
				// add a delay of one second while loading a word from server
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						if (searchEditText.getText().toString()
								.equals(currentString)) {
							getLoad(true);
						}
					}
				}, 1000);
				getLoad(false);
			}
		});
	}

	private void getLoad(boolean isDelay) {
		Loader<Cursor> loader = getLoaderManager().getLoader(
				LOADER_DICTIONARY_ID);
		Bundle bundle = new Bundle();
		bundle.putString(DictionaryLoader.ARGS_SEARCH_WORD, searchEditText
				.getText().toString());
		// set lang for word
		if (lang != null) {
			bundle.putString(DictionaryLoader.ARGS_LANG, lang);
		}
		// set delay for loading word
		if (isDelay) {
			bundle.putBoolean(DictionaryLoader.ARGS_FULL_WORD, true);
		} else {
			bundle.putBoolean(DictionaryLoader.ARGS_FULL_WORD, false);
		}
		loader = getLoaderManager().restartLoader(LOADER_DICTIONARY_ID, bundle,
				loaderCallbacks);
		loader.forceLoad();
	}

	private void findViews() {
		dictionaryListView = (ListView) findViewById(R.id.dictionary_list_view);
		searchEditText = (EditText) findViewById(R.id.search_word_edit_text);
	}

	// method for determination of the language
	private String getLang(String string) {
		if (string.matches("[a-zA-Z\\s]+")) {
			Log.d(LOG_DEBUG, "ENGLISH");
			return DictionaryLoader.LANG_EN;
		} else if (string.matches("[à-ÿÀ-ß\\s]+")) {
			Log.d(LOG_DEBUG, "RUSSIAN");
			return DictionaryLoader.LANG_RU;
		} else {
			// default value
			return DictionaryLoader.LANG_RU;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		Loader<Cursor> loader = null;
		if (id == LOADER_DICTIONARY_ID) {
			loader = new DictionaryLoader(this, args, this);

			Log.d(LOG_DEBUG, "onCreateLoader");
		}
		return loader;
	}

	public void updateDictionaryList(Cursor cursor, String lang) {
		int to[] = { android.R.id.text1, android.R.id.text2 };
		adapter = null;
		// refresh a list at the specified language
		if (lang.equals(DictionaryLoader.LANG_EN)) {
			String[] from = { DictionaryProvider.EN_WORD,
					DictionaryProvider.RU_WORD };
			adapter = new SimpleCursorAdapter(getApplicationContext(),
					android.R.layout.simple_list_item_2, cursor, from, to, 0);
		} else if (lang.equals(DictionaryLoader.LANG_RU)) {
			String[] from = { DictionaryProvider.RU_WORD,
					DictionaryProvider.EN_WORD };
			adapter = new SimpleCursorAdapter(getApplicationContext(),
					android.R.layout.simple_list_item_2, cursor, from, to, 0);
		}
		if (adapter != null) {
			dictionaryListView.setAdapter(adapter);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
		int loaderId = loader.getId();
		if (loaderId == LOADER_DICTIONARY_ID) {
			DictionaryLoader dictionaryLoader = (DictionaryLoader) loader;
			Bundle args = dictionaryLoader.getBundleArgs();
			String lang = args.getString(DictionaryLoader.ARGS_LANG);
			if (lang != null) {
				updateDictionaryList(result, lang);
			}
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_DEBUG, "onLoaderReset");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem shareItem = menu.findItem(R.id.menu_item_sharing);
		shareItem.setOnMenuItemClickListener(this);
		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_sharing:
			String message = "This is "
					+ getResources().getString(R.string.app_name);
			Intent shareIntent = new Intent();
			shareIntent.setAction(Intent.ACTION_SEND);
			shareIntent.putExtra(Intent.EXTRA_TEXT, message);
			shareIntent.setType("text/plain");
			startActivity(shareIntent);
			break;
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Cursor cursor = ((SimpleCursorAdapter) adapter).getCursor();
		cursor.moveToPosition(position);
		String ru_word = cursor.getString(cursor
				.getColumnIndex(DictionaryProvider.RU_WORD));
		String en_word = cursor.getString(cursor
				.getColumnIndex(DictionaryProvider.EN_WORD));
		Intent openNewActivity = new Intent(getApplicationContext(),
				WordInfoActivity.class);
		openNewActivity.putExtra(DictionaryProvider.RU_WORD, ru_word);
		openNewActivity.putExtra(DictionaryProvider.EN_WORD, en_word);
		startActivity(openNewActivity);
	}

	@Override
	public void setProgressVisible(boolean isVisible) {
		setProgressBarIndeterminateVisibility(isVisible);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Parcelable state = dictionaryListView.onSaveInstanceState();
		outState.putParcelable(STATE_LIST_VIEW, state);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		hiddenKeyboard();
		Parcelable state = savedInstanceState.getParcelable(STATE_LIST_VIEW);
		dictionaryListView.onRestoreInstanceState(state);
	}

	private void hiddenKeyboard() {
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

}
