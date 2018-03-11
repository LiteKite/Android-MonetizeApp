/*
 * Copyright 2018 LiteKite Startup. All rights reserved.
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
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;

import com.litekite.inappbilling.R;
import com.litekite.inappbilling.billing.BillingConstants;
import com.litekite.inappbilling.network.NetworkManager;
import com.litekite.inappbilling.room.database.AppDatabase;
import com.litekite.inappbilling.view.activity.BaseActivity;
import com.litekite.inappbilling.view.activity.StoreActivity;
import com.litekite.inappbilling.view.activity.ViewPurchasesActivity;
import com.litekite.inappbilling.view.fragment.dialog.BillingPremiumDialog;

/**
 * HomeVM, a view model that gives Premium Feature Purchase Status from local database to View,
 * Handles View's Click Event Actions.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class HomeVM extends AndroidViewModel implements LifecycleObserver {

	private LiveData<Boolean> isPremiumPurchased = new MutableLiveData<>();

	/**
	 * Makes a call to check whether the Premium Feature was purchased and stored in the local
	 * database.
	 *
	 * @param application An Application Instance.
	 */
	public HomeVM(@NonNull Application application) {
		super(application);
		fetchFromDB();
	}

	@BindingAdapter("android:drawableEnd")
	public static void setDrawableEnd(Button button, Boolean isPremiumPurchased) {
		setBtnDrawableRightEnd(button, isPremiumPurchased);
	}

	@BindingAdapter("android:drawableRight")
	public static void setDrawableRight(Button button, Boolean isPremiumPurchased) {
		setBtnDrawableRightEnd(button, isPremiumPurchased);
	}

	/**
	 * Sets two features (View Your Purchases and Buy From Store) locked, if Premium Feature
	 * Product was not purchased, Unlocked otherwise.
	 *
	 * @param button             An instance of a Button Widget.
	 * @param isPremiumPurchased A boolean value represents whether the Premium Feature Product was
	 *                           purchased or not.
	 */
	private static void setBtnDrawableRightEnd(Button button, Boolean isPremiumPurchased) {
		if (isPremiumPurchased != null && isPremiumPurchased) {
			button.setCompoundDrawablesWithIntrinsicBounds(
					0,
					0,
					0,
					0);
		} else {
			button.setCompoundDrawablesWithIntrinsicBounds(
					0,
					0,
					R.drawable.ic_lock_outline_white,
					0);
		}
	}

	/**
	 * A view gets this LiveData of Premium Feature purchased or not and observes for
	 * changes and updates with it.
	 *
	 * @return a LiveData of Premium Feature Purchased or not.
	 */
	public LiveData<Boolean> getIsPremiumPurchased() {
		if (isPremiumPurchased == null) {
			isPremiumPurchased = new MutableLiveData<>();
		}
		return isPremiumPurchased;
	}

	/**
	 * Handles Click Events from View.
	 *
	 * @param v A view in which the click action performed.
	 */
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_buy_from_store:
				if (checkIsPremiumPurchased(v)) {
					StoreActivity.start(v.getContext());
				}
				break;
			case R.id.btn_view_your_purchases:
				if (checkIsPremiumPurchased(v)) {
					ViewPurchasesActivity.start(v.getContext());
				}
				break;
		}
	}

	/**
	 * Fetches and checks whether the Premium Feature was purchased and stored in the local
	 * database and assigns it to {@link #isPremiumPurchased} LiveData.
	 */
	private void fetchFromDB() {
		isPremiumPurchased = AppDatabase.getAppDatabase(this.getApplication()).getBillingDao()
				.getIsThisSkuPurchased(BillingConstants.SKU_UNLOCK_APP_FEATURES);
	}

	/**
	 * Launches BillingPremiumDialog if Premium Purchase was not purchased.
	 * Shows a SnackBar if there is no Internet Connectivity.
	 *
	 * @param v A view in which the click action performed.
	 *
	 * @return whether the Premium Feature Purchased or not.
	 */
	private boolean checkIsPremiumPurchased(View v) {
		Boolean isPurchased = isPremiumPurchased.getValue();
		if (isPurchased != null) {
			if (!isPurchased && !NetworkManager.isNetworkConnected) {
				BaseActivity.showSnackBar(v, R.string.err_no_internet);
				return false;
			}
			if (!isPurchased && NetworkManager.isNetworkConnected) {
				BillingPremiumDialog.show(v.getContext());
				return false;
			}
			return true;
		}
		return false;
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	public void onDestroy() {
		AppDatabase.destroyAppDatabase();
	}

}
