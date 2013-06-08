package com.github.mmin18.safelooper;


import java.io.PrintWriter;
import java.io.StringWriter;

import com.github.mmin18.safelooper.R;
import com.github.mmin18.safelooper.R.id;
import com.github.mmin18.safelooper.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener,
		Thread.UncaughtExceptionHandler {
	Thread.UncaughtExceptionHandler androidExceptionHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// save android default handler, it will show you the force close dialog
		androidExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

		setContentView(R.layout.activity_main);
		findViewById(R.id.install).setOnClickListener(this);
		findViewById(R.id.crash).setOnClickListener(this);
		findViewById(R.id.crash_in_background).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.crash:
			// crash directly in main thread
			throw new RuntimeException("Opps!!");

		case R.id.crash_in_background:
			// create a background thread to crash
			new Thread("crash-in-background") {
				@Override
				public void run() {
					throw new RuntimeException("Opps in background!!");
				}
			}.start();
			break;

		case R.id.install:
			if (!SafeLooper.isSafe()) {

				// install SafeLooper in main thread
				SafeLooper.install();
				SafeLooper.setUncaughtExceptionHandler(this);
				// process any uncaught exception in other threads
				Thread.setDefaultUncaughtExceptionHandler(this);

				// update UI
				((TextView) v).setText("Uninstall SafeLooper");
				TextView t = (TextView) findViewById(R.id.status);
				t.setText("SafeLooper is ON");
				t.setBackgroundColor(0xFF00A000);

			} else {

				SafeLooper.uninstall();
				Thread.setDefaultUncaughtExceptionHandler(androidExceptionHandler);

				// update UI
				((TextView) v).setText("Install SafeLooper");
				TextView t = (TextView) findViewById(R.id.status);
				t.setText("SafeLooper is OFF");
				t.setBackgroundColor(0xFFA00000);

			}
			break;
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		// print the crash info on the screen
		StringWriter buf = new StringWriter();
		buf.append("Crashed in " + thread + "\n\n");
		PrintWriter w = new PrintWriter(buf);
		ex.printStackTrace(w);
		final String txt = buf.toString();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				((TextView) findViewById(R.id.text)).setText(txt);
			}
		});
	}

}
