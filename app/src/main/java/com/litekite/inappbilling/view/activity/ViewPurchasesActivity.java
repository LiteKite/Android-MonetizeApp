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

package com.litekite.inappbilling.view.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.litekite.inappbilling.R;
import com.litekite.inappbilling.databinding.ActivityViewPurchasesBinding;
import com.litekite.inappbilling.room.entity.BillingSkuRelatedPurchases;
import com.litekite.inappbilling.view.adapter.ViewPurchasesAdapter;
import com.litekite.inappbilling.viewmodel.ProductsAndPurchasesVM;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPurchasesActivity, which displays list of inApp and subscription products that are all
 * purchased by the user.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class ViewPurchasesActivity extends BaseActivity {

	private ActivityViewPurchasesBinding viewPurchasesBinding;
	private List<BillingSkuRelatedPurchases> skuProductsAndPurchasesList;
	private ViewPurchasesAdapter viewPurchasesAdapter;

	/**
	 * Observes changes and updates of Sku Products and Purchases which is stored in local database.
	 * Updates observed changes to the products list.
	 */
	private Observer<List<BillingSkuRelatedPurchases>> skuProductsAndPurchasesObserver =
			skuRelatedPurchasesList -> {
				if (skuRelatedPurchasesList != null && skuRelatedPurchasesList.size() > 0) {
					ViewPurchasesActivity.this.skuProductsAndPurchasesList.clear();
					ViewPurchasesActivity.this.skuProductsAndPurchasesList
							.addAll(skuRelatedPurchasesList);
					ViewPurchasesActivity.this.viewPurchasesAdapter.notifyDataSetChanged();
				}
			};

	/**
	 * Launches ViewPurchasesActivity.
	 *
	 * @param context An Activity Context.
	 */
	public static void start(@NonNull Context context) {
		if (context instanceof AppCompatActivity) {
			Intent intent = new Intent(context, ViewPurchasesActivity.class);
			context.startActivity(intent);
			startActivityAnimation(context);
		}
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		viewPurchasesBinding =
				DataBindingUtil.setContentView(this, R.layout.activity_view_purchases);
		init();
	}

	/**
	 * Sets Toolbar.
	 * Initializes Presenter ProductsAndPurchasesViewModel and registers LifeCycle Observers.
	 * Observes Sku Products and Purchases.
	 * Initializes RecyclerView Products List and its adapter.
	 */
	private void init() {
		setToolbar(viewPurchasesBinding.tbWidget.findViewById(R.id.toolbar),
				true,
				getString(R.string.your_purchases),
				viewPurchasesBinding.tbWidget.findViewById(R.id.tv_toolbar_title));
		ProductsAndPurchasesVM productsAndPurchasesVM =
				new ViewModelProvider(this).get(ProductsAndPurchasesVM.class);
		this.getLifecycle().addObserver(productsAndPurchasesVM);
		skuProductsAndPurchasesList = new ArrayList<>();
		viewPurchasesAdapter = new ViewPurchasesAdapter(this, skuProductsAndPurchasesList);
		viewPurchasesBinding.rvProductsPurchases.setAdapter(viewPurchasesAdapter);
		productsAndPurchasesVM.getSkuProductsAndPurchasesList()
				.observe(this, skuProductsAndPurchasesObserver);
	}

}