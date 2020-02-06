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

package com.litekite.inappbilling.viewmodel;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.litekite.inappbilling.billing.BillingManager;
import com.litekite.inappbilling.billing.BillingUpdatesListener;
import com.litekite.inappbilling.network.NetworkManager;
import com.litekite.inappbilling.view.activity.BaseActivity;

/**
 * BillingVM, a view model which provides BillingManager which has BillingClient, a Google Play
 * Billing Library helps to make purchases, gets Sku Details, gets recent purchases list and the
 * list of history of purchases made by the user.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class BillingVM extends AndroidViewModel implements
		LifecycleObserver,
		BillingUpdatesListener {

	private static final String TAG = BillingVM.class.getName();
	private BillingManager billingManager;

	/**
	 * A Broadcast Receiver which will be called if there was a Network Connectivity Change.
	 * Initiates BillingManager if there is a Network Connection.
	 */
	private BroadcastReceiver networkBrReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (NetworkManager.isOnline(context)) {
				BaseActivity.printLog(TAG, "Network Connected");
				initPlayBilling();
			}
		}
	};

	/**
	 * Initializes BillingManager.
	 *
	 * @param application An Application Instance.
	 */
	public BillingVM(@NonNull Application application) {
		super(application);
		initPlayBilling();
	}

	/**
	 * Initializes BillingManager.
	 */
	private void initPlayBilling() {
		billingManager = new BillingManager(this.getApplication(), this);
	}

	/**
	 * Gives BillingManager Instance.
	 *
	 * @return a BillingManager Instance.
	 */
	@NonNull
	public BillingManager getBillingManager() {
		return billingManager;
	}

	@Override
	public void onBillingError(@NonNull String error) {
		BaseActivity.showToast(this.getApplication(), error);
	}

	@SuppressWarnings("unused")
	@OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
	void onCreate() {
		NetworkManager.registerNetworkBrReceiver(this.getApplication(), networkBrReceiver);
	}

	@SuppressWarnings("unused")
	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	void onDestroy() {
		billingManager.destroy();
		NetworkManager.unregisterNetworkBrReceiver(this.getApplication(), networkBrReceiver);
	}

}