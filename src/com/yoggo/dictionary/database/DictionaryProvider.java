package com.yoggo.dictionary.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;

public class DictionaryProvider extends ContentProvider{
	final String LOG_DEBUG = "DictionaryLog";
	
	//constants for DB
	static final String DB_NAME = "dictionary_db";
	static final int DB_VERSION = 1;
	
	//table
	static  final String DICTIONARY_TABLE = "dictionary";
	
	//table's fields
	public static final String WORD_ID = "_id";
	public static final String RU_WORD = "ru_word";
	public static final String EN_WORD = "en_word";
	public static final String DATE_USE = "date_use";
	
	//database creating
	static final String DB_CREATE = "CREATE TABLE " + DICTIONARY_TABLE + "("
			+ WORD_ID + " integer primary key autoincrement, " 
			+ RU_WORD + " text, "
			+ EN_WORD + " text, "
			+ DATE_USE + " text "
			+ ");";
	
	//uri
	static final String AUTHORITY = "com.yoggo.dictionary.database.DictionaryProvider";
	
	//path
	static final String DICTIONARY_PATH = "Dictionary";
	
	//full uri
	public static final Uri DICTIONARY_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + DICTIONARY_PATH);
	
	//type, set of strings
	static final String DICTIONARY_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
			+ AUTHORITY + "." + DICTIONARY_PATH;
	
	//type, one string
	static final String DICTIONARY_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
		   + AUTHORITY + "." + DICTIONARY_PATH;
	
	//general uri
	static final int URI_DICTIONARY = 1;
	
	//uri with id
	static final int URI_DICTIONARY_ID = 2;
	
	//creating UriMatcher
	private static final UriMatcher uriMatcher;
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, DICTIONARY_PATH, URI_DICTIONARY);
		uriMatcher.addURI(AUTHORITY, DICTIONARY_PATH + "/#", URI_DICTIONARY_ID);
	}
	
	private DBHelper dbHelper;
	private SQLiteDatabase db;
	
	public boolean onCreate(){
		dbHelper = new DBHelper(getContext());
		return true;
	}
	
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder){
		//switch uri
		switch(uriMatcher.match(uri)){
		case URI_DICTIONARY:
			//if sorting is not specified, puts it by name
			if(TextUtils.isEmpty(sortOrder)){
				sortOrder =   DATE_USE + " DESC ";
			}
			break;
		case URI_DICTIONARY_ID:
			String id = uri.getLastPathSegment();
			Log.d(LOG_DEBUG, "URI_CONTACTS_ID, " + id);
			if(TextUtils.isEmpty(selection)){
				selection = WORD_ID + " = " +id;
			}else{
				selection = selection + " AND " + WORD_ID + " = " +id;
			}
			break;
		default:
			throw new IllegalArgumentException("Wrong URI: " + uri);
		}
		
		db = dbHelper.getWritableDatabase();
		Cursor cursor = db.query(DICTIONARY_TABLE, projection, selection, selectionArgs,
				null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), DICTIONARY_URI);
		return cursor;
	}
	
	public Uri insert(Uri uri, ContentValues values){
		if(uriMatcher.match(uri) != URI_DICTIONARY){
			throw new IllegalArgumentException("Wrong URI: " + uri);
		}
		db = dbHelper.getWritableDatabase();
		long rowId = db.insert(DICTIONARY_TABLE, null, values);
		Uri resultUri = ContentUris.withAppendedId(DICTIONARY_URI, rowId);
		getContext().getContentResolver().notifyChange(resultUri, null);
		return resultUri;
	}
	
	public int delete(Uri uri, String selection, String[] selectionArgs){
		switch(uriMatcher.match(uri)){
		case URI_DICTIONARY:
			break;
		case URI_DICTIONARY_ID:
			String id = uri.getLastPathSegment();
			Log.d(LOG_DEBUG, "URI_CONTACTS_ID, " + id);
			if(TextUtils.isEmpty(selection)){
				selection = WORD_ID + " = " + id;
			}else{
				selection = selection + " AND " + WORD_ID + " = " + id;
			}
			break;
		default:
			throw new IllegalArgumentException("Wrong URI: " + 	uri);
		}
		db = dbHelper.getWritableDatabase();
		int cnt = db.delete(DICTIONARY_TABLE, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return cnt;
	}
	
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs){
		switch(uriMatcher.match(uri)){
		case URI_DICTIONARY:
			break;
		case URI_DICTIONARY_ID:
			String id = uri.getLastPathSegment();
			if(TextUtils.isEmpty(selection)){
				selection = WORD_ID + " = " + id;
			}else{
				selection = selection + " AND "+ WORD_ID + " = " + id;
			}
			break;
		default:
			throw new IllegalArgumentException("Wrong URI: " + uri);
		}
		db = dbHelper.getWritableDatabase();
		int cnt = db.update(DICTIONARY_TABLE, values, selection, selectionArgs);
		return cnt;
	}
	
	public String getType(Uri uri){
		switch(uriMatcher.match(uri)){
		case URI_DICTIONARY:
			return DICTIONARY_CONTENT_TYPE;
		case URI_DICTIONARY_ID:
			return DICTIONARY_CONTENT_ITEM_TYPE;
		}
		return null;
	}
	
	private class DBHelper extends SQLiteOpenHelper{
		
		public DBHelper(Context context){
			super(context, DB_NAME, null, DB_VERSION);
		}
		
		public void onCreate(SQLiteDatabase db){
			db.execSQL(DB_CREATE);

		}
		

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
			
		}
	}
	
}
			
	