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
package com.litekite.monetize.room.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.litekite.monetize.room.dao.BillingDao;
import com.litekite.monetize.room.entity.BillingPurchaseDetails;
import com.litekite.monetize.room.entity.BillingSkuDetails;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import java.util.List;

/**
 * Database Class, Creates Database, Database Instance and destroys Database instance.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/topic/libraries/architecture/room.html">Room
 *     Persistence Library Guide.</a>
 * @see <a
 *     href="https://developer.android.com/reference/android/arch/persistence/room/package-summary.html">Room
 *     Persistence Library, A Reference Guide.</a>
 * @since 1.0
 */
@Database(
        entities = {BillingSkuDetails.class, BillingPurchaseDetails.class},
        version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "MonetizeAppDB";

    private static volatile AppDatabase APP_DATABASE_INSTANCE;

    /**
     * Creates Room Database Instance if was not already initiated.
     *
     * @param context Activity or Application Context.
     * @return {@link #APP_DATABASE_INSTANCE}
     */
    @NonNull
    public static synchronized AppDatabase getAppDatabase(@NonNull Context context) {
        if (APP_DATABASE_INSTANCE == null) {
            APP_DATABASE_INSTANCE =
                    Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).build();
        }
        return APP_DATABASE_INSTANCE;
    }

    @NonNull
    public LiveData<Boolean> getIsThisSkuPurchased(@NonNull String skuID) {
        return Transformations.map(
                getBillingDao().getIsThisSkuPurchased(skuID), input -> input != null && input != 0);
    }

    @NonNull
    public LiveData<BillingSkuDetails> getSkuDetails(@NonNull String skuID) {
        return getBillingDao().getSkuDetails(skuID);
    }

    @NonNull
    public LiveData<List<BillingSkuRelatedPurchases>> getSkuRelatedPurchases() {
        return getBillingDao().getSkuRelatedPurchases();
    }

    public void insertPurchaseDetails(
            @NonNull List<BillingPurchaseDetails> billingPurchaseDetailsList) {
        getBillingDao().insertPurchaseDetails(billingPurchaseDetailsList);
    }

    public void insertSkuDetails(@NonNull List<BillingSkuDetails> billingSkuDetailsList) {
        getBillingDao().insertSkuDetails(billingSkuDetailsList);
    }

    /**
     * Gives BillingDao Database Operations.
     *
     * @return BillingDao abstract implementation.
     */
    @SuppressWarnings("NullableProblems")
    @NonNull
    public abstract BillingDao getBillingDao();
}
