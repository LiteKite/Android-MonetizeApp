/*
 * Copyright 2020 LiteKite Startup. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.litekite.inappbilling.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

/**
 * Handles Network Connectivity, checks whether network is available and notifies network
 * connectivity status to the registered receivers.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/training/basics/network-ops/index.html"> Network
 * Connectivity Guide</a>
 * @see <a href="https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html">
 * Checking Network Connectivity Status Guide</a>
 * @since 1.0
 */
public class NetworkManager {

	public static boolean isNetworkConnected = false;

	private NetworkManager() {

	}

	/**
	 * Check Network Connectivity through Connectivity Manager.
	 *
	 * @param context Activity or Application Context.
	 *
	 * @return boolean value of whether the network has internet connectivity or not.
	 */
	public static boolean isOnline(@NonNull Context context) {
		ConnectivityManager connMgr = (ConnectivityManager)
				context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo;
		if (connMgr != null) {
			networkInfo = connMgr.getActiveNetworkInfo();
			isNetworkConnected = networkInfo != null && networkInfo.isConnected();
			return isNetworkConnected;
		}
		return false;
	}

	/**
	 * Registers a Broadcast Receiver that will be triggered whenever there's a network
	 * connectivity actions occurs and connectivity change happens. This receivers which are
	 * registered in the manifest will not be triggered.
	 *
	 * @param context           Activity or Application Context.
	 * @param networkBrReceiver which will be called when any network connectivity actions or
	 *                          changes made.
	 */
	public static void registerNetworkBrReceiver(@NonNull Context context,
	                                             @NonNull BroadcastReceiver networkBrReceiver) {
		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		context.registerReceiver(networkBrReceiver, intentFilter);
	}

	/**
	 * Unregisters Broadcast receiver.
	 *
	 * @param context           Activity or Application Context.
	 * @param networkBrReceiver which will be called when any network connectivity actions or
	 *                          changes made.
	 */
	public static void unregisterNetworkBrReceiver(@NonNull Context context,
	                                               @NonNull BroadcastReceiver networkBrReceiver) {
		context.unregisterReceiver(networkBrReceiver);
	}

}