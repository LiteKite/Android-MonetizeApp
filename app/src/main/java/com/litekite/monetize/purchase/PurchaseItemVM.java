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
package com.litekite.monetize.purchase;

import android.content.Context;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.ObservableField;
import com.litekite.monetize.R;
import com.litekite.monetize.billing.BillingConstants;
import com.litekite.monetize.room.entity.BillingPurchaseDetails;
import com.litekite.monetize.room.entity.BillingSkuDetails;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import com.litekite.monetize.util.DateTimeUtil;
import java.util.List;

/**
 * PurchaseItemVM, a Presenter which provides Product Item that has product sku details, name, price
 * and its purchase details.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class PurchaseItemVM {

    public final ObservableField<String> skuProductName = new ObservableField<>();
    public final ObservableField<String> skuProductState = new ObservableField<>();
    private final Context context;
    private final BillingSkuDetails skuProductDetails;
    private final List<BillingPurchaseDetails> productPurchaseDetails;

    /**
     * Initializes Product Item attributes.
     *
     * @param context An AppCompatActivity Context.
     * @param productRelatedPurchases contains Products with its Sku Details and its related
     *     Purchases.
     */
    public PurchaseItemVM(
            @NonNull Context context, @NonNull BillingSkuRelatedPurchases productRelatedPurchases) {
        this.context = context;
        this.skuProductDetails = productRelatedPurchases.billingSkuDetails;
        this.productPurchaseDetails = productRelatedPurchases.billingPurchaseDetails;
        init();
    }

    @BindingAdapter("purchaseItemSrcCompat")
    public static void setPurchaseItemSrcCompat(
            @NonNull ImageView iv, @NonNull String skuProductName) {
        int drawableResId =
                skuProductName.equals(iv.getContext().getString(R.string.unlimited_popcorn))
                        ? R.drawable.ic_popcorn
                        : R.drawable.ic_apple;
        iv.setImageResource(drawableResId);
    }

    /**
     * Initializes Product Item and sets its respective values and tells the updates to the view.
     */
    private void init() {
        if (skuProductDetails.skuID.equals(BillingConstants.SKU_BUY_APPLE)) {
            // This is Apple.
            String productName =
                    context.getResources()
                            .getQuantityString(R.plurals.apples, productPurchaseDetails.size());
            skuProductName.set(productName);
            String productState =
                    context.getResources()
                            .getQuantityString(
                                    R.plurals.qty,
                                    productPurchaseDetails.size(),
                                    productPurchaseDetails.size());
            skuProductState.set(productState);
        } else {
            // This is Popcorn.
            skuProductName.set(context.getString(R.string.unlimited_popcorn));
            checkPopcornPurchaseStatus();
        }
    }

    /**
     * Checks whether the Popcorn Product Item was purchased or not and it tells its Purchase Status
     * the updates to the view.
     */
    private void checkPopcornPurchaseStatus() {
        // Unlimited popcorn was not purchased yet.
        if (productPurchaseDetails.size() <= 0) {
            skuProductState.set(context.getString(R.string.not_purchased_yet));
            return;
        }
        long productPurchaseTimeInMillis =
                productPurchaseDetails.get(productPurchaseDetails.size() - 1).purchaseTime;
        // Test Subscriptions are valid for 5 minutes from the purchase time. For
        // production release, add 30 days to the purchase time that gives the expiry date of
        // subscription
        productPurchaseTimeInMillis =
                productPurchaseTimeInMillis + DateTimeUtil.FIVE_MINUTES_IN_MILLIS;
        // Expiry Date of Subscription
        String productExpiryDateTime = DateTimeUtil.getDateTime(productPurchaseTimeInMillis);
        // Unlimited popcorn purchase was expired if true.
        if (DateTimeUtil.isDateTimePast(productPurchaseTimeInMillis)) {
            skuProductState.set(
                    context.getString(R.string.purchase_expired, productExpiryDateTime));
            return;
        }
        // Unlimited popcorn purchase is active. For apple, it can be bought multiple times.
        skuProductState.set(
                context.getString(R.string.purchased_with_expiry, productExpiryDateTime));
    }
}
