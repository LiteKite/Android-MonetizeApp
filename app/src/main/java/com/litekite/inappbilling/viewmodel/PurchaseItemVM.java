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

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.ObservableField;
import android.widget.ImageView;

import com.litekite.inappbilling.R;
import com.litekite.inappbilling.billing.BillingConstants;
import com.litekite.inappbilling.room.entity.BillingPurchaseDetails;
import com.litekite.inappbilling.room.entity.BillingSkuDetails;
import com.litekite.inappbilling.room.entity.BillingSkuRelatedPurchases;
import com.litekite.inappbilling.util.DataTimeUtil;

import java.util.List;

/**
 * PurchaseItemVM, a Presenter which provides Product Item that has product sku
 * details, name, price and its purchase details.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class PurchaseItemVM {

	public final ObservableField<String> skuProductName = new ObservableField<>();
	public final ObservableField<String> skuProductState = new ObservableField<>();
	private Context context;
	private BillingSkuDetails skuProductDetails;
	private List<BillingPurchaseDetails> productPurchaseDetails;

	/**
	 * Initializes Product Item attributes.
	 *
	 * @param context                 An AppCompatActivity Context.
	 * @param productRelatedPurchases contains Products with its Sku Details and its related
	 *                                Purchases.
	 */
	public PurchaseItemVM(Context context,
	                      BillingSkuRelatedPurchases productRelatedPurchases) {
		this.context = context;
		this.skuProductDetails = productRelatedPurchases.billingSkuDetails;
		this.productPurchaseDetails = productRelatedPurchases.billingPurchaseDetails;
		init();
	}

	/**
	 * Initializes Product Item and sets its respective values and tells the updates to the view.
	 */
	private void init() {
		// This is Apple.
		if (skuProductDetails.skuID.equals(BillingConstants.SKU_BUY_APPLE)) {
			skuProductName.set(context.getResources().getQuantityString(R.plurals.apples,
					productPurchaseDetails.size()));
			skuProductState.set(context.getString(R.string.qty, productPurchaseDetails.size()));
		} else { // This is Popcorn.
			skuProductName.set(context.getString(R.string.unlimited_popcorn));
			checkPopcornPurchaseStatus();
		}
	}

	/**
	 * Checks whether the Popcorn Product Item was purchased or not and it tells its Purchase
	 * Status the updates to the view.
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
				productPurchaseTimeInMillis + DataTimeUtil.FIVE_MINUTES_IN_MILLIS;
		// Expiry Date of Subscription
		String productExpiryDateTime = DataTimeUtil.getDateTime(productPurchaseTimeInMillis);
		// Unlimited popcorn purchase was expired if true.
		if (DataTimeUtil.isDateTimePast(productPurchaseTimeInMillis)) {
			skuProductState.set(context.getString(R.string.purchase_expired,
					productExpiryDateTime));
			return;
		}
		// Unlimited popcorn purchase is active. For apple, it can be bought multiple times.
		skuProductState.set(context.getString(R.string.purchased_with_expiry,
				productExpiryDateTime));
	}

	@BindingAdapter("purchaseItemSrcCompat")
	public static void setPurchaseItemSrcCompat(ImageView iv, String skuProductName) {
		iv.setImageResource(
				skuProductName.equals(iv.getContext().getString(R.string.unlimited_popcorn))
						? R.drawable.ic_popcorn
						: R.drawable.ic_apple);
	}

}
