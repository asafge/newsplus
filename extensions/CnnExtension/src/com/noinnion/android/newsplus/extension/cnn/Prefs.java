package com.noinnion.android.newsplus.extension.cnn;

import android.content.Context;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

	public static final String KEY_LOGGED_IN = "logged_in";
	public static final String KEY_SESSION_ID = "session_id";

	public static boolean isLoggedIn(Context c) {
		return getBoolean(c, KEY_LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, KEY_LOGGED_IN, value);
	}
	
	public static String getSessionID(Context c) {
		return getString(c, KEY_SESSION_ID);
	}
	
	public static void setSessionID(Context c, String value) {
		putString(c, KEY_SESSION_ID, value);
	}
}
