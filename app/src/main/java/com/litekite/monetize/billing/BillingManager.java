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

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.litekite.monetize.R;
import com.litekite.monetize.app.MonetizeApp;
import com.litekite.monetize.base.CallbackProvider;
import com.litekite.monetize.network.NetworkManager;
import com.litekite.monetize.room.database.AppDatabase;
import com.litekite.monetize.room.entity.BillingPurchaseDetails;
import com.litekite.monetize.room.entity.BillingSkuDetails;
import com.litekite.monetize.worker.WorkExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides access to BillingClient {@link #myBillingClient}, handles and performs InApp Purchases.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/google/play/billing/billing_library.html">Google Play
 *     Billing Library Guide</a>
 * @see <a href="https://developer.android.com/training/play-billing-library/index.html">Google Play
 *     Billing Training Guide</a>
 * @see <a href="https://developer.android.com/google/play/billing/billing_testing.html">Testing
 *     InApp and Subscription purchases and Renewal Timing Guide</a>
 * @see <a href="https://github.com/android/play-billing-samples">Google's Play Billing Sample</a>
 * @since 1.0
 */
@Singleton
public class BillingManager
        implements PurchasesUpdatedListener,
                CallbackProvider<BillingCallback>,
                NetworkManager.NetworkStateCallback {

    public static final String TAG = BillingManager.class.getName();
    // Default value of mBillingClientResponseCode until BillingManager was not yet initialized
    private final List<Purchase> myPurchasesResultList = new ArrayList<>();
    // Background work executor
    private final Context context;
    private final AppDatabase appDatabase;
    private final NetworkManager networkManager;
    private final WorkExecutor workExecutor;
    /** A reference to BillingClient */
    private final BillingClient myBillingClient;

    private final List<BillingCallback> billingCallbacks = new ArrayList<>();
    private final Set<String> tokensToBeConsumed = new HashSet<>();

    /**
     * Initializes BillingClient, makes connection and queries sku details, purchase details from
     * Google Play Remote Server, gets purchase details from Google Play Cache.
     *
     * @param context activity or application context.
     * @param workExecutor An executor with fixed thread pool handles background works.
     */
    @Inject
    public BillingManager(
            @NonNull Context context,
            @NonNull AppDatabase appDatabase,
            @NonNull NetworkManager networkManager,
            @NonNull WorkExecutor workExecutor) {
        this.context = context;
        this.appDatabase = appDatabase;
        this.networkManager = networkManager;
        this.workExecutor = workExecutor;
        MonetizeApp.printLog(TAG, "Creating Billing client.");
        myBillingClient =
                BillingClient.newBuilder(context)
                        .enablePendingPurchases()
                        .setListener(this)
                        .build();
        // clears billing manager when the jvm exits or gets terminated.
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
        // starts play billing service connection
        connectToPlayBillingService();
        // Watches network changes and initiates billing service connection
        // if not started before...
        this.networkManager.addCallback(this);
    }

    @Override
    public void onNetworkAvailable() {
        MonetizeApp.printLog(TAG, "onNetworkAvailable: Network Connected");
        connectToPlayBillingService();
    }

    @Override
    public void addCallback(@NonNull BillingCallback cb) {
        if (!billingCallbacks.contains(cb)) {
            billingCallbacks.add(cb);
        }
    }

    @Override
    public void removeCallback(@NonNull BillingCallback cb) {
        billingCallbacks.remove(cb);
    }

    /** Clears the resources */
    private void destroy() {
        MonetizeApp.printLog(TAG, "Destroying the billing manager.");
        if (myBillingClient.isReady()) {
            myBillingClient.endConnection();
        }
        networkManager.removeCallback(this);
    }

    /** Initiates Google Play Billing Service. */
    private void connectToPlayBillingService() {
        MonetizeApp.printLog(TAG, "connectToPlayBillingService");
        if (!myBillingClient.isReady()) {
            startServiceConnection(
                    () -> {
                        // IAB is fully set up. Now, let's get an inventory of stuff we own.
                        MonetizeApp.printLog(TAG, "Setup successful. Querying inventory.");
                        querySkuDetails();
                        queryPurchaseHistoryAsync();
                        queryPurchasesLocally();
                    });
        }
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through a
     * listener
     */
    private void queryPurchasesLocally() {
        executeServiceRequest(
                () -> {
                    myPurchasesResultList.clear();
                    final PurchasesResult purchasesResult =
                            myBillingClient.queryPurchases(SkuType.INAPP);
                    final List<Purchase> purchases = new ArrayList<>();
                    if (purchasesResult.getPurchasesList() != null) {
                        purchases.addAll(purchasesResult.getPurchasesList());
                    }
                    // If there are subscriptions supported, we add subscription rows as well
                    if (areSubscriptionsSupported()) {
                        final PurchasesResult subscriptionResult =
                                myBillingClient.queryPurchases(SkuType.SUBS);
                        final List<Purchase> subscriptionPurchases =
                                subscriptionResult.getPurchasesList();
                        if (subscriptionPurchases != null) {
                            MonetizeApp.printLog(
                                    TAG,
                                    "Subscription purchase result size: "
                                            + subscriptionPurchases.size());
                            purchases.addAll(subscriptionPurchases);
                        } else {
                            MonetizeApp.printLog(TAG, "Subscription purchase result is null:");
                        }
                    }
                    MonetizeApp.printLog(
                            TAG, "Local Query Purchase List Size: " + purchases.size());
                    processPurchases(purchases);
                });
    }

    /**
     * Has runnable implementation of querying InApp and Subscription purchases from Google Play
     * Remote Server.
     */
    private void queryPurchaseHistoryAsync() {
        final List<PurchaseHistoryRecord> purchasesList = new ArrayList<>();
        queryPurchaseHistoryAsync(
                purchasesList,
                SkuType.INAPP,
                () -> {
                    if (areSubscriptionsSupported()) {
                        queryPurchaseHistoryAsync(purchasesList, SkuType.SUBS, null);
                    }
                });
    }

    /**
     * Queries InApp and Subscribed purchase results from Google Play Remote Server.
     *
     * @param purchases this list contains all the product purchases made, has InApp and
     *     Subscription purchased results.
     * @param skuType InApp or Subscription.
     * @param executeWhenFinished Once the InApp product purchase results are given, then
     *     subscription based purchase results are queried and results are placed into the {@link
     *     #myPurchasesResultList}
     */
    private void queryPurchaseHistoryAsync(
            final List<PurchaseHistoryRecord> purchases,
            final @SkuType String skuType,
            final Runnable executeWhenFinished) {
        PurchaseHistoryResponseListener listener =
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingResponseCode.OK && list != null) {
                        purchases.addAll(list);
                        if (executeWhenFinished != null) {
                            executeWhenFinished.run();
                        }
                    } else {
                        MonetizeApp.printLog(
                                TAG,
                                "queryPurchaseHistoryAsync() got an error response code: "
                                        + billingResult.getResponseCode());
                        logErrorType(billingResult);
                    }
                    if (executeWhenFinished == null) {
                        storePurchaseHistoryRecordsLocally(purchases);
                    }
                };
        executeServiceRequest(() -> myBillingClient.queryPurchaseHistoryAsync(skuType, listener));
    }

    /**
     * Stores Purchased Items, consumes consumable items, acknowledges non-consumable items.
     *
     * @param purchases list of Purchase Details returned from the queries.
     */
    private void processPurchases(@NonNull List<Purchase> purchases) {
        if (purchases.size() > 0) {
            MonetizeApp.printLog(TAG, "purchase list size: " + purchases.size());
        }
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                handlePurchase(purchase);
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                MonetizeApp.printLog(
                        TAG, "Received a pending purchase of SKU: " + purchase.getSku());
                // handle pending purchases, e.g. confirm with users about the pending
                // purchases, prompt them to complete it, etc.
                // TODO: 8/24/2020 handle this in the next release.
            }
        }
        storePurchaseResultsLocally(myPurchasesResultList);
        for (Purchase purchase : purchases) {
            if (purchase.getSku().equals(BillingConstants.SKU_BUY_APPLE)) {
                handleConsumablePurchasesAsync(purchase);
            } else {
                acknowledgeNonConsumablePurchasesAsync(purchase);
            }
        }
    }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchaseAsync] inside your app.
     *
     * @param purchase list of Purchase Details returned from the queries.
     */
    private void acknowledgeNonConsumablePurchasesAsync(Purchase purchase) {
        final AcknowledgePurchaseParams params =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        final AcknowledgePurchaseResponseListener listener =
                billingResult -> {
                    if (billingResult.getResponseCode() == BillingResponseCode.OK) {
                        MonetizeApp.printLog(
                                TAG,
                                "onAcknowledgePurchaseResponse: "
                                        + billingResult.getResponseCode());
                    } else {
                        MonetizeApp.printLog(
                                TAG,
                                "onAcknowledgePurchaseResponse: "
                                        + billingResult.getDebugMessage());
                    }
                };
        executeServiceRequest(() -> myBillingClient.acknowledgePurchase(params, listener));
    }

    @Override
    public void onPurchasesUpdated(
            @NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        MonetizeApp.printLog(
                TAG, "onPurchasesUpdate() responseCode: " + billingResult.getResponseCode());
        if (billingResult.getResponseCode() == BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases);
        } else {
            // Handle any other error codes.
            logErrorType(billingResult);
        }
    }

    /**
     * Adds purchase results to the {@link #myPurchasesResultList} after successful purchase.
     *
     * @param purchase the purchase result contains Purchase Details.
     */
    private void handlePurchase(@NonNull Purchase purchase) {
        MonetizeApp.printLog(TAG, "Got a purchase: " + purchase);
        myPurchasesResultList.add(purchase);
    }

    /**
     * Stores Purchase Details on local storage.
     *
     * @param purchases list of Purchase Details returned from the queries.
     */
    private void storePurchaseResultsLocally(List<Purchase> purchases) {
        final List<BillingPurchaseDetails> billingPurchaseDetailsList = new ArrayList<>();
        for (Purchase purchase : purchases) {
            BillingPurchaseDetails billingPurchaseDetails = new BillingPurchaseDetails();
            billingPurchaseDetails.purchaseToken = purchase.getPurchaseToken();
            billingPurchaseDetails.orderID = purchase.getOrderId();
            billingPurchaseDetails.skuID = purchase.getSku();
            billingPurchaseDetails.purchaseTime = purchase.getPurchaseTime();
            billingPurchaseDetailsList.add(billingPurchaseDetails);
        }
        workExecutor.execute(() -> appDatabase.insertPurchaseDetails(billingPurchaseDetailsList));
    }

    /**
     * Stores Purchase Details on local storage.
     *
     * @param purchases list of Purchase Details returned from the queries.
     */
    private void storePurchaseHistoryRecordsLocally(List<PurchaseHistoryRecord> purchases) {
        final List<BillingPurchaseDetails> billingPurchaseDetailsList = new ArrayList<>();
        for (PurchaseHistoryRecord purchase : purchases) {
            BillingPurchaseDetails billingPurchaseDetails = new BillingPurchaseDetails();
            billingPurchaseDetails.purchaseToken = purchase.getPurchaseToken();
            billingPurchaseDetails.skuID = purchase.getSku();
            billingPurchaseDetails.purchaseTime = purchase.getPurchaseTime();
            billingPurchaseDetailsList.add(billingPurchaseDetails);
        }
        workExecutor.execute(() -> appDatabase.insertPurchaseDetails(billingPurchaseDetailsList));
    }

    /**
     * Consumes InApp Product Purchase after successful purchase of InApp Product Purchase. InApp
     * Products cannot be bought after a purchase was made. We need to consume it after a successful
     * purchase, so that we can purchase again and it will become available for the next time we
     * make purchase of the same product that was bought before.
     *
     * @param purchase the purchase result contains Purchase Details.
     */
    private void handleConsumablePurchasesAsync(Purchase purchase) {
        // If we've already scheduled to consume this token - no action is needed (this could happen
        // if you received the token when querying purchases inside onReceive() and later from
        // onActivityResult()
        if (tokensToBeConsumed.contains(purchase.getPurchaseToken())) {
            MonetizeApp.printLog(TAG, "Token was already scheduled to be consumed - skipping...");
            return;
        }
        tokensToBeConsumed.add(purchase.getPurchaseToken());
        // Generating Consume Response listener
        final ConsumeResponseListener listener =
                (billingResult, purchaseToken) -> {
                    // If billing service was disconnected, we try to reconnect 1 time
                    // (feel free to introduce your retry policy here).
                    if (billingResult.getResponseCode() == BillingResponseCode.OK) {
                        MonetizeApp.printLog(
                                TAG, "onConsumeResponse, Purchase Token: " + purchaseToken);
                    } else {
                        MonetizeApp.printLog(
                                TAG, "onConsumeResponse: " + billingResult.getDebugMessage());
                    }
                };
        // Consume the purchase async
        final ConsumeParams consumeParams =
                ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
        // Creating a runnable from the request to use it inside our connection retry policy below
        executeServiceRequest(() -> myBillingClient.consumeAsync(consumeParams, listener));
    }

    /**
     * Logs Billing Client Success, Failure and error responses.
     *
     * @param billingResult to identify the states of Billing Client Responses.
     * @see <a
     *     href="https://developer.android.com/google/play/billing/billing_reference.html">Google
     *     Play InApp Purchase Response Types Guide</a>
     */
    private void logErrorType(BillingResult billingResult) {
        switch (billingResult.getResponseCode()) {
            case BillingResponseCode.DEVELOPER_ERROR:
            case BillingResponseCode.BILLING_UNAVAILABLE:
                MonetizeApp.printLog(
                        TAG,
                        "Billing unavailable. Make sure your Google Play app is setup correctly");
                break;
            case BillingResponseCode.SERVICE_DISCONNECTED:
                notifyBillingError(R.string.err_service_disconnected);
                connectToPlayBillingService();
                break;
            case BillingResponseCode.OK:
                MonetizeApp.printLog(TAG, "Setup successful!");
                break;
            case BillingResponseCode.USER_CANCELED:
                MonetizeApp.printLog(TAG, "User has cancelled Purchase!");
                break;
            case BillingResponseCode.SERVICE_UNAVAILABLE:
                notifyBillingError(R.string.err_no_internet);
                break;
            case BillingResponseCode.ITEM_UNAVAILABLE:
                MonetizeApp.printLog(TAG, "Product is not available for purchase");
                break;
            case BillingResponseCode.ERROR:
                MonetizeApp.printLog(TAG, "fatal error during API action");
                break;
            case BillingResponseCode.ITEM_ALREADY_OWNED:
                MonetizeApp.printLog(TAG, "Failure to purchase since item is already owned");
                queryPurchasesLocally();
                break;
            case BillingResponseCode.ITEM_NOT_OWNED:
                MonetizeApp.printLog(TAG, "Failure to consume since item is not owned");
                break;
            case BillingResponseCode.FEATURE_NOT_SUPPORTED:
                MonetizeApp.printLog(TAG, "Billing feature is not supported on your device");
                break;
            case BillingResponseCode.SERVICE_TIMEOUT:
                MonetizeApp.printLog(TAG, "Billing service timeout occurred");
                break;
            default:
                MonetizeApp.printLog(TAG, "Billing unavailable. Please check your device");
                break;
        }
    }

    /**
     * Notifies billing error message to all the registered clients.
     *
     * @param id A StringResID {@link StringRes}
     */
    private void notifyBillingError(@StringRes int id) {
        MonetizeApp.showToast(context, id);
        billingCallbacks.forEach(cb -> cb.onBillingError(context.getString(id)));
    }

    /**
     * Starts BillingClient Service if not connected already, Or does the tasks written inside the
     * runnable implementation.
     *
     * @param runnable A runnable implementation.
     */
    private void executeServiceRequest(Runnable runnable) {
        if (myBillingClient.isReady()) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
    }

    /**
     * Makes connection with BillingClient.
     *
     * @param executeOnSuccess A runnable implementation.
     */
    private void startServiceConnection(Runnable executeOnSuccess) {
        myBillingClient.startConnection(
                new BillingClientStateListener() {
                    @Override
                    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                        // The billing client is ready. You can query purchases here.
                        MonetizeApp.printLog(TAG, "Setup finished");
                        if (billingResult.getResponseCode() == BillingResponseCode.OK) {
                            if (executeOnSuccess != null) {
                                executeOnSuccess.run();
                            }
                        }
                        logErrorType(billingResult);
                    }

                    @Override
                    public void onBillingServiceDisconnected() {
                        // Try to restart the connection on the next request to
                        // Google Play by calling the startConnection() method.
                    }
                });
    }

    /** Queries for in-app and subscriptions SKU details. */
    private void querySkuDetails() {
        Map<String, SkuDetails> skuResultMap = new HashMap<>();
        List<String> subscriptionSkuList = BillingConstants.getSkuList(SkuType.SUBS);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(subscriptionSkuList).setType(SkuType.SUBS);
        querySkuDetailsAsync(
                skuResultMap,
                params,
                SkuType.SUBS,
                () -> {
                    List<String> inAppSkuList = BillingConstants.getSkuList(SkuType.INAPP);
                    SkuDetailsParams.Builder params1 = SkuDetailsParams.newBuilder();
                    params1.setSkusList(inAppSkuList).setType(SkuType.INAPP);
                    querySkuDetailsAsync(skuResultMap, params1, SkuType.INAPP, null);
                });
    }

    /**
     * Queries SKU Details from Google Play Remote Server of SKU Types (InApp and Subscription).
     *
     * @param skuResultLMap contains SKU ID and Price Details returned by the sku details query.
     * @param params contains list of SKU IDs and SKU Type (InApp or Subscription).
     * @param billingType InApp or Subscription.
     * @param executeWhenFinished contains query for InApp SKU Details that will be run after
     */
    private void querySkuDetailsAsync(
            Map<String, SkuDetails> skuResultLMap,
            SkuDetailsParams.Builder params,
            @SkuType String billingType,
            Runnable executeWhenFinished) {
        final SkuDetailsResponseListener listener =
                (billingResult, skuDetailsList) -> {
                    // Process the result.
                    if (billingResult.getResponseCode() != BillingResponseCode.OK) {
                        MonetizeApp.printLog(
                                TAG,
                                "Unsuccessful query for type: "
                                        + billingType
                                        + ". Error code: "
                                        + billingResult.getResponseCode());
                    } else if (skuDetailsList != null && skuDetailsList.size() > 0) {
                        for (SkuDetails skuDetails : skuDetailsList) {
                            skuResultLMap.put(skuDetails.getSku(), skuDetails);
                        }
                    }
                    if (executeWhenFinished != null) {
                        executeWhenFinished.run();
                        return;
                    }
                    if (skuResultLMap.size() == 0) {
                        MonetizeApp.printLog(
                                TAG, "sku error: " + context.getString(R.string.err_no_sku));
                    } else {
                        MonetizeApp.printLog(TAG, "storing sku list locally");
                        storeSkuDetailsLocally(skuResultLMap);
                    }
                };
        // Creating a runnable from the request to use it inside our connection retry policy below
        executeServiceRequest(() -> myBillingClient.querySkuDetailsAsync(params.build(), listener));
    }

    /**
     * Start a purchase flow.
     *
     * @param activity requires activity class to initiate purchase flow.
     * @param skuDetails The SKU Details registered in the Google Play Developer Console.
     */
    public void initiatePurchaseFlow(@NonNull Activity activity, @NonNull SkuDetails skuDetails) {
        if (skuDetails.getType().equals(SkuType.SUBS) && areSubscriptionsSupported()
                || skuDetails.getType().equals(SkuType.INAPP)) {
            final BillingFlowParams purchaseParams =
                    BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
            executeServiceRequest(
                    () -> {
                        MonetizeApp.printLog(TAG, "Launching in-app purchase flow.");
                        myBillingClient.launchBillingFlow(activity, purchaseParams);
                    });
        }
    }

    /**
     * Checks if subscriptions are supported for current client.
     *
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED. It is only
     * used in unit tests and after queryPurchases execution, which already has a retry-mechanism
     * implemented.
     *
     * @return boolean value of whether the subscription is supported or not.
     */
    private boolean areSubscriptionsSupported() {
        final BillingResult billingResult =
                myBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
        if (billingResult.getResponseCode() != BillingResponseCode.OK) {
            MonetizeApp.printLog(
                    TAG,
                    "areSubscriptionsSupported() got an error response: "
                            + billingResult.getResponseCode());
            notifyBillingError(R.string.err_subscription_not_supported);
        }
        return billingResult.getResponseCode() == BillingResponseCode.OK;
    }

    /**
     * Stores SKU Details on local storage.
     *
     * @param skuDetailsMap Map of SKU Details returned from the queries.
     */
    private void storeSkuDetailsLocally(Map<String, SkuDetails> skuDetailsMap) {
        final List<BillingSkuDetails> billingSkuDetailsList = new ArrayList<>();
        for (String key : skuDetailsMap.keySet()) {
            final SkuDetails skuDetail = skuDetailsMap.get(key);
            if (skuDetail != null) {
                BillingSkuDetails billingSkuDetails = new BillingSkuDetails();
                billingSkuDetails.skuID = skuDetail.getSku();
                billingSkuDetails.skuType =
                        skuDetail.getType().equals(SkuType.SUBS) ? SkuType.SUBS : SkuType.INAPP;
                billingSkuDetails.skuPrice = skuDetail.getPrice();
                billingSkuDetails.originalJson = skuDetail.getOriginalJson();
                billingSkuDetailsList.add(billingSkuDetails);
            }
        }
        workExecutor.execute(() -> appDatabase.insertSkuDetails(billingSkuDetailsList));
    }
}
