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

package com.litekite.inappbilling.room.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.litekite.inappbilling.room.dao.BillingDao;
import com.litekite.inappbilling.room.entity.BillingPurchaseDetails;
import com.litekite.inappbilling.room.entity.BillingSkuDetails;

/**
 * Database Class, Creates Database, Database Instance and destroys Database instance.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/topic/libraries/architecture/room.html">Room
 * Persistence Library Guide.</a>
 * @see <a href="https://developer.android.com/reference/android/arch/persistence/room/package-summary.html">Room
 * Persistence Library, A Reference Guide.</a>
 * @since 1.0
 */
@Database(entities = {BillingSkuDetails.class, BillingPurchaseDetails.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

	private static final String DATABASE_NAME = "InAppBilling";

	private static AppDatabase APP_DATABASE_INSTANCE;

	/**
	 * Creates Room Database Instance if was not already initiated.
	 *
	 * @param context Activity or Application Context.
	 *
	 * @return {@link #APP_DATABASE_INSTANCE}
	 */
	@NonNull
	public static AppDatabase getAppDatabase(@NonNull Context context) {
		if (APP_DATABASE_INSTANCE == null) {
			APP_DATABASE_INSTANCE =
					Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME).build();
		}
		return APP_DATABASE_INSTANCE;
	}

	/**
	 * Destroys {@link #APP_DATABASE_INSTANCE}
	 */
	public static void destroyAppDatabase() {
		APP_DATABASE_INSTANCE = null;
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