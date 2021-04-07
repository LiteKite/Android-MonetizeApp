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
package com.litekite.monetize.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.litekite.monetize.R;
import com.litekite.monetize.base.BaseActivity;
import com.litekite.monetize.databinding.ActivityHomeBinding;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * HomeActivity, users can view their purchases and buy products from store by tapping respective
 * buttons on the ViewGroup.
 *
 * @author Vignesh S
 * @version 1.0, 08/03/2018
 * @since 1.0
 */
@AndroidEntryPoint
public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding homeBinding;

    /**
     * A Premium Purchase Observer, observes about whether the Premium Purchase has been already
     * purchased by user or not. If it was purchased, user has granted access for accessing View
     * Your Purchases and Buy From Store. These two features are locked otherwise, and the user
     * needs to purchase this in order to use those features.
     */
    private final Observer<Boolean> isPremiumPurchasedObserver =
            value -> {
                boolean isPurchased = value != null ? value : false;
                // Dismisses BillingPremiumDialog after successful purchase of Premium Feature.
                if (isPurchased) {
                    BillingPremiumDialog.dismiss(HomeActivity.this);
                }
                HomeVM.setDrawableRight(homeBinding.btnBuyFromStore, isPurchased);
                HomeVM.setDrawableEnd(homeBinding.btnBuyFromStore, isPurchased);
                HomeVM.setDrawableRight(homeBinding.btnViewYourPurchases, isPurchased);
                HomeVM.setDrawableEnd(homeBinding.btnViewYourPurchases, isPurchased);
                homeBinding.executePendingBindings();
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
     * Sets Toolbar. Initializes Presenter HomeViewModel, BillingViewModel and registers LifeCycle
     * Observers. Observes Premium Purchase.
     */
    private void init() {
        setToolbar(
                homeBinding.tbWidget.toolbar,
                false,
                getString(R.string.home),
                homeBinding.tbWidget.tvToolbarTitle);
        HomeVM homeVM = new ViewModelProvider(this).get(HomeVM.class);
        homeBinding.setPresenter(homeVM);
        this.getLifecycle().addObserver(homeVM);
        homeVM.getIsPremiumPurchased().observe(this, isPremiumPurchasedObserver);
    }
}
