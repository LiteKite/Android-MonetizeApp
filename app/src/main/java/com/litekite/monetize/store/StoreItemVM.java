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
package com.litekite.monetize.store;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.ObservableField;
import com.android.billingclient.api.SkuDetails;
import com.litekite.monetize.R;
import com.litekite.monetize.billing.BillingConstants;
import com.litekite.monetize.billing.BillingManager;
import com.litekite.monetize.room.entity.BillingPurchaseDetails;
import com.litekite.monetize.room.entity.BillingSkuDetails;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import com.litekite.monetize.util.DateTimeUtil;
import java.util.List;
import org.json.JSONException;

/**
 * StoreItemVM, a Presenter which provides Store Product Item that has product sku details, name,
 * price and its purchase details.
 *
 * <p>Handles Click Event Actions from View and performs Purchase Flow.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class StoreItemVM {

    public final ObservableField<String> skuProductName = new ObservableField<>();
    public final ObservableField<String> skuProductPrice = new ObservableField<>();
    public final ObservableField<Boolean> isAlreadyPurchased = new ObservableField<>();
    private final Context context;
    private final BillingManager billingManager;
    private final List<BillingPurchaseDetails> productPurchaseDetails;
    private final BillingSkuDetails skuProductDetails;

    /**
     * Initializes Store Product Item attributes.
     *
     * @param context An AppCompatActivity Context.
     * @param billingManager Provides access to BillingClient which perform Product Purchases from
     *     Google Play Billing Library.
     * @param productRelatedPurchases contains Products with its Sku Details and its related
     *     Purchases.
     */
    public StoreItemVM(
            @NonNull Context context,
            @NonNull BillingManager billingManager,
            @NonNull BillingSkuRelatedPurchases productRelatedPurchases) {
        this.context = context;
        this.billingManager = billingManager;
        this.skuProductDetails = productRelatedPurchases.billingSkuDetails;
        this.productPurchaseDetails = productRelatedPurchases.billingPurchaseDetails;
        init();
    }

    @BindingAdapter("storeItemSrcCompat")
    public static void setStoreItemSrcCompat(
            @NonNull ImageView iv, @NonNull String skuProductName) {
        int drawableRes =
                skuProductName.equals(iv.getContext().getString(R.string.one_apple))
                        ? R.drawable.ic_apple
                        : R.drawable.ic_popcorn;
        iv.setImageResource(drawableRes);
    }

    /**
     * Initializes Store Product Item and sets its respective values and tells the updates to the
     * view.
     */
    private void init() {
        skuProductPrice.set(skuProductDetails.skuPrice);
        if (skuProductDetails.skuID.equals(BillingConstants.SKU_BUY_APPLE)) {
            // This is Apple.
            skuProductName.set(context.getString(R.string.one_apple));
            // For apple, it can be bought multiple times.
            isAlreadyPurchased.set(Boolean.FALSE);
        } else {
            // This is Popcorn.
            skuProductName.set(context.getString(R.string.unlimited_popcorn));
            checkPopcornPurchaseStatus();
        }
    }

    /**
     * Checks whether the Popcorn Store Product Item was purchased or not and it tells the updates
     * to the view.
     *
     * <p>"Buy" option will be available if it was not purchased. "Purchased" will be shown
     * otherwise.
     */
    private void checkPopcornPurchaseStatus() {
        // Unlimited popcorn was not purchased yet.
        if (productPurchaseDetails.size() <= 0) {
            isAlreadyPurchased.set(Boolean.FALSE);
            return;
        }
        long productPurchaseTimeInMillis =
                productPurchaseDetails.get(productPurchaseDetails.size() - 1).purchaseTime;
        // Test Subscriptions are valid for 5 minutes from the purchase time. For
        // production release, add 30 days to the purchase time that gives the expiry date of
        // subscription
        productPurchaseTimeInMillis =
                productPurchaseTimeInMillis + DateTimeUtil.FIVE_MINUTES_IN_MILLIS;
        // Unlimited popcorn purchase was expired if true, and need to buy again.
        if (DateTimeUtil.isDateTimePast(productPurchaseTimeInMillis)) {
            isAlreadyPurchased.set(Boolean.FALSE);
            return;
        }
        // Popcorn was already purchased, no need to buy again.
        isAlreadyPurchased.set(Boolean.TRUE);
    }

    /**
     * Handles Click Events from View.
     *
     * @param v A view in which the click action performed.
     */
    public void onClick(@NonNull View v) {
        if (v.getId() == R.id.btn_product_buy) {
            initPurchaseFlow();
        }
    }

    /** Performs Purchase Flow through BillingClient of Google Play Billing Library. */
    private void initPurchaseFlow() {
        try {
            SkuDetails skuDetails = new SkuDetails(skuProductDetails.originalJson);
            billingManager.initiatePurchaseFlow((Activity) context, skuDetails);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
