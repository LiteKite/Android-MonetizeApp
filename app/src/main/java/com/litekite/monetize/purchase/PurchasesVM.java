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

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.litekite.monetize.room.database.AppDatabase;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;
import javax.inject.Inject;

/**
 * PurchasesVM, a view model which gets Sku Products List and its related Purchases from local
 * database and updates it to the observing view.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
@HiltViewModel
public class PurchasesVM extends AndroidViewModel implements LifecycleObserver {

    private final AppDatabase appDatabase;
    private LiveData<List<BillingSkuRelatedPurchases>> skuProductsAndPurchasesList =
            new MutableLiveData<>();

    /**
     * Makes a call to get Sku Product Details and its related Purchases from local database.
     *
     * @param application application An Application Instance.
     */
    @Inject
    public PurchasesVM(@NonNull Application application, @NonNull AppDatabase appDatabase) {
        super(application);
        this.appDatabase = appDatabase;
        // Sync with the local database
        fetchFromDB();
    }

    /**
     * Fetches Sku Products List and its related Purchases stored in the local database and assigns
     * it to {@link #skuProductsAndPurchasesList} LiveData.
     */
    private void fetchFromDB() {
        skuProductsAndPurchasesList = appDatabase.getSkuRelatedPurchases();
    }

    /**
     * A view gets this {@link #skuProductsAndPurchasesList} and observes for changes and updates
     * with it.
     *
     * @return a LiveData of Sku Products List and its related Purchases.
     */
    @NonNull
    public LiveData<List<BillingSkuRelatedPurchases>> getSkuProductsAndPurchasesList() {
        return skuProductsAndPurchasesList;
    }
}
