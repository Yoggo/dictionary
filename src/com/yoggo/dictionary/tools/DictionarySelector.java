package com.yoggo.dictionary.tools;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.yoggo.dictionary.database.DictionaryProvider;
import com.yoggo.dictionary.loaders.DictionaryLoader;

public class DictionarySelector {

	private Context context;

	public DictionarySelector(Context context) {
		this.context = context;

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
	
	public void insertWord(String translatedText, String q, String lang) {
		ContentValues contentValues = new ContentValues();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String date = sdf.format(new Date());
		contentValues.put(DictionaryProvider.DATE_USE, date);
		if (lang.equals(DictionaryLoader.LANG_EN)) {
			contentValues.put(DictionaryProvider.RU_WORD,
					translatedText.toLowerCase());
			contentValues.put(DictionaryProvider.EN_WORD, q.toLowerCase());
		} else if (lang.equals(DictionaryLoader.LANG_RU)) {
			contentValues.put(DictionaryProvider.EN_WORD,
					translatedText.toLowerCase());
			contentValues.put(DictionaryProvider.RU_WORD, q.toLowerCase());
		}
		context.getContentResolver().insert(DictionaryProvider.DICTIONARY_URI,
				contentValues);
	}
}
