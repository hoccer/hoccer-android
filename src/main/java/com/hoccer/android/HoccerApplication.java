package com.hoccer.android;

import android.app.Application;

public class HoccerApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		AndroidLogHandler.engage();
	}

}
