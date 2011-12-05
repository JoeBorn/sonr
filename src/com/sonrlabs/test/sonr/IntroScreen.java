package com.sonrlabs.test.sonr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ErrorReporter;

import com.sonrlabs.test.sonr.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.text.util.Linkify.*;
import android.view.View;
import android.widget.TextView;

public class IntroScreen extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.intro);
			TextView t2 = (TextView) findViewById(R.id.intro_msg);
			Pattern pattern = Pattern.compile("terms of service and privacy policy");
			TransformFilter transformFilter = new TransformFilter() {
				public final String transformUrl(final Matcher match, String url) {
					return ".TermsScreen://";
				}
			};

			Linkify.addLinks(t2, pattern, "com.sonrlabs.sonr", null, transformFilter);

			t2.setMovementMethod(LinkMovementMethod.getInstance());
		} catch (Exception e) {
			e.printStackTrace();
			ErrorReporter.getInstance().handleException(e);
		}
	}

	@Override
	public void onBackPressed() {
		; // do nothing
	}

	public void acceptTerms(View view) {
		SONR.WritePreferences("DEFAULT, false");
		finish();
	}

	@Override
	public void onPause() {
		super.onPause();
		finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
