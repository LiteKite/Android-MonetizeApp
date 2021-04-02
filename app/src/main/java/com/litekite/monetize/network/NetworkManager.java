/*
 * Copyright 2021 LiteKite Startup. All rights reserved.
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
package com.litekite.monetize.network;

import static android.net.ConnectivityManager.NetworkCallback;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.litekite.monetize.base.CallbackProvider;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles Network Connectivity, checks whether network is available and notifies network
 * connectivity status to the registered receivers.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/training/basics/network-ops/index.html">Network
 *     Connectivity Guide</a>
 * @see <a
 *     href="https://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html">
 *     Checking Network Connectivity Status Guide</a>
 * @since 1.0
 */
@Singleton
public class NetworkManager implements CallbackProvider<NetworkManager.NetworkStateCallback> {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ConnectivityManager connMgr;
    private final List<NetworkStateCallback> networkStateCallbacks = new ArrayList<>();
    private final NetworkCallback networkCallback =
            new NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    notifyNetworkState(network);
                }

                @Override
                public void onCapabilitiesChanged(
                        @NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                    notifyNetworkState(network);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    notifyNetworkState(network);
                }
            };
    private boolean networkCallbackRegistered = false;

    @Inject
    public NetworkManager(@NonNull Context context) {
        connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Check Network Connectivity through Connectivity Manager.
     *
     * @param context Activity or Application Context.
     * @return boolean value of whether the network has internet connectivity or not.
     */
    public static boolean isOnline(@NonNull Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr.getActiveNetwork() != null) {
            return connMgr.getNetworkCapabilities(connMgr.getActiveNetwork())
                    .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }

    /**
     * Checks the network availability state and notifies the state to all the registered clients.
     *
     * @param network An active network object.
     */
    private void notifyNetworkState(@NonNull Network network) {
        boolean isAvailable =
                connMgr.getNetworkCapabilities(network)
                        .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        if (isAvailable) {
            handler.post(
                    () -> networkStateCallbacks.forEach(NetworkStateCallback::onNetworkAvailable));
        } else {
            handler.post(() -> networkStateCallbacks.forEach(NetworkStateCallback::onNetworkLost));
        }
    }

    /**
     * Registers a Default Network Callback that will be notified whenever there's a network change
     * happens.
     */
    private void registerNetworkCallback() {
        if (!networkCallbackRegistered && networkStateCallbacks.size() > 0) {
            networkCallbackRegistered = true;
            connMgr.registerDefaultNetworkCallback(networkCallback);
        }
    }

    /** Unregisters Default Network Callback. */
    private void unregisterNetworkCallback() {
        if (networkCallbackRegistered && networkStateCallbacks.size() == 0) {
            networkCallbackRegistered = false;
            connMgr.unregisterNetworkCallback(networkCallback);
        }
    }

    @Override
    public void addCallback(@NonNull NetworkStateCallback cb) {
        if (!networkStateCallbacks.contains(cb)) {
            networkStateCallbacks.add(cb);
            registerNetworkCallback();
        }
    }

    @Override
    public void removeCallback(@NonNull NetworkStateCallback cb) {
        if (networkStateCallbacks.remove(cb)) {
            unregisterNetworkCallback();
        }
    }

    public interface NetworkStateCallback {
        default void onNetworkAvailable() {}

        default void onNetworkLost() {}
    }
}
