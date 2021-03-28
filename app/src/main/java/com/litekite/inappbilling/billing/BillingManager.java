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

package com.litekite.inappbilling.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.billingclient.api.AcknowledgePurchaseParams;
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
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.litekite.inappbilling.R;
import com.litekite.inappbilling.app.InAppBillingApp;
import com.litekite.inappbilling.base.CallbackProvider;
import com.litekite.inappbilling.network.NetworkManager;
import com.litekite.inappbilling.room.database.AppDatabase;
import com.litekite.inappbilling.room.entity.BillingPurchaseDetails;
import com.litekite.inappbilling.room.entity.BillingSkuDetails;
import com.litekite.inappbilling.worker.WorkExecutor;

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
 * @see <a href="https://developer.android.com/google/play/billing/billing_library.html">
 * Google Play Billing Library Guide</a>
 * @see <a href="https://developer.android.com/training/play-billing-library/index.html">Google
 * Play Billing Training Guide</a>
 * @see <a href="https://developer.android.com/google/play/billing/billing_testing.html">Testing
 * InApp and Subscription purchases and Renewal Timing Guide</a>
 * @see <a href="https://github.com/android/play-billing-samples">Google's Play Billing Sample</a>
 * @since 1.0
 */
@Singleton
public class BillingManager implements
		PurchasesUpdatedListener,
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
	/**
	 * A reference to BillingClient
	 **/
	private final BillingClient myBillingClient;
	private final List<BillingCallback> billingCallbacks = new ArrayList<>();
	private Set<String> tokensToBeConsumed;

	/**
	 * Initializes BillingClient, makes connection and queries sku details, purchase details from
	 * Google Play Remote Server, gets purchase details from Google Play Cache.
	 *
	 * @param context      activity or application context.
	 * @param workExecutor An executor with fixed thread pool handles background works.
	 */
	@Inject
	public BillingManager(@NonNull Context context,
	                      @NonNull AppDatabase appDatabase,
	                      @NonNull NetworkManager networkManager,
	                      @NonNull WorkExecutor workExecutor) {
		this.context = context;
		this.appDatabase = appDatabase;
		this.networkManager = networkManager;
		this.workExecutor = workExecutor;
		InAppBillingApp.printLog(TAG, "Creating Billing client.");
		myBillingClient = BillingClient.newBuilder(context)
				.enablePendingPurchases()
				.setListener(this)
				.build();
	}

	@Override
	public void onNetworkAvailable() {
		InAppBillingApp.printLog(TAG, "onNetworkAvailable: Network Connected");
		connectToPlayBillingService();
	}

	@Override
	public void addCallback(@NonNull BillingCallback cb) {
		if (!billingCallbacks.contains(cb)) {
			billingCallbacks.add(cb);
			connectToPlayBillingService();
		}
	}

	@Override
	public void removeCallback(@NonNull BillingCallback cb) {
		if (billingCallbacks.remove(cb) && billingCallbacks.size() == 0) {
			destroy();
		}
	}

	/**
	 * Clears the resources
	 */
	private void destroy() {
		InAppBillingApp.printLog(TAG, "Destroying the billing manager.");
		if (myBillingClient.isReady()) {
			myBillingClient.endConnection();
		}
		this.networkManager.removeCallback(this);
		// Destroys app local database
		appDatabase.destroyAppDatabase();
	}

	/**
	 * Initiates Google Play Billing Service.
	 */
	private void connectToPlayBillingService() {
		InAppBillingApp.printLog(TAG, "connectToPlayBillingService");
		if (!myBillingClient.isReady() && billingCallbacks.size() > 0) {
			startServiceConnection(() -> {
				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				InAppBillingApp.printLog(TAG, "Setup successful. Querying inventory.");
				querySkuDetails();
				queryPurchasesAsync();
			});
			// Watches network changes and initiates billing service connection
			// if not started before...
			this.networkManager.addCallback(this);
		}
	}

	/**
	 * Query purchases across various use cases and deliver the result in a formalized way through
	 * a listener
	 */
	private void queryPurchasesAsync() {
		Runnable queryToExecute = () -> {
			myPurchasesResultList.clear();
			PurchasesResult purchasesResult = myBillingClient.queryPurchases(SkuType.INAPP);
			List<Purchase> purchases = new ArrayList<>();
			if (purchasesResult.getPurchasesList() != null) {
				purchases.addAll(purchasesResult.getPurchasesList());
			}
			// If there are subscriptions supported, we add subscription rows as well
			if (areSubscriptionsSupported()) {
				PurchasesResult subscriptionResult
						= myBillingClient.queryPurchases(SkuType.SUBS);
				List<Purchase> subscriptionPurchases = subscriptionResult.getPurchasesList();
				if (subscriptionPurchases != null) {
					InAppBillingApp.printLog(TAG, "Subscription purchase result size: "
							+ subscriptionPurchases.size());
					purchases.addAll(subscriptionPurchases);
				} else {
					InAppBillingApp.printLog(TAG, "Subscription purchase result is null:");
				}
			}
			InAppBillingApp.printLog(TAG, "Local Query Purchase List Size: "
					+ purchases.size());
			processPurchases(purchases);
		};
		executeServiceRequest(queryToExecute);
	}

	/**
	 * Stores Purchased Items, consumes consumable items, acknowledges non-consumable items.
	 *
	 * @param purchases list of Purchase Details returned from the queries.
	 */
	private void processPurchases(@NonNull List<Purchase> purchases) {
		if (purchases.size() > 0) {
			InAppBillingApp.printLog(TAG, "purchase list size: " + purchases.size());
		}
		for (Purchase purchase : purchases) {
			if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
				handlePurchase(purchase);
			} else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
				InAppBillingApp.printLog(TAG, "Received a pending purchase of SKU: "
						+ purchase.getSku());
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
	private void acknowledgeNonConsumablePurchasesAsync(final Purchase purchase) {
		Runnable acknowledgePurchaseRunnable = () -> {
			AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
					.setPurchaseToken(purchase.getPurchaseToken())
					.build();
			myBillingClient.acknowledgePurchase(params, billingResult -> {
				if (billingResult.getResponseCode() == BillingResponseCode.OK) {
					InAppBillingApp.printLog(TAG,
							"onAcknowledgePurchaseResponse: "
									+ billingResult.getResponseCode());
				} else {
					InAppBillingApp.printLog(TAG,
							"onAcknowledgePurchaseResponse: "
									+ billingResult.getDebugMessage());
				}
			});
		};
		executeServiceRequest(acknowledgePurchaseRunnable);
	}

	@Override
	public void onPurchasesUpdated(@NonNull BillingResult billingResult,
	                               @Nullable List<Purchase> purchases) {
		InAppBillingApp.printLog(TAG, "onPurchasesUpdate() responseCode: "
				+ billingResult.getResponseCode());
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
		InAppBillingApp.printLog(TAG, "Got a purchase: " + purchase);
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
	 * Consumes InApp Product Purchase after successful purchase of InApp Product Purchase. InApp
	 * Products cannot be bought after a purchase was made. We need to consume it after a
	 * successful purchase, so that we can purchase again and it will become available for the
	 * next time we make purchase of the same product that was bought before.
	 *
	 * @param purchase the purchase result contains Purchase Details.
	 */
	private void handleConsumablePurchasesAsync(final Purchase purchase) {
		// If we've already scheduled to consume this token - no action is needed (this could happen
		// if you received the token when querying purchases inside onReceive() and later from
		// onActivityResult()
		if (tokensToBeConsumed == null) {
			tokensToBeConsumed = new HashSet<>();
		} else if (tokensToBeConsumed.contains(purchase.getPurchaseToken())) {
			InAppBillingApp.printLog(TAG,
					"Token was already scheduled to be consumed - skipping...");
			return;
		}
		tokensToBeConsumed.add(purchase.getPurchaseToken());
		// Generating Consume Response listener
		final ConsumeResponseListener onConsumeListener = (billingResult, purchaseToken) -> {
			// If billing service was disconnected, we try to reconnect 1 time
			// (feel free to introduce your retry policy here).
			if (billingResult.getResponseCode() == BillingResponseCode.OK) {
				InAppBillingApp.printLog(TAG,
						"onConsumeResponse, Purchase Token: " + purchaseToken);
			} else {
				InAppBillingApp.printLog(TAG, "onConsumeResponse: "
						+ billingResult.getDebugMessage());
			}
		};
		// Creating a runnable from the request to use it inside our connection retry policy below
		Runnable consumeRequest = () -> {
			// Consume the purchase async
			ConsumeParams consumeParams = ConsumeParams.newBuilder()
					.setPurchaseToken(purchase.getPurchaseToken())
					.build();
			myBillingClient.consumeAsync(consumeParams, onConsumeListener);
		};
		executeServiceRequest(consumeRequest);
	}

	/**
	 * Logs Billing Client Success, Failure and error responses.
	 *
	 * @param billingResult to identify the states of Billing Client Responses.
	 *
	 * @see <a href="https://developer.android.com/google/play/billing/billing_reference.html">
	 * Google Play InApp Purchase Response Types Guide</a>
	 */
	private void logErrorType(BillingResult billingResult) {
		switch (billingResult.getResponseCode()) {
			case BillingResponseCode.DEVELOPER_ERROR:
			case BillingResponseCode.BILLING_UNAVAILABLE:
				InAppBillingApp.printLog(TAG, "Billing unavailable. "
						+ "Make sure your Google Play app is setup correctly");
				break;
			case BillingResponseCode.SERVICE_DISCONNECTED:
				notifyBillingError(R.string.err_service_disconnected);
				connectToPlayBillingService();
				break;
			case BillingResponseCode.OK:
				InAppBillingApp.printLog(TAG, "Setup successful!");
				break;
			case BillingResponseCode.USER_CANCELED:
				InAppBillingApp.printLog(TAG, "User has cancelled Purchase!");
				break;
			case BillingResponseCode.SERVICE_UNAVAILABLE:
				notifyBillingError(R.string.err_no_internet);
				break;
			case BillingResponseCode.ITEM_UNAVAILABLE:
				InAppBillingApp.printLog(TAG, "Product is not available for purchase");
				break;
			case BillingResponseCode.ERROR:
				InAppBillingApp.printLog(TAG, "fatal error during API action");
				break;
			case BillingResponseCode.ITEM_ALREADY_OWNED:
				InAppBillingApp.printLog(TAG,
						"Failure to purchase since item is already owned");
				queryPurchasesAsync();
				break;
			case BillingResponseCode.ITEM_NOT_OWNED:
				InAppBillingApp.printLog(TAG, "Failure to consume since item is not owned");
				break;
			case BillingResponseCode.FEATURE_NOT_SUPPORTED:
				InAppBillingApp.printLog(TAG, "Billing feature is not supported on your device");
				break;
			case BillingResponseCode.SERVICE_TIMEOUT:
				InAppBillingApp.printLog(TAG, "Billing service timeout occurred");
				break;
			default:
				InAppBillingApp.printLog(TAG, "Billing unavailable. Please check your device");
				break;
		}
	}

	/**
	 * Notifies billing error message to all the registered clients.
	 *
	 * @param id A StringResID {@link StringRes}
	 */
	private void notifyBillingError(@StringRes int id) {
		InAppBillingApp.showToast(context, id);
		billingCallbacks.forEach(cb -> cb.onBillingError(context.getString(id)));
	}

	/**
	 * Starts BillingClient Service if not connected already, Or does the tasks written inside
	 * the runnable implementation.
	 *
	 * @param runnable A runnable implementation.
	 */
	private void executeServiceRequest(Runnable runnable) {
		if (myBillingClient.isReady()) {
			runnable.run();
		} else if (billingCallbacks.size() > 0) {
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
	private void startServiceConnection(final Runnable executeOnSuccess) {
		myBillingClient.startConnection(new BillingClientStateListener() {
			@Override
			public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
				// The billing client is ready. You can query purchases here.
				InAppBillingApp.printLog(TAG, "Setup finished");
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

	/**
	 * Queries for in-app and subscriptions SKU details.
	 */
	private void querySkuDetails() {
		final Map<String, SkuDetails> skuResultMap = new HashMap<>();
		List<String> subscriptionSkuList = BillingConstants.getSkuList(SkuType.SUBS);
		SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
		params.setSkusList(subscriptionSkuList).setType(SkuType.SUBS);
		querySkuDetailsAsync(skuResultMap, params, SkuType.SUBS, () -> {
			List<String> inAppSkuList = BillingConstants.getSkuList(SkuType.INAPP);
			SkuDetailsParams.Builder params1 = SkuDetailsParams.newBuilder();
			params1.setSkusList(inAppSkuList).setType(SkuType.INAPP);
			querySkuDetailsAsync(skuResultMap, params1, SkuType.INAPP, null);
		});
	}

	/**
	 * Queries SKU Details from Google Play Remote Server of SKU Types (InApp and Subscription).
	 *
	 * @param skuResultLMap       contains SKU ID and Price Details returned by the sku details
	 *                            query.
	 * @param params              contains list of SKU IDs and SKU Type (InApp or Subscription).
	 * @param billingType         InApp or Subscription.
	 * @param executeWhenFinished contains query for InApp SKU Details that will be run after
	 */
	private void querySkuDetailsAsync(final Map<String, SkuDetails> skuResultLMap,
	                                  final SkuDetailsParams.Builder params,
	                                  final @SkuType String billingType,
	                                  final Runnable executeWhenFinished) {
		// Creating a runnable from the request to use it inside our connection retry policy below
		final Runnable queryRequest = () -> myBillingClient.querySkuDetailsAsync(params.build(),
				(billingResult, skuDetailsList) -> {
					// Process the result.
					if (billingResult.getResponseCode() != BillingResponseCode.OK) {
						InAppBillingApp.printLog(TAG, "Unsuccessful query for type: "
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
						InAppBillingApp.printLog(TAG, "sku error: "
								+ context.getString(R.string.err_no_sku));
					} else {
						InAppBillingApp.printLog(TAG, "storing sku list locally");
						storeSkuDetailsLocally(skuResultLMap);
					}
				});
		executeServiceRequest(queryRequest);
	}

	/**
	 * Start a purchase flow.
	 *
	 * @param activity   requires activity class to initiate purchase flow.
	 * @param skuDetails The SKU Details registered in the Google Play Developer Console.
	 */
	public void initiatePurchaseFlow(@NonNull final Activity activity,
	                                 @NonNull final SkuDetails skuDetails) {
		if (skuDetails.getType().equals(SkuType.SUBS) && areSubscriptionsSupported()
				|| skuDetails.getType().equals(SkuType.INAPP)) {
			Runnable purchaseFlowRequest = () -> {
				InAppBillingApp.printLog(TAG, "Launching in-app purchase flow.");
				BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
						.setSkuDetails(skuDetails)
						.build();
				myBillingClient.launchBillingFlow(activity, purchaseParams);
			};
			executeServiceRequest(purchaseFlowRequest);
		}
	}

	/**
	 * Checks if subscriptions are supported for current client.
	 * <p>
	 * Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
	 * It is only used in unit tests and after queryPurchases execution, which already has
	 * a retry-mechanism implemented.
	 * </p>
	 *
	 * @return boolean value of whether the subscription is supported or not.
	 */
	private boolean areSubscriptionsSupported() {
		BillingResult billingResult = myBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
		if (billingResult.getResponseCode() != BillingResponseCode.OK) {
			InAppBillingApp.printLog(TAG, "areSubscriptionsSupported() got an error response: "
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
			SkuDetails skuDetail = skuDetailsMap.get(key);
			if (skuDetail != null) {
				BillingSkuDetails billingSkuDetails = new BillingSkuDetails();
				billingSkuDetails.skuID = skuDetail.getSku();
				billingSkuDetails.skuType = skuDetail.getType().equals(SkuType.SUBS)
						? SkuType.SUBS
						: SkuType.INAPP;
				billingSkuDetails.skuPrice = skuDetail.getPrice();
				billingSkuDetails.originalJson = skuDetail.getOriginalJson();
				billingSkuDetailsList.add(billingSkuDetails);
			}
		}
		workExecutor.execute(() -> appDatabase.insertSkuDetails(billingSkuDetailsList));
	}

}