package com.hoccer.android;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.hoccer.android.service.HoccerService;
import com.hoccer.android.service.IHoccerService;
import com.hoccer.util.HoccerLoggers;

public class HoccerActivity extends Activity {
	
	private final static Logger LOG =
			HoccerLoggers.getLogger(HoccerActivity.class);

	IHoccerService mService;

	ServiceConnection mSeviceConnection;
	
	ScheduledExecutorService mTimers;
	
	ScheduledFuture<?> mKeepAliveTimer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LOG.info("onCreate()");
		
		setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LOG.info("onResume()");

		mTimers = Executors.newSingleThreadScheduledExecutor();
		
		mSeviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LOG.info("onServiceConnected(" + name + ")");
				mService = (IHoccerService)service;
				scheduleKeepAlive();
			}
			@Override
			public void onServiceDisconnected(ComponentName name) {
				LOG.info("onServiceDisconnected(" + name + ")");
				shutdownKeepAlive();
			}
		};
		
		Intent serviceIntent =
				new Intent(getApplicationContext(),
						HoccerService.class);
				
		startService(serviceIntent);

		bindService(serviceIntent, mSeviceConnection, BIND_IMPORTANT);
	}

	@Override
	protected void onPause() {
		super.onPause();
		LOG.info("onPause()");
		shutdownKeepAlive();
		if(mSeviceConnection != null) {
			unbindService(mSeviceConnection);
		}
		if(mTimers != null) {
			mTimers.shutdown();
			mTimers = null;
		}
	}
	
	private void scheduleKeepAlive() {
		shutdownKeepAlive();
		mKeepAliveTimer = mTimers.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					mService.keepAlive();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}, 1, 20, TimeUnit.SECONDS);
	}
	
	private void shutdownKeepAlive() {
		if(mKeepAliveTimer != null) {
			mKeepAliveTimer.cancel(false);
			mKeepAliveTimer = null;
		}
	}

}
