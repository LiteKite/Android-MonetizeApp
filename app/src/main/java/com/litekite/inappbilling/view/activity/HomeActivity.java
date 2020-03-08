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

package com.litekite.inappbilling.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.litekite.inappbilling.R;
import com.litekite.inappbilling.databinding.ActivityHomeBinding;
import com.litekite.inappbilling.view.fragment.dialog.BillingPremiumDialog;
import com.litekite.inappbilling.viewmodel.BillingVM;
import com.litekite.inappbilling.viewmodel.HomeVM;

/**
 * HomeActivity, users can view their purchases and buy products from store by tapping respective
 * buttons on the ViewGroup.
 *
 * @author Vignesh S
 * @version 1.0, 08/03/2018
 * @since 1.0
 */
public class HomeActivity extends BaseActivity {

	private ActivityHomeBinding homeBinding;

	/**
	 * A Premium Purchase Observer, observes about whether the Premium Purchase has been already
	 * purchased by user or not. If it was purchased, user has granted access for accessing View
	 * Your Purchases and Buy From Store. These two features are locked otherwise, and the user
	 * needs to purchase this in order to use those features.
	 */
	private Observer<Boolean> isPremiumPurchasedObserver = new Observer<Boolean>() {
		@Override
		public void onChanged(@Nullable Boolean aBoolean) {
			if (aBoolean != null) {
				// Dismisses BillingPremiumDialog after successful purchase of Premium Feature.
				if (aBoolean) {
					BillingPremiumDialog.dismiss(HomeActivity.this);
				}
				HomeVM.setDrawableRight(homeBinding.btnBuyFromStore, aBoolean);
				HomeVM.setDrawableEnd(homeBinding.btnBuyFromStore, aBoolean);
				HomeVM.setDrawableRight(homeBinding.btnViewYourPurchases, aBoolean);
				HomeVM.setDrawableEnd(homeBinding.btnViewYourPurchases, aBoolean);
				homeBinding.executePendingBindings();
			}
		}
	};

	/**
	 * Launches HomeActivity.
	 *
	 * @param context An Activity Context.
	 */
	public static void start(@NonNull Context context) {
		if (context instanceof AppCompatActivity) {
			Intent intent = new Intent(context, HomeActivity.class);
			context.startActivity(intent);
			startActivityAnimation(context);
		}
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		homeBinding = DataBindingUtil.setContentView(this, R.layout.activity_home);
		init();
	}

	/**
	 * Sets Toolbar.
	 * Initializes Presenter HomeViewModel, BillingViewModel and registers LifeCycle Observers.
	 * Observes Premium Purchase.
	 */
	private void init() {
		setToolbar(homeBinding.tbWidget.findViewById(R.id.toolbar),
				false,
				getString(R.string.home),
				homeBinding.tbWidget.findViewById(R.id.tv_toolbar_title));
		HomeVM homeVM = new ViewModelProvider(this).get(HomeVM.class);
		BillingVM billingVM = new ViewModelProvider(this).get(BillingVM.class);
		homeBinding.setPresenter(homeVM);
		this.getLifecycle().addObserver(homeVM);
		this.getLifecycle().addObserver(billingVM);
		homeVM.getIsPremiumPurchased().observe(this, isPremiumPurchasedObserver);
	}

}