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

package com.litekite.inappbilling.billing;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.litekite.inappbilling.room.database.AppDatabase;
import com.litekite.inappbilling.room.entity.BillingPurchaseDetails;
import com.litekite.inappbilling.room.entity.BillingSkuDetails;
import com.litekite.inappbilling.view.activity.BaseActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides access to BillingClient {@link #myBillingClient}, handles and performs InApp Purchases.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/google/play/billing/billing_library.html">
 * Google Play Billing Library Guide</a>
 * @see <a href="https://developer.android.com/training/play-billing-library/index.html"> Google
 * Play Billing Training Guide</a>
 * @see <a href="https://developer.android.com/google/play/billing/billing_testing.html"> Testing
 * InApp and Subscription purchases and Renewal Timing Guide</a>
 * @see <a href="https://github.com/googlesamples/android-play-billing/tree/master/TrivialDrive_v2">
 * Google's InAppBilling Sample Project</a>
 * @since 1.0
 */
public class BillingManager implements PurchasesUpdatedListener {

	private static final String TAG = BillingManager.class.getName();
	// Default value of mBillingClientResponseCode until BillingManager was not yet initialized
	private final List<Purchase> myPurchasesResultList = new ArrayList<>();
	/**
	 * A reference to BillingClient
	 **/
	private BillingClient myBillingClient;
	/**
	 * True if billing service is connected now.
	 */
	private boolean isServiceConnected;
	private Context context;
	private Set<String> tokensToBeConsumed;
	private BillingUpdatesListener billingUpdatesListener;

	/**
	 * Initializes BillingClient, makes connection and queries sku details, purchase details from
	 * Google Play Remote Server, gets purchase details from Google Play Cache.
	 *
	 * @param context                activity or application context.
	 * @param billingUpdatesListener listener that updates implemented classes about the billing
	 *                               errors.
	 */
	public BillingManager(@NonNull Context context,
	                      @NonNull BillingUpdatesListener billingUpdatesListener) {
		this.context = context;
		this.billingUpdatesListener = billingUpdatesListener;
		BaseActivity.printLog(TAG, "Creating Billing client.");
		myBillingClient = BillingClient.newBuilder(context)
				.enablePendingPurchases()
				.setListener(this)
				.build();
		connectToPlayBillingService();
	}

	/**
	 * Initiates Google Play Billing Service.
	 */
	private void connectToPlayBillingService() {
		BaseActivity.printLog(TAG, "connectToPlayBillingService");
		if (!myBillingClient.isReady()) {
			startServiceConnection(() -> {
				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				BaseActivity.printLog(TAG, "Setup successful. Querying inventory.");
				querySkuDetails();
				queryPurchasesAsync();
			});
		}
	}

	/**
	 * Query purchases across various use cases and deliver the result in a formalized way through
	 * a listener
	 */
	private void queryPurchasesAsync() {
		Runnable queryToExecute = () -> {
			myPurchasesResultList.clear();
			PurchasesResult purchasesResult =
					myBillingClient.queryPurchases(SkuType.INAPP);
			// If there are subscriptions supported, we add subscription rows as well
			if (areSubscriptionsSupported()) {
				PurchasesResult subscriptionResult
						= myBillingClient.queryPurchases(SkuType.SUBS);
				BaseActivity.printLog(TAG, "Querying subscriptions result code: "
						+ subscriptionResult.getResponseCode()
						+ " res: " + subscriptionResult.getPurchasesList().size());
				if (subscriptionResult.getResponseCode() == BillingResponseCode.OK) {
					purchasesResult.getPurchasesList().addAll(
							subscriptionResult.getPurchasesList());
				} else {
					BaseActivity.printLog(TAG, "Got an error response "
							+ "trying to query subscription purchases");
				}
			} else if (purchasesResult.getResponseCode() == BillingResponseCode.OK) {
				BaseActivity.printLog(TAG, "Skipped subscription purchases query "
						+ "since they are not supported");
			} else {
				BaseActivity.printLog(TAG, "queryPurchases() got an error response code: "
						+ purchasesResult.getResponseCode());
			}
			BaseActivity.printLog(TAG, "Local Query Purchase List Size: "
					+ purchasesResult.getPurchasesList().size());
			processPurchases(purchasesResult.getPurchasesList());
		};
		executeServiceRequest(queryToExecute);
	}

	/**
	 * Stores Purchased Items, consumes consumable items, acknowledges non-consumable items.
	 *
	 * @param purchases list of Purchase Details returned from the queries.
	 */
	private void processPurchases(List<Purchase> purchases) {
		if (purchases.size() > 0) {
			BaseActivity.printLog(TAG, "purchase list size: " + purchases.size());
		}
		for (Purchase purchase : purchases) {
			if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
				handlePurchase(purchase);
			} else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
				BaseActivity.printLog(TAG, "Received a pending purchase of SKU: "
						+ purchase.getSku());
				// handle pending purchases, e.g. confirm with users about the pending
				// purchases, prompt them to complete it, etc.
				// TODO handle this in the next release.
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
					BaseActivity.printLog(TAG,
							"onAcknowledgePurchaseResponse: "
									+ billingResult.getResponseCode());
				} else {
					BaseActivity.printLog(TAG,
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
		BaseActivity.printLog(TAG, "onPurchasesUpdate() responseCode: "
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
	private void handlePurchase(Purchase purchase) {
		BaseActivity.printLog(TAG, "Got a purchase: " + purchase);
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
		new Thread(() -> AppDatabase.getAppDatabase(context)
				.getBillingDao().insertPurchaseDetails(billingPurchaseDetailsList)).start();
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
			BaseActivity.printLog(TAG,
					"Token was already scheduled to be consumed - skipping...");
			return;
		}
		tokensToBeConsumed.add(purchase.getPurchaseToken());
		// Generating Consume Response listener
		final ConsumeResponseListener onConsumeListener = (billingResult, purchaseToken) -> {
			// If billing service was disconnected, we try to reconnect 1 time
			// (feel free to introduce your retry policy here).
			if (billingResult.getResponseCode() == BillingResponseCode.OK) {
				BaseActivity.printLog(TAG,
						"onConsumeResponse, Purchase Token: " + purchaseToken);
			} else {
				BaseActivity.printLog(TAG, "onConsumeResponse: "
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
				BaseActivity.printLog(TAG, "Billing unavailable. "
						+ "Make sure your Google Play app is setup correctly");
				break;
			case BillingResponseCode.SERVICE_DISCONNECTED:
				billingUpdatesListener
						.onBillingError(context.getString(R.string.err_service_disconnected));
				connectToPlayBillingService();
				break;
			case BillingResponseCode.OK:
				BaseActivity.printLog(TAG, "Setup successful!");
				break;
			case BillingResponseCode.USER_CANCELED:
				BaseActivity.printLog(TAG, "User has cancelled Purchase!");
				break;
			case BillingResponseCode.SERVICE_UNAVAILABLE:
				billingUpdatesListener
						.onBillingError(context.getString(R.string.err_no_internet));
				break;
			case BillingResponseCode.ITEM_UNAVAILABLE:
				BaseActivity.printLog(TAG, "Product is not available for purchase");
				break;
			case BillingResponseCode.ERROR:
				BaseActivity.printLog(TAG, "fatal error during API action");
				break;
			case BillingResponseCode.ITEM_ALREADY_OWNED:
				BaseActivity.printLog(TAG,
						"Failure to purchase since item is already owned");
				queryPurchasesAsync();
				break;
			case BillingResponseCode.ITEM_NOT_OWNED:
				BaseActivity.printLog(TAG, "Failure to consume since item is not owned");
				break;
			case BillingResponseCode.FEATURE_NOT_SUPPORTED:
				BaseActivity.printLog(TAG, "Billing feature is not supported on your device");
				break;
			case BillingResponseCode.SERVICE_TIMEOUT:
				BaseActivity.printLog(TAG, "Billing service timeout occurred");
				break;
			default:
				BaseActivity.printLog(TAG, "Billing unavailable. Please check your device");
				break;
		}
	}

	/**
	 * Starts BillingClient Service if not connected already, Or does the tasks written inside
	 * the runnable implementation.
	 *
	 * @param runnable A runnable implementation.
	 */
	private void executeServiceRequest(Runnable runnable) {
		if (isServiceConnected) {
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
	private void startServiceConnection(final Runnable executeOnSuccess) {
		myBillingClient.startConnection(new BillingClientStateListener() {
			@Override
			public void onBillingSetupFinished(BillingResult billingResult) {
				// The billing client is ready. You can query purchases here.
				BaseActivity.printLog(TAG, "Setup finished");
				if (billingResult.getResponseCode() == BillingResponseCode.OK) {
					isServiceConnected = true;
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
				isServiceConnected = false;
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
						BaseActivity.printLog(TAG, "Unsuccessful query for type: "
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
						BaseActivity.printLog(TAG, "sku error: "
								+ context.getString(R.string.err_no_sku));
					} else {
						BaseActivity.printLog(TAG, "storing sku list locally");
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
		if (areSubscriptionsSupported()) {
			Runnable purchaseFlowRequest = () -> {
				BaseActivity.printLog(TAG, "Launching in-app purchase flow.");
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
		if (myBillingClient == null) {
			BaseActivity.printLog(TAG, "Billing client was null and quitting");
			return false;
		}
		BillingResult billingResult = myBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
		if (billingResult.getResponseCode() != BillingResponseCode.OK) {
			BaseActivity.printLog(TAG, "areSubscriptionsSupported() got an error response: "
					+ billingResult.getResponseCode());
			billingUpdatesListener
					.onBillingError(context.getString(R.string.err_subscription_not_supported));
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
		new Thread(() -> AppDatabase.getAppDatabase(context)
				.getBillingDao().insertSkuDetails(billingSkuDetailsList)).start();
	}

	/**
	 * Clears the resources
	 */
	public void destroy() {
		BaseActivity.printLog(TAG, "Destroying the manager.");
		if (myBillingClient != null && myBillingClient.isReady()) {
			myBillingClient.endConnection();
			myBillingClient = null;
		}
	}

}