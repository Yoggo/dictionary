package com.yoggo.dictionary;

import com.yoggo.dictionary.database.DictionaryProvider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class WordInfoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_word_info);
		Intent intentObject = getIntent();
		String ru_word = intentObject.getStringExtra(DictionaryProvider.RU_WORD);
		String en_word = intentObject.getStringExtra(DictionaryProvider.EN_WORD);
		((TextView)findViewById(R.id.english_word_text_view)).setText(en_word);
		((TextView)findViewById(R.id.russian_word_text_view)).setText(ru_word);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
