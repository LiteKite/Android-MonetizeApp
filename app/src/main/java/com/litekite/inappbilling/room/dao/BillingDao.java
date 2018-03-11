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

package com.litekite.inappbilling.room.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import com.litekite.inappbilling.room.entity.BillingPurchaseDetails;
import com.litekite.inappbilling.room.entity.BillingSkuDetails;
import com.litekite.inappbilling.room.entity.BillingSkuRelatedPurchases;

import java.util.List;

import static com.litekite.inappbilling.billing.BillingConstants.SKU_UNLOCK_APP_FEATURES;

/**
 * DAO class, performs database operations and returns result in the form of Objects.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/reference/android/arch/persistence/room/Transaction.html">A
 * Single Transaction Guide</a>
 * @since 1.0
 */
@Dao
public interface BillingDao {

	@Transaction
	@Query("select * from billing_sku_details where sku_id != '" + SKU_UNLOCK_APP_FEATURES + "'")
	LiveData<List<BillingSkuRelatedPurchases>> getSkuRelatedPurchases();

	@Query("select * from billing_sku_details where sku_id = :skuID")
	LiveData<BillingSkuDetails> getSkuDetails(String skuID);

	@Query("select exists(select * from billing_purchase_details where sku_id = :skuID)")
	LiveData<Boolean> getIsThisSkuPurchased(String skuID);

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insertSkuDetails(List<BillingSkuDetails> billingSkuDetails);

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	void insertPurchaseDetails(List<BillingPurchaseDetails> billingPurchaseDetails);

}
