package com.hoccer.android.service;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.hoccer.android.environment.AndroidLocationProvider;
import com.hoccer.android.environment.AndroidWifiProvider;
import com.hoccer.api.ClientConfig;
import com.hoccer.client.HoccerClient;
import com.hoccer.client.HoccerPeer;
import com.hoccer.util.HoccerLoggers;

public class HoccerService extends Service {
	
	private static final Logger LOG = HoccerLoggers.getLogger(HoccerService.class);
	
	HoccerClient mClient;
	
	AndroidWifiProvider mWifiProvider;
	AndroidLocationProvider mGpsLocProvider;
	AndroidLocationProvider mNetLocProvider;

	ScheduledExecutorService mTimers;
	
	ScheduledFuture<Void> mStartupFuture;
	ScheduledFuture<Void> mShutdownFuture;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		LOG.info("onCreate()");
		
		Context context = getApplicationContext();
		
		mTimers = Executors.newSingleThreadScheduledExecutor();
		
		mClient = new HoccerClient();
		
		ClientConfig.useProductionServers();
		
		mClient.configure(new ClientConfig(
				"hoccer-android-ng",
				UUID.fromString("1ccce744-1880-11e2-a974-5cff3500d8dd"),
				"b3b03410159c012e7b5a00163e001ab0",
				"ROOCiND4FPqDDwP1taRmdyBejEs="));
		
		mWifiProvider = new AndroidWifiProvider(context);
		mGpsLocProvider = AndroidLocationProvider.getGpsProvider(context);
		mNetLocProvider = AndroidLocationProvider.getNetworkProvider(context);
		
		mClient.registerEnvironmentProvider(mWifiProvider);
		mClient.registerEnvironmentProvider(mGpsLocProvider);
		mClient.registerEnvironmentProvider(mNetLocProvider);
		
		mClient.registerPeerListener(new PeerListener());
		
		doKeepAlive();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LOG.info("onDestroy()");
		mTimers.shutdownNow();
		if(mClient.isRunning()) {
			mClient.stop();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		LOG.info("onBind(" + intent.toUri(0) + ")");
		return new Connection();
	}
	
	private synchronized boolean scheduleStop(int delay) {
		boolean wasRunning = false;
		if(mShutdownFuture != null) {
			mShutdownFuture.cancel(false);
			wasRunning = true;
		}
		mShutdownFuture = mTimers.schedule(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				LOG.info("Terminating");
				if(mClient.isRunning()) {
					mClient.stop();
				}
				stopSelf();
				mShutdownFuture = null;
				return null;
			}
		}, delay, TimeUnit.SECONDS);
		return wasRunning;
	}

	private synchronized void doKeepAlive() {
		if(!mClient.isRunning()) {
			mClient.start();
		}
		
		scheduleStop(30);
	}
	
	class Connection extends IHoccerService.Stub {

		@Override
		public void keepAlive() throws RemoteException {
			LOG.info("Call: keepAlive()");
			doKeepAlive();
		}
		
	}
	
	class PeerListener implements HoccerClient.PeerListener {

		@Override
		public void peerAdded(HoccerPeer peer) {
		}

		@Override
		public void peerRemoved(HoccerPeer peer) {
		}

		@Override
		public void peerUpdated(HoccerPeer peer) {
		}
		
	}

}
