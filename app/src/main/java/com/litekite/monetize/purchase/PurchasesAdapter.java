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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.litekite.monetize.R;
import com.litekite.monetize.databinding.AdapterPurchaseItemBinding;
import com.litekite.monetize.room.entity.BillingSkuRelatedPurchases;
import java.util.List;

/**
 * PurchasesAdapter, a RecyclerViewAdapter which provides product item and each product item has its
 * own name and represents its purchases. For inApp Products, quantity of product purchases will be
 * displayed. For subscription based products, the date of expiration will be displayed if it was
 * already purchased. Otherwise "Not Purchased Yet" will be displayed.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
public class PurchasesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<BillingSkuRelatedPurchases> skuProductsAndPurchasesList;

    /**
     * Initializes attributes.
     *
     * @param context An Activity Context.
     * @param skuProductsAndPurchasesList Has Sku Products and its related purchases.
     */
    public PurchasesAdapter(
            @NonNull Context context,
            @NonNull List<BillingSkuRelatedPurchases> skuProductsAndPurchasesList) {
        this.context = context;
        this.skuProductsAndPurchasesList = skuProductsAndPurchasesList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AdapterPurchaseItemBinding adapterPurchaseItemBinding =
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.getContext()),
                        R.layout.adapter_purchase_item,
                        parent,
                        false);
        return new ViewHolderPurchaseProduct(adapterPurchaseItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolderPurchaseProduct viewHolderPurchaseProduct = (ViewHolderPurchaseProduct) holder;
        BillingSkuRelatedPurchases productRelatedPurchases =
                skuProductsAndPurchasesList.get(position);
        viewHolderPurchaseProduct.adapterPurchaseItemBinding.setPresenter(
                new PurchaseItemVM(context, productRelatedPurchases));
        viewHolderPurchaseProduct.adapterPurchaseItemBinding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return skuProductsAndPurchasesList.size();
    }

    /** ViewHolderPurchasedProduct, which provides product view item. */
    static class ViewHolderPurchaseProduct extends RecyclerView.ViewHolder {

        AdapterPurchaseItemBinding adapterPurchaseItemBinding;

        /**
         * Gives product view item and its bindings.
         *
         * @param adapterPurchaseItemBinding Has bindings for the product view item.
         */
        ViewHolderPurchaseProduct(AdapterPurchaseItemBinding adapterPurchaseItemBinding) {
            super(adapterPurchaseItemBinding.getRoot());
            this.adapterPurchaseItemBinding = adapterPurchaseItemBinding;
        }
    }
}
