package com.noinnion.android.newsplus.extension.cnn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.provider.IItem;

public class LoginActivity extends Activity implements OnClickListener {
	
	protected ProgressDialog	mBusy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		
		final Context c = getApplicationContext();
		
		String action = getIntent().getAction();
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			logout();
		}
		
		if (Prefs.isLoggedIn(c)) {
			setResult(RESULT_OK);
			finish();
		}		
		
		setContentView(R.layout.login_newsblur);
		
		findViewById(R.id.ok_button).setOnClickListener(this);
	}

	private void logout() {
		final Context c = getApplicationContext();
		Prefs.setLoggedIn(c, false);
		setResult(ReaderExtension.RESULT_LOGOUT);
		finish();
	}
	
	private void login() {
		final Context c = getApplicationContext();
		String url = "http://www.newsblur.com/api/login/"; 
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("username", "asafge");
		params.put("password", "test");

		AQuery aq = new AQuery(this);
		aq.ajax(url, params, JSONObject.class, new AjaxCallback<JSONObject>() {

			@Override
			public void callback(String url, JSONObject json, AjaxStatus status) {
				try
				{
					if (json != null && json.getString("authenticated") == "true") {
						Prefs.setLoggedIn(c, true);
						setResult(ReaderExtension.RESULT_LOGIN);
						finish();
					}
				}
				catch (JSONException e) {
					AQUtility.report(e);
				}
			}
		});
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.ok_button:
				login();
				break;
		}
	}
}
