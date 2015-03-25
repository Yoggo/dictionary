package com.yoggo.dictionary.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.yoggo.dictionary.R;
import com.yoggo.dictionary.WordInfoActivity;
import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.interfaces.ProgressCallback;
import com.yoggo.dictionary.loaders.DictionaryLoader;

public class DictionaryFragment extends Fragment implements
		LoaderCallbacks<Cursor>, ProgressCallback,
		AdapterView.OnItemClickListener {

	final String LOG_DEBUG = "DICTIONARY_LOG";
	public static final int LOADER_DICTIONARY_ID = 1;
	final String STATE_LIST_VIEW = "state_list_view";

	private ListView dictionaryListView;
	private EditText searchEditText;
	private SimpleCursorAdapter adapter;
	private LoaderCallbacks<Cursor> loaderCallbacks;
	private String lang;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_dictionary, container,
				false);
		findViews(view);
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
		lang = getLang("");
		loaderCallbacks = this;
		setSearchTextWatcher(searchEditText);
		Cursor cursor = getActivity().getContentResolver().query(
				DictionaryProvider.DICTIONARY_URI, null, null, null, null);
		updateDictionaryList(cursor, lang);
		dictionaryListView.setOnItemClickListener(this);
		// set bundle for loader
		return view;
	}

	private void restoreState(Bundle savedInstanceState) {
		hiddenKeyboard();
		Parcelable state = savedInstanceState.getParcelable(STATE_LIST_VIEW);
		dictionaryListView.onRestoreInstanceState(state);
	}

	@Override
	public void onResume() {
		super.onResume();
		Loader loader = getLoaderManager().getLoader(LOADER_DICTIONARY_ID);
		if (loader != null && loader.isReset()) {
			getLoaderManager().restartLoader(0, getArguments(), this);
		} else {
			getLoaderManager().initLoader(0, getArguments(), this);
		}

	}

	private void hiddenKeyboard() {
		getActivity().getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Loader<Cursor> loader = null;
		if (id == LOADER_DICTIONARY_ID) {
			loader = new DictionaryLoader(getActivity(), args, this);
			Log.d(LOG_DEBUG, "onCreateLoader");
		}
		return loader;
	}

	private void findViews(View view) {
		dictionaryListView = (ListView) view
				.findViewById(R.id.dictionary_list_view);
		searchEditText = (EditText) view
				.findViewById(R.id.search_word_edit_text);
	}

	public void updateDictionaryList(Cursor cursor, String lang) {
		int to[] = { android.R.id.text1, android.R.id.text2 };
		adapter = null;
		// refresh a list at the specified language
		if (lang.equals(DictionaryLoader.LANG_EN)) {
			String[] from = { DictionaryProvider.EN_WORD,
					DictionaryProvider.RU_WORD };
			adapter = new SimpleCursorAdapter(getActivity(),
					android.R.layout.simple_list_item_2, cursor, from, to, 0);
		} else if (lang.equals(DictionaryLoader.LANG_RU)) {
			String[] from = { DictionaryProvider.RU_WORD,
					DictionaryProvider.EN_WORD };
			adapter = new SimpleCursorAdapter(getActivity(),
					android.R.layout.simple_list_item_2, cursor, from, to, 0);
		}
		if (adapter != null) {
			dictionaryListView.setAdapter(adapter);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
		int loaderId = loader.getId();
		Log.d("Translated", "there");
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
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Cursor cursor = ((SimpleCursorAdapter) adapter).getCursor();
		cursor.moveToPosition(position);
		String ru_word = cursor.getString(cursor
				.getColumnIndex(DictionaryProvider.RU_WORD));
		String en_word = cursor.getString(cursor
				.getColumnIndex(DictionaryProvider.EN_WORD));
		Intent openNewActivity = new Intent(getActivity(),
				WordInfoActivity.class);
		openNewActivity.putExtra(DictionaryProvider.RU_WORD, ru_word);
		openNewActivity.putExtra(DictionaryProvider.EN_WORD, en_word);
		startActivity(openNewActivity);
	}

	@Override
	public void setProgressVisible(boolean isVisible) {
		getActivity().setProgressBarIndeterminateVisibility(isVisible);
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
	public void onSaveInstanceState(Bundle outState) {
		Parcelable state = dictionaryListView.onSaveInstanceState();
		outState.putParcelable(STATE_LIST_VIEW, state);
	}

}
