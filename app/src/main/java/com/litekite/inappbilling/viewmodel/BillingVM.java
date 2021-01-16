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

package com.litekite.inappbilling.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.hilt.lifecycle.ViewModelInject;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.litekite.inappbilling.billing.BillingCallback;
import com.litekite.inappbilling.billing.BillingManager;
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
		BillingCallback,
		NetworkManager.NetworkStateCallback {

	private static final String TAG = BillingVM.class.getName();
	private final NetworkManager networkManager;
	private final BillingManager billingManager;

	/**
	 * Initializes BillingManager.
	 *
	 * @param application An Application Instance.
	 */
	@ViewModelInject
	public BillingVM(@NonNull Application application,
	                 @NonNull NetworkManager networkManager,
	                 @NonNull BillingManager billingManager) {
		super(application);
		this.networkManager = networkManager;
		this.billingManager = billingManager;
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
	public void onNetworkAvailable() {
		BaseActivity.printLog(TAG, "onNetworkAvailable: Network Connected");
		connectPlayBilling();
	}

	/**
	 * Connects BillingManager's Play Billing Service if it was not ready.
	 */
	private void connectPlayBilling() {
		billingManager.connectToPlayBillingService();
	}

	@Override
	public void onBillingError(@NonNull String error) {
		BaseActivity.showToast(this.getApplication(), error);
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
	void onCreate() {
		networkManager.addCallback(this);
		billingManager.addCallback(this);
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	void onDestroy() {
		billingManager.removeCallback(this);
		networkManager.removeCallback(this);
	}

}