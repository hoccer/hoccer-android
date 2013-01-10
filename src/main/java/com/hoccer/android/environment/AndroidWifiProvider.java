package com.hoccer.android.environment;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.hoccer.client.environment.EnvironmentProvider;

/**
 * Provides a list of visible BSSIDs
 * 
 * It does so by querying the WifiManager for information and
 * triggering on the appropriate broadcast intent for
 * change notification.
 * 
 * Safe to use even on devices without wifi and with disabled wifi.
 * 
 * @author ingo
 */
public class AndroidWifiProvider extends EnvironmentProvider {

	/** Tag used for node in environment */
	private static final String TAG = "wifi";
	
	/** Context used for accessing wifi manager */
	Context mContext;
	
	/** Wifi manager providing our data */
	WifiManager mWifiManager;
	
	/** Broadcast receiver for change notification */
	ScanResultReceiver mScanResultReceiver;
	
	/**
	 * Default ctor
	 */
	public AndroidWifiProvider(Context pContext) {
		super(TAG);
		
		mContext = pContext;
	}
	
	/**
	 * Start function
	 */
	@Override
	public void start() {
		mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		
		if(mWifiManager != null) {
			IntentFilter intentFilter =
					new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			
			LOG.fine("Requesting wifi scan result broadcasts");
			
			mScanResultReceiver = new ScanResultReceiver();
			
			mContext.registerReceiver(mScanResultReceiver, intentFilter);
			
			dataChanged();
		}
	}

	/**
	 * Stop function
	 */
	@Override
	public void stop() {
		mWifiManager = null;
		
		if(mScanResultReceiver != null) {
			LOG.fine("Unrequesting wifi scan result broadcasts");
			mContext.unregisterReceiver(mScanResultReceiver);
			mScanResultReceiver = null;
		}
	}

	/**
	 * Internal: check if wifi is currently enabled
	 * 
	 * Used to check if we really have any information to send.
	 * 
	 * @return true if wifi is enabled
	 */
	private boolean isAvailable() {
		if(mWifiManager != null) {
			int wifiState = mWifiManager.getWifiState();
			
			switch(wifiState) {
			case WifiManager.WIFI_STATE_DISABLED:
			case WifiManager.WIFI_STATE_DISABLING:
			case WifiManager.WIFI_STATE_UNKNOWN:
				return false;
			default:
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Update the given environment with current data
	 */
	@Override
	public void updateEnvironment(JSONObject pEnvironment) throws JSONException {
		if(isAvailable()) {
			// create our root node
			JSONObject root = new JSONObject();
			
			// attach timestamp
			root.put("timestamp", getTimestamp());
			
			// get and attach bssids
			JSONArray idArray = new JSONArray();
			List<ScanResult> scanResults = mWifiManager.getScanResults();
			for(ScanResult scanResult: scanResults) {
				idArray.put(scanResult.BSSID);
			}
			root.put("bssids", idArray);
			
			// attach the whole thing
			pEnvironment.put(TAG, root);
		}
	}
	
	private class ScanResultReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			LOG.info("Wifi station list changed");
			dataChanged();
		}
	}

}
