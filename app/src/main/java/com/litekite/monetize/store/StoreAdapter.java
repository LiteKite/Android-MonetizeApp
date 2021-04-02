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
package com.litekite.monetize.store;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.litekite.monetize.R;
import com.litekite.monetize.billing.BillingManager;
import com.litekite.monetize.databinding.AdapterStoreItemBinding;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import java.util.List;

/**
 * StoreAdapter, a RecyclerViewAdapter which provides product item and each product item has its own
 * name and price. Each product has respective buy button if it wasn't already purchased. This is
 * not applicable for inApp Products as it can be purchased multiple times. For subscription based
 * products, "Purchased" will be shown to indicate that it was already purchased, otherwise the buy
 * button will be there to make purchase.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class StoreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<BillingSkuRelatedPurchases> skuProductsAndPurchasesList;
    private final BillingManager billingManager;

    /**
     * Initializes attributes.
     *
     * @param context An Activity Context.
     * @param skuProductsAndPurchasesList Has Sku Products and its related purchases.
     * @param billingManager Provides access to BillingClient which perform Product Purchases from
     *     Google Play Billing Library.
     */
    public StoreAdapter(
            @NonNull Context context,
            @NonNull List<BillingSkuRelatedPurchases> skuProductsAndPurchasesList,
            @NonNull BillingManager billingManager) {
        this.context = context;
        this.skuProductsAndPurchasesList = skuProductsAndPurchasesList;
        this.billingManager = billingManager;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterStoreItemBinding adapterStoreItemBinding =
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.getContext()),
                        R.layout.adapter_store_item,
                        parent,
                        false);
        return new ViewHolderStoreProduct(adapterStoreItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolderStoreProduct viewHolderStoreProduct = (ViewHolderStoreProduct) holder;
        BillingSkuRelatedPurchases productRelatedPurchases =
                skuProductsAndPurchasesList.get(position);
        viewHolderStoreProduct.adapterStoreItemBinding.setPresenter(
                new StoreItemVM(context, billingManager, productRelatedPurchases));
        viewHolderStoreProduct.adapterStoreItemBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return skuProductsAndPurchasesList.size();
    }

    /** ViewHolderStoreProduct, which provides product view item. */
    static class ViewHolderStoreProduct extends RecyclerView.ViewHolder {

        AdapterStoreItemBinding adapterStoreItemBinding;

        /**
         * Gives product view item and its bindings.
         *
         * @param adapterStoreItemBinding Has bindings for the product view item.
         */
        ViewHolderStoreProduct(AdapterStoreItemBinding adapterStoreItemBinding) {
            super(adapterStoreItemBinding.getRoot());
            this.adapterStoreItemBinding = adapterStoreItemBinding;
        }
    }
}
