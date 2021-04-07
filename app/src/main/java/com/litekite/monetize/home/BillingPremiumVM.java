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

import android.app.Activity;
import android.app.Application;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.android.billingclient.api.SkuDetails;
import com.litekite.monetize.R;
import com.litekite.monetize.billing.BillingCallback;
import com.litekite.monetize.billing.BillingConstants;
import com.litekite.monetize.billing.BillingManager;
import com.litekite.monetize.room.database.AppDatabase;
import com.litekite.monetize.room.entity.BillingSkuDetails;
import com.litekite.monetize.util.ContextUtil;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import org.json.JSONException;

/**
 * BillingPremiumVM, a view model which gets Premium Feature Sku Details from local database, It
 * tells to the view about the changes and updates, Handles View Click Event Actions.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
@HiltViewModel
public class BillingPremiumVM extends AndroidViewModel
        implements LifecycleObserver, BillingCallback {

    private final AppDatabase appDatabase;
    private final BillingManager billingManager;
    private LiveData<BillingSkuDetails> premiumSkuDetails = new MutableLiveData<>();

    /**
     * Makes a call to get Premium Feature Sku Details from local database.
     *
     * @param application An Application Instance.
     * @param billingManager Provides access to BillingClient which perform Product Purchases from
     *     Google Play Billing Library.
     */
    @Inject
    public BillingPremiumVM(
            @NonNull Application application,
            @NonNull AppDatabase appDatabase,
            @NonNull BillingManager billingManager) {
        super(application);
        this.appDatabase = appDatabase;
        this.billingManager = billingManager;
        // Sync with the local database
        fetchFromDB();
    }

    /**
     * Fetches Premium Feature Sku Details stored in the local database and assigns it to {@link
     * #premiumSkuDetails} LiveData.
     */
    private void fetchFromDB() {
        premiumSkuDetails = appDatabase.getSkuDetails(BillingConstants.SKU_UNLOCK_APP_FEATURES);
    }

    /**
     * Handles Click Events from View.
     *
     * @param v A view in which the click action performed.
     */
    public void onClick(@NonNull View v) {
        if (v.getId() == R.id.btn_billing_buy) {
            // Performs Premium Feature Purchase Flow through BillingClient of Google Play
            // Billing Library.
            if (premiumSkuDetails.getValue() != null) {
                BillingSkuDetails billingSkuDetails = premiumSkuDetails.getValue();
                try {
                    SkuDetails skuDetails = new SkuDetails(billingSkuDetails.originalJson);
                    Activity activityContext = ContextUtil.getActivity(v.getContext());
                    if (activityContext != null) {
                        billingManager.initiatePurchaseFlow(activityContext, skuDetails);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * A view gets this Premium Feature LiveData and observes for changes and updates with it.
     *
     * @return a LiveData of Premium Feature Sku Details.
     */
    @NonNull
    public LiveData<BillingSkuDetails> getPremiumSkuDetails() {
        return premiumSkuDetails;
    }
}
