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

package com.litekite.inappbilling.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.litekite.inappbilling.room.database.AppDatabase;
import com.litekite.inappbilling.room.entity.BillingSkuRelatedPurchases;

import java.util.List;

/**
 * ProductsAndPurchasesVM, a view model which gets Sku Products List and its related Purchases
 * from local database and updates it to the observing view.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class ProductsAndPurchasesVM extends AndroidViewModel implements LifecycleObserver {

	private LiveData<List<BillingSkuRelatedPurchases>> skuProductsAndPurchasesList;

	/**
	 * Makes a call to get Sku Product Details and its related Purchases from local database.
	 *
	 * @param application application An Application Instance.
	 */
	public ProductsAndPurchasesVM(@NonNull Application application) {
		super(application);
		fetchFromDB();
	}

	/**
	 * Fetches Sku Products List and its related Purchases stored in the local database and assigns
	 * it to {@link #skuProductsAndPurchasesList} LiveData.
	 */
	private void fetchFromDB() {
		skuProductsAndPurchasesList = AppDatabase.getAppDatabase(this.getApplication())
				.getBillingDao().getSkuRelatedPurchases();
	}

	/**
	 * A view gets this {@link #skuProductsAndPurchasesList} and observes for changes and updates
	 * with it.
	 *
	 * @return a LiveData of Sku Products List and its related Purchases.
	 */
	@NonNull
	public LiveData<List<BillingSkuRelatedPurchases>> getSkuProductsAndPurchasesList() {
		if (skuProductsAndPurchasesList == null) {
			skuProductsAndPurchasesList = new MutableLiveData<>();
		}
		return skuProductsAndPurchasesList;
	}

	@SuppressWarnings("unused")
	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	void onDestroy() {
		AppDatabase.destroyAppDatabase();
	}

}