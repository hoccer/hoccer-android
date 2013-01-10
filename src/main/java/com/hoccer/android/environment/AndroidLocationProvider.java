package com.hoccer.android.environment;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.hoccer.client.environment.EnvironmentProvider;

/**
 * Base class for android location providers
 * 
 * This class integrates a single android location provider as an
 * environment provider for the Hoccer client.
 * 
 * Currently, subclasses must provide:
 * 
 *  - an environment tag string
 *  - the name of the location provider
 *  - an android context
 *  
 * Everything else will be handled by this class.
 * 
 * Note that this class can cope with the situation where the given
 * location provider is not supported by the device, so it is safe
 * to register without checking for support.
 * 
 * @author ingo
 */
public class AndroidLocationProvider extends EnvironmentProvider {
	
	public static String TAG_GPS = "gps";
	public static String TAG_NETWORK = "network";

	
	/** Android context */
	Context mContext;
	
	/** Name used for child node in environment */
	String mEnvironmentTag;
	
	/** Name of location provider */
	String mProviderName;
	
	/** The systems location manager */
	LocationManager mLocationManager;
	
	/** Our location listener */
	LocationListener mLocationListener;
	
	/** Last location received from the provider */
	Location mLastLocation;
	
	LocationThread mThread;
	
	/**
	 * Default super constructor
	 * 
	 * @param pTag used in environment
	 * @param pContext to use
	 * @param pProviderName of location provider
	 */
	protected AndroidLocationProvider(String pTag, Context pContext, String pProviderName) {
		super(pTag);
		
		mContext = pContext;
		
		mEnvironmentTag = pTag;
		
		mProviderName = pProviderName;
		
		mLocationListener = new Listener();
	}
	
	/**
	 * Start function
	 */
	@Override
	public void start() {
		mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
		
		if(mLocationManager != null) {
			List<String> availableProviders = mLocationManager.getAllProviders();
			if(!availableProviders.contains(mProviderName)) {
				LOG.info("Location provider " + mProviderName + " not available");
				return;
			}
			
			mThread = new LocationThread();
			mThread.start();
		}
	}

	/**
	 * Stop function
	 */
	@Override
	public void stop() {
		if(mThread != null) {
			mThread.shutdown();
			mThread = null;
		}
	}

	/**
	 * Update the given environment with current data
	 */
	@Override
	public void updateEnvironment(JSONObject pEnvironment) throws JSONException {
		if(mLastLocation != null) {
			JSONObject root = new JSONObject();
						
			root.put("timestamp", mLastLocation.getTime());
			root.put("latitude", mLastLocation.getLatitude());
			root.put("longitude", mLastLocation.getLongitude());
			
			// XXX results look wrong
			root.put("accuracy", Math.round(mLastLocation.getAccuracy()));
			
			pEnvironment.put(mEnvironmentTag, root);
		}
	}
	
	/**
	 * Internal: location listener
	 *
	 * This updates our cached location whenever appropriate, triggering
	 * an environment submission at the next opportunity.
	 *
	 */
	private class Listener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			LOG.info("Location for provider " + mProviderName + " has changed");
			mLastLocation = location;
			dataChanged();
		}

		@Override
		public void onProviderDisabled(String provider) {
			LOG.info("Location provider " + mProviderName + " disabled");
			mLastLocation = null;
			dataChanged();
		}

		@Override
		public void onProviderEnabled(String provider) {
			LOG.info("Location provider " + mProviderName + " enabled");
			mLastLocation = mLocationManager.getLastKnownLocation(mProviderName);
			dataChanged();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {			
		}
		
	}
	
	private class LocationThread extends Thread {
		Handler mStopHandler;
		
		@Override
		public void run() {
			LOG.fine("Location thread for " + mProviderName + " started");
			
			Looper.prepare();
			
			LOG.fine("Requesting location from provider " + mProviderName);

			// XXX magic constants
			try {
				mLocationManager.requestLocationUpdates(
						mProviderName, 15000, 50, mLocationListener);
			} catch (RuntimeException ex) {
				ex.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			mStopHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					LOG.fine("Location thread for " + mProviderName + " shutting down");
					getLooper().quit();
				}
			};
			
			Looper.loop();
			
			LOG.fine("Unrequesting location from provider " + mProviderName);
			
			mLocationManager.removeUpdates(mLocationListener);
			
			LOG.fine("Location thread for " + mProviderName + " finished");
		}
		
		public void shutdown() {
			if(mStopHandler != null) {
				mStopHandler.sendEmptyMessage(0);
			}
		}
	}
	
	public static AndroidLocationProvider getGpsProvider(Context pContext) {
		return new AndroidLocationProvider(TAG_GPS,
										   pContext,
										   LocationManager.GPS_PROVIDER);
	}
	
	public static AndroidLocationProvider getNetworkProvider(Context pContext) {
		return new AndroidLocationProvider(TAG_NETWORK,
				   						   pContext,
				   						   LocationManager.NETWORK_PROVIDER);
	}
	
}
