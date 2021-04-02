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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.litekite.monetize.R;
import com.litekite.monetize.base.BaseActivity;
import com.litekite.monetize.databinding.ActivityViewPurchasesBinding;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * PurchasesActivity, which displays list of inApp and subscription products that are all purchased
 * by the user.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
@AndroidEntryPoint
public class PurchasesActivity extends BaseActivity {

    private final List<BillingSkuRelatedPurchases> skuProductsAndPurchasesList = new ArrayList<>();
    private ActivityViewPurchasesBinding viewPurchasesBinding;
    private PurchasesAdapter purchasesAdapter;

    /**
     * Observes changes and updates of Sku Products and Purchases which is stored in local database.
     * Updates observed changes to the products list.
     */
    private final Observer<List<BillingSkuRelatedPurchases>> skuProductsAndPurchasesObserver =
            skuRelatedPurchasesList -> {
                if (skuRelatedPurchasesList != null && skuRelatedPurchasesList.size() > 0) {
                    PurchasesActivity.this.skuProductsAndPurchasesList.clear();
                    PurchasesActivity.this.skuProductsAndPurchasesList.addAll(
                            skuRelatedPurchasesList);
                    PurchasesActivity.this.purchasesAdapter.notifyDataSetChanged();
                }
            };

    /**
     * Launches PurchasesActivity.
     *
     * @param context An Activity Context.
     */
    public static void start(@NonNull Context context) {
        if (context instanceof AppCompatActivity) {
            Intent intent = new Intent(context, PurchasesActivity.class);
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
     * Sets Toolbar. Initializes Presenter ProductsAndPurchasesViewModel and registers LifeCycle
     * Observers. Observes Sku Products and Purchases. Initializes RecyclerView Products List and
     * its adapter.
     */
    private void init() {
        setToolbar(
                viewPurchasesBinding.tbWidget.toolbar,
                true,
                getString(R.string.your_purchases),
                viewPurchasesBinding.tbWidget.tvToolbarTitle);
        PurchasesVM purchasesVM = new ViewModelProvider(this).get(PurchasesVM.class);
        this.getLifecycle().addObserver(purchasesVM);
        purchasesAdapter = new PurchasesAdapter(this, skuProductsAndPurchasesList);
        viewPurchasesBinding.rvProductsPurchases.setAdapter(purchasesAdapter);
        purchasesVM.getSkuProductsAndPurchasesList().observe(this, skuProductsAndPurchasesObserver);
    }
}
