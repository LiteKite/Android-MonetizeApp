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

package com.litekite.inappbilling.billing;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClient.FeatureType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchasesResult;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.litekite.inappbilling.R;
import com.litekite.inappbilling.room.database.AppDatabase;
import com.litekite.inappbilling.room.entity.BillingPurchaseDetails;
import com.litekite.inappbilling.room.entity.BillingSkuDetails;
import com.litekite.inappbilling.view.activity.BaseActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides access to BillingClient {@link #myBillingClient}, handles and performs InApp Purchases.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/google/play/billing/billing_library.html#connecting">
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
	 * @param billingUpdatesListener listener that updates implemented classes about the billing
	 *                               errors.
	 * @param context                activity or application context.
	 */
	public BillingManager(BillingUpdatesListener billingUpdatesListener, Context context) {
		this.context = context;
		this.billingUpdatesListener = billingUpdatesListener;
		BaseActivity.printLog(TAG, "Creating Billing client.");
		myBillingClient = BillingClient.newBuilder(context).setListener(this).build();
		BaseActivity.printLog(TAG, "Starting setup.");
		startServiceConnection(new Runnable() {
			@Override
			public void run() {
				// IAB is fully set up. Now, let's get an inventory of stuff we own.
				BaseActivity.printLog(TAG, "Setup successful. Querying inventory.");
				queryPurchasesLocally();
				queryPurchases();
				querySkuDetails();
			}
		});
	}

	/**
	 * Query purchases across various use cases and deliver the result in a formalized way through
	 * a listener
	 */
	private void queryPurchasesLocally() {
		Runnable queryToExecute = new Runnable() {
			@Override
			public void run() {
				PurchasesResult purchasesResult =
						myBillingClient.queryPurchases(SkuType.INAPP);
				// If there are subscriptions supported, we add subscription rows as well
				if (areSubscriptionsSupported()) {
					PurchasesResult subscriptionResult
							= myBillingClient.queryPurchases(SkuType.SUBS);
					BaseActivity.printLog(TAG, "Querying subscriptions result code: "
							+ subscriptionResult.getResponseCode()
							+ " res: " + subscriptionResult.getPurchasesList().size());
					if (subscriptionResult.getResponseCode() == BillingResponse.OK) {
						purchasesResult.getPurchasesList().addAll(
								subscriptionResult.getPurchasesList());
					} else {
						BaseActivity.printLog(TAG, "Got an error response "
								+ "trying to query subscription purchases");
					}
				} else if (purchasesResult.getResponseCode() == BillingResponse.OK) {
					BaseActivity.printLog(TAG, "Skipped subscription purchases query "
							+ "since they are not supported");
				} else {
					BaseActivity.printLog(TAG, "queryPurchases() got an error response code: "
							+ purchasesResult.getResponseCode());
				}
				BaseActivity.printLog(TAG, "Local Query Purchase List Size: "
						+ purchasesResult.getPurchasesList().size());
				storePurchaseResultsLocally(purchasesResult.getPurchasesList());
			}
		};
		executeServiceRequest(queryToExecute);
	}

	/**
	 * Has runnable implementation of querying InApp and Subscription purchases from Google Play
	 * Remote Server.
	 */
	private void queryPurchases() {
		final List<Purchase> purchasesResultList = new ArrayList<>();
		queryPurchaseHistoryAsync(purchasesResultList, SkuType.INAPP, new Runnable() {
			@Override
			public void run() {
				if (areSubscriptionsSupported()) {
					queryPurchaseHistoryAsync(purchasesResultList,
							SkuType.SUBS, null);
				}
			}
		});
	}

	/**
	 * Queries InApp and Subscribed purchase results from Google Play Remote Server.
	 *
	 * @param purchasesResultList this list contains all the product purchases made, has InApp and
	 *                            Subscription purchased results.
	 * @param skuType             InApp or Subscription.
	 * @param executeWhenFinished Once the InApp product purchase results are given, then
	 *                            subscription based purchase results are queried and results are
	 *                            placed into the {@link #myPurchasesResultList}
	 */
	private void queryPurchaseHistoryAsync(final List<Purchase> purchasesResultList,
	                                       final @SkuType String skuType,
	                                       final Runnable executeWhenFinished) {
		Runnable queryPurchases = new Runnable() {
			@Override
			public void run() {
				myBillingClient.queryPurchaseHistoryAsync(skuType,
						new PurchaseHistoryResponseListener() {
							@Override
							public void onPurchaseHistoryResponse(@BillingResponse int responseCode,
							                                      List<Purchase> purchasesList) {
								if (responseCode == BillingResponse.OK && purchasesList != null) {
									purchasesResultList.addAll(purchasesList);
									if (executeWhenFinished != null) {
										executeWhenFinished.run();
									}
								} else {
									BaseActivity.printLog(TAG,
											"queryPurchases() got an error response code: "
													+ responseCode);
									logErrorType(responseCode);
								}
								if (executeWhenFinished == null) {
									onQueryPurchasesFinished(purchasesResultList, responseCode);
								}
							}
						});
			}
		};
		executeServiceRequest(queryPurchases);
	}

	/**
	 * Handle a result from querying of purchases and report an updated list to the listener.
	 *
	 * @param result       Purchase result returned from the query.
	 * @param responseCode BillingClient response code about the success or failure result.
	 */
	private void onQueryPurchasesFinished(List<Purchase> result,
	                                      @BillingResponse int responseCode) {
		// Have we been disposed of in the meantime? If so, or bad result code, then quit
		if (myBillingClient == null) {
			BaseActivity.printLog(TAG, "Billing client was null or result code ("
					+ responseCode + ") was bad - quitting");
			return;
		}
		BaseActivity.printLog(TAG, "Query inventory was successful.");
		// Update the UI and purchases inventory with new list of purchases
		myPurchasesResultList.clear();
		onPurchasesUpdated(BillingResponse.OK, result);
	}

	/**
	 * Queries for in-app and subscriptions SKU details.
	 */
	private void querySkuDetails() {
		final List<SkuDetails> skuResultList = new ArrayList<>();
		List<String> subscriptionSkuList = BillingConstants.getSkuList(SkuType.SUBS);
		SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
		params.setSkusList(subscriptionSkuList).setType(SkuType.SUBS);
		querySkuDetailsAsync(skuResultList, params, SkuType.SUBS, new Runnable() {
			@Override
			public void run() {
				List<String> inAppSkuList = BillingConstants.getSkuList(SkuType.INAPP);
				SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
				params.setSkusList(inAppSkuList).setType(SkuType.INAPP);
				querySkuDetailsAsync(skuResultList, params, SkuType.INAPP, null);
			}
		});
	}

	/**
	 * Queries SKU Details from Google Play Remote Server of SKU Types (InApp and Subscription).
	 *
	 * @param skuResultList       contains SKU ID and Price Details returned by the sku details
	 *                            query.
	 * @param params              contains list of SKU IDs and SKU Type (InApp or Subscription).
	 * @param billingType         InApp or Subscription.
	 * @param executeWhenFinished contains query for InApp SKU Details that will be run after
	 *                            getting results of Subscription based SKU Details.
	 */
	private void querySkuDetailsAsync(final List<SkuDetails> skuResultList,
	                                  final SkuDetailsParams.Builder params,
	                                  final @SkuType String billingType,
	                                  final Runnable executeWhenFinished) {
		// Creating a runnable from the request to use it inside our connection retry policy below
		final Runnable queryRequest = new Runnable() {
			@Override
			public void run() {
				myBillingClient.querySkuDetailsAsync(params.build(),
						new SkuDetailsResponseListener() {
							@Override
							public void onSkuDetailsResponse(int responseCode,
							                                 List<SkuDetails> skuDetailsList) {
								// Process the result.
								if (responseCode != BillingResponse.OK) {
									BaseActivity.printLog(TAG,
											"Unsuccessful query for type: " + billingType
													+ ". Error code: " + responseCode);
								} else if (skuDetailsList != null && skuDetailsList.size() > 0) {
									skuResultList.addAll(skuDetailsList);
								}
								if (executeWhenFinished != null) {
									executeWhenFinished.run();
									return;
								}
								if (skuResultList.size() == 0) {
									BaseActivity.printLog(TAG, "sku error: "
											+ context.getString(R.string.err_no_sku));
								} else {
									BaseActivity.printLog(TAG, "storing sku list locally");
									storeSkuDetailsLocally(skuResultList);
								}
							}
						});
			}
		};
		executeServiceRequest(queryRequest);
	}

	/**
	 * Start a purchase flow.
	 *
	 * @param activity    requires activity class to initiate purchase flow.
	 * @param skuId       The SKU ID registered in the Google Play Developer Console.
	 * @param billingType InApp or Subscription based Product.
	 */
	public void initiatePurchaseFlow(final Activity activity,
	                                 final String skuId,
	                                 final @SkuType String billingType) {
		if (areSubscriptionsSupported()) {
			Runnable purchaseFlowRequest = new Runnable() {
				@Override
				public void run() {
					BaseActivity.printLog(TAG, "Launching in-app purchase flow.");
					BillingFlowParams purchaseParams =
							BillingFlowParams.newBuilder()
									.setSku(skuId)
									.setType(billingType)
									.build();
					myBillingClient.launchBillingFlow(activity, purchaseParams);
				}
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
		int responseCode = myBillingClient.isFeatureSupported(FeatureType.SUBSCRIPTIONS);
		if (responseCode != BillingResponse.OK) {
			BaseActivity.printLog(TAG,
					"areSubscriptionsSupported() got an error response: " + responseCode);
			billingUpdatesListener
					.onBillingError(context.getString(R.string.err_subscription_not_supported));
		}
		return responseCode == BillingResponse.OK;
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
			public void onBillingSetupFinished(@BillingResponse int billingResponseCode) {
				// The billing client is ready. You can query purchases here.
				BaseActivity.printLog(TAG, "Setup finished");
				if (billingResponseCode == BillingResponse.OK) {
					isServiceConnected = true;
					if (executeOnSuccess != null) {
						executeOnSuccess.run();
					}
				}
				logErrorType(billingResponseCode);
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
	 * Logs Billing Client Success, Failure and error responses.
	 *
	 * @param responseCode Billing Client Response code to identify the states of Billing Client
	 *                     Responses.
	 *
	 * @see <a href="https://developer.android.com/google/play/billing/billing_reference.html">
	 * Google Play InApp Purchase Response Types Guide</a>
	 */
	private void logErrorType(int responseCode) {
		switch (responseCode) {
			case BillingResponse.DEVELOPER_ERROR:
			case BillingResponse.BILLING_UNAVAILABLE:
				BaseActivity.printLog(TAG, "Billing unavailable. "
						+ "Make sure your Google Play app is setup correctly");
				break;
			case BillingResponse.SERVICE_DISCONNECTED:
				billingUpdatesListener
						.onBillingError(context.getString(R.string.err_service_disconnected));
				break;
			case BillingResponse.OK:
				BaseActivity.printLog(TAG, "Setup successful!");
				break;
			case BillingResponse.USER_CANCELED:
				BaseActivity.printLog(TAG, "User has cancelled Purchase!");
				break;
			case BillingResponse.SERVICE_UNAVAILABLE:
				billingUpdatesListener
						.onBillingError(context.getString(R.string.err_no_internet));
				break;
			case BillingResponse.ITEM_UNAVAILABLE:
				BaseActivity.printLog(TAG, "Product is not available for purchase");
				break;
			case BillingResponse.ERROR:
				BaseActivity.printLog(TAG, "fatal error during API action");
				break;
			case BillingResponse.ITEM_ALREADY_OWNED:
				BaseActivity.printLog(TAG,
						"Failure to purchase since item is already owned");
				break;
			case BillingResponse.ITEM_NOT_OWNED:
				BaseActivity.printLog(TAG, "Failure to consume since item is not owned");
				break;
			default:
				BaseActivity.printLog(TAG, "Billing unavailable. Please check your device");
				break;
		}
	}

	@Override
	public void onPurchasesUpdated(@BillingResponse int responseCode,
	                               @Nullable List<Purchase> purchases) {
		if (responseCode == BillingResponse.OK && purchases != null) {
			for (Purchase purchase : purchases) {
				handlePurchase(purchase);
			}
			storePurchaseResultsLocally(myPurchasesResultList);
			for (Purchase purchase : purchases) {
				if (purchase.getSku().equals(BillingConstants.SKU_BUY_APPLE)) {
					consumeAsync(purchase.getPurchaseToken());
				}
			}
			if (purchases.size() > 0) {
				BaseActivity.printLog(TAG, "purchase list size: " + purchases.size());
			}
		} else if (responseCode == BillingResponse.USER_CANCELED) {
			// Handle an error caused by a user cancelling the purchase flow.
			BaseActivity.printLog(TAG,
					"onPurchasesUpdate() - user cancelled the purchase flow - skipping");
		} else {
			// Handle any other error codes.
			BaseActivity.printLog(TAG,
					"onPurchasesUpdate() got unknown responseCode: " + responseCode);
			logErrorType(responseCode);
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
	 * Consumes InApp Product Purchase after successful purchase of InApp Product Purchase. InApp
	 * Products cannot be bought after a purchase was made. We need to consume it after a
	 * successful purchase, so that we can purchase again and it will become available for the
	 * next time we make purchase of the same product that was bought before.
	 *
	 * @param purchaseToken a token that uniquely identifies a purchase for a given item and user
	 *                      pair.
	 */
	private void consumeAsync(final String purchaseToken) {
		// If we've already scheduled to consume this token - no action is needed (this could happen
		// if you received the token when querying purchases inside onReceive() and later from
		// onActivityResult()
		if (tokensToBeConsumed == null) {
			tokensToBeConsumed = new HashSet<>();
		} else if (tokensToBeConsumed.contains(purchaseToken)) {
			BaseActivity.printLog(TAG,
					"Token was already scheduled to be consumed - skipping...");
			return;
		}
		tokensToBeConsumed.add(purchaseToken);
		// Generating Consume Response listener
		final ConsumeResponseListener onConsumeListener = new ConsumeResponseListener() {
			@Override
			public void onConsumeResponse(@BillingResponse int responseCode, String purchaseToken) {
				// If billing service was disconnected, we try to reconnect 1 time
				// (feel free to introduce your retry policy here).
				BaseActivity.printLog(TAG,
						"Consume Response, Purchase Token: " + purchaseToken);
				logErrorType(responseCode);
			}
		};
		// Creating a runnable from the request to use it inside our connection retry policy below
		Runnable consumeRequest = new Runnable() {
			@Override
			public void run() {
				// Consume the purchase async
				myBillingClient.consumeAsync(purchaseToken, onConsumeListener);
			}
		};
		executeServiceRequest(consumeRequest);
	}

	/**
	 * Stores SKU Details on local storage.
	 *
	 * @param skuDetailsList list of SKU Details returned from the queries.
	 */
	private void storeSkuDetailsLocally(List<SkuDetails> skuDetailsList) {
		final List<BillingSkuDetails> billingSkuDetailsList = new ArrayList<>();
		for (SkuDetails skuDetails : skuDetailsList) {
			BillingSkuDetails billingSkuDetails = new BillingSkuDetails();
			billingSkuDetails.skuID = skuDetails.getSku();
			billingSkuDetails.skuType = skuDetails.getType();
			billingSkuDetails.skuPrice = skuDetails.getPrice();
			billingSkuDetailsList.add(billingSkuDetails);
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				AppDatabase.getAppDatabase(context)
						.getBillingDao().insertSkuDetails(billingSkuDetailsList);
			}
		}).start();
	}

	/**
	 * Stores Purchase Details on local storage.
	 *
	 * @param purchasesResultList list of Purchase Details returned from the queries.
	 */
	private void storePurchaseResultsLocally(List<Purchase> purchasesResultList) {
		final List<BillingPurchaseDetails> billingPurchaseDetailsList = new ArrayList<>();
		for (Purchase purchase : purchasesResultList) {
			BillingPurchaseDetails billingPurchaseDetails = new BillingPurchaseDetails();
			billingPurchaseDetails.purchaseToken = purchase.getPurchaseToken();
			billingPurchaseDetails.orderID = purchase.getOrderId();
			billingPurchaseDetails.skuID = purchase.getSku();
			billingPurchaseDetails.purchaseTime = purchase.getPurchaseTime();
			billingPurchaseDetailsList.add(billingPurchaseDetails);
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				AppDatabase.getAppDatabase(context)
						.getBillingDao().insertPurchaseDetails(billingPurchaseDetailsList);
			}
		}).start();
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
