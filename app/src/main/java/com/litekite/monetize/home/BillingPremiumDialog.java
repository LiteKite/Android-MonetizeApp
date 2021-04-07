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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.litekite.monetize.R;
import com.litekite.monetize.databinding.DialogBillingPremiumBinding;
import com.litekite.monetize.room.entity.BillingSkuDetails;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * BillingPremiumDialog, A PremiumPurchaseDialog which is a BottomSheet which lets user to buy
 * Premium Feature and perform purchase actions from Google Play Billing Library. This Premium
 * Feature is an inApp Product and we won't consume it as it was a one time purchase. Once
 * purchased, no need to purchase again. To buy inApp Products many time, it needs to be consumed
 * otherwise.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
@AndroidEntryPoint
public class BillingPremiumDialog extends BottomSheetDialogFragment {

    private static final String TAG = BillingPremiumDialog.class.getName();
    private DialogBillingPremiumBinding dialogBillingPremiumBinding;

    /**
     * Observes changes and updates about the Premium Feature Sku Product which is stored in the
     * local database.
     *
     * <p>Sets Premium Feature Product Price.
     */
    private final Observer<BillingSkuDetails> premiumSkuDetailsObserver =
            billingSkuDetails -> {
                if (billingSkuDetails != null) {
                    dialogBillingPremiumBinding.tvBillingPrice.setText(billingSkuDetails.skuPrice);
                    dialogBillingPremiumBinding.executePendingBindings();
                }
            };

    /**
     * Launches BillingPremiumDialog.
     *
     * @param context An Activity Context.
     */
    public static void show(@NonNull Context context) {
        if (context instanceof AppCompatActivity) {
            BillingPremiumDialog billingPremiumDialog = new BillingPremiumDialog();
            billingPremiumDialog.setStyle(
                    BottomSheetDialogFragment.STYLE_NORMAL, R.style.MyBottomSheetDialogTheme);
            billingPremiumDialog.show(
                    ((AppCompatActivity) context).getSupportFragmentManager(), TAG);
        }
    }

    /**
     * Dismisses BillingPremiumDialog.
     *
     * @param context An Activity Context.
     */
    public static void dismiss(@NonNull Context context) {
        if (context instanceof AppCompatActivity) {
            BillingPremiumDialog billingPremiumDialog =
                    (BillingPremiumDialog)
                            ((AppCompatActivity) context)
                                    .getSupportFragmentManager()
                                    .findFragmentByTag(TAG);
            if (billingPremiumDialog != null && billingPremiumDialog.isAdded()) {
                billingPremiumDialog.dismiss();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        dialogBillingPremiumBinding =
                DataBindingUtil.inflate(
                        inflater, R.layout.dialog_billing_premium, container, false);
        return dialogBillingPremiumBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    /**
     * Initializes Presenter BillingPremiumViewModel, BillingViewModel and registers LifeCycle
     * Observers. Observes Premium Feature Product Sku Details.
     */
    private void init() {
        BillingPremiumVM billingPremiumVM = new ViewModelProvider(this).get(BillingPremiumVM.class);
        dialogBillingPremiumBinding.setPresenter(billingPremiumVM);
        this.getLifecycle().addObserver(billingPremiumVM);
        billingPremiumVM
                .getPremiumSkuDetails()
                .observe(getViewLifecycleOwner(), premiumSkuDetailsObserver);
    }
}
