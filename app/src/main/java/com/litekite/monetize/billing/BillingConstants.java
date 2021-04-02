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
package com.litekite.monetize.billing;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.SkuType;
import java.util.Arrays;
import java.util.List;

/**
 * This class has static fields and methods useful for Google's Play Billing.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @since 1.0
 */
public final class BillingConstants {

    // SKUs for our managed products
    // One time purchase (valid for lifetime)
    public static final String SKU_UNLOCK_APP_FEATURES = "app_premium_feature";
    // can be purchased many times by consuming
    public static final String SKU_BUY_APPLE = "one_apple";
    // SKU for our subscription
    private static final String SKU_POPCORN_UNLIMITED_MONTHLY = "unlimited_popcorn_monthly";
    private static final String[] IN_APP_SKU = {SKU_UNLOCK_APP_FEATURES, SKU_BUY_APPLE};
    private static final String[] SUBSCRIPTIONS_SKU = {SKU_POPCORN_UNLIMITED_MONTHLY};

    private BillingConstants() {}

    /**
     * Gives a list of SKUs based on the type of billing, InApp or Subscription.
     *
     * @param billingType the billing type, InApp or Subscription.
     * @return the list of all SKUs for the billing type specified.
     */
    static List<String> getSkuList(@SkuType String billingType) {
        return (billingType.equals(BillingClient.SkuType.INAPP))
                ? Arrays.asList(IN_APP_SKU)
                : Arrays.asList(SUBSCRIPTIONS_SKU);
    }
}
