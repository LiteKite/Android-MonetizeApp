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
package com.litekite.monetize.home;

import android.app.Application;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.litekite.monetize.R;
import com.litekite.monetize.base.BaseActivity;
import com.litekite.monetize.billing.BillingCallback;
import com.litekite.monetize.billing.BillingConstants;
import com.litekite.monetize.network.NetworkManager;
import com.litekite.monetize.purchase.PurchasesActivity;
import com.litekite.monetize.room.database.AppDatabase;
import com.litekite.monetize.store.StoreActivity;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * HomeVM, a view model that gives Premium Feature Purchase Status from local database to View,
 * Handles View's Click Event Actions.
 *
 * @author Vignesh S
 * @version 1.0, 10/03/2018
 * @since 1.0
 */
@HiltViewModel
public class HomeVM extends AndroidViewModel implements LifecycleObserver, BillingCallback {

    private final AppDatabase appDatabase;
    private LiveData<Boolean> isPremiumPurchased = new MutableLiveData<>();

    /**
     * Makes a call to check whether the Premium Feature was purchased and stored in the local
     * database.
     *
     * @param application An Application Instance.
     */
    @Inject
    public HomeVM(@NonNull Application application, @NonNull AppDatabase appDatabase) {
        super(application);
        this.appDatabase = appDatabase;
        // Sync with the local database
        fetchFromDB();
    }

    @BindingAdapter("android:drawableEnd")
    public static void setDrawableEnd(@NonNull Button button, @NonNull Boolean isPremiumPurchased) {
        setBtnDrawableRightEnd(button, isPremiumPurchased);
    }

    /**
     * Sets two features (View Your Purchases and Buy From Store) locked, if Premium Feature Product
     * was not purchased, Unlocked otherwise.
     *
     * @param button An instance of a Button Widget.
     * @param isPremiumPurchased A boolean value represents whether the Premium Feature Product was
     *     purchased or not.
     */
    private static void setBtnDrawableRightEnd(Button button, Boolean isPremiumPurchased) {
        if (isPremiumPurchased) {
            button.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        } else {
            button.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.ic_lock_outline_white, 0);
        }
    }

    @BindingAdapter("android:drawableRight")
    public static void setDrawableRight(
            @NonNull Button button, @NonNull Boolean isPremiumPurchased) {
        setBtnDrawableRightEnd(button, isPremiumPurchased);
    }

    /**
     * Fetches and checks whether the Premium Feature was purchased and stored in the local database
     * and assigns it to {@link #isPremiumPurchased} LiveData.
     */
    private void fetchFromDB() {
        isPremiumPurchased =
                appDatabase.getIsThisSkuPurchased(BillingConstants.SKU_UNLOCK_APP_FEATURES);
    }

    /**
     * A view gets this LiveData of Premium Feature purchased or not and observes for changes and
     * updates with it.
     *
     * @return a LiveData of Premium Feature Purchased or not.
     */
    @NonNull
    public LiveData<Boolean> getIsPremiumPurchased() {
        return isPremiumPurchased;
    }

    /**
     * Handles Click Events from View.
     *
     * @param v A view in which the click action performed.
     */
    public void onClick(@NonNull View v) {
        final int id = v.getId();
        if (id == R.id.btn_buy_from_store) {
            if (checkIsPremiumPurchased(v)) {
                StoreActivity.start(v.getContext());
            }
        } else if (id == R.id.btn_view_your_purchases) {
            if (checkIsPremiumPurchased(v)) {
                PurchasesActivity.start(v.getContext());
            }
        }
    }

    /**
     * Launches BillingPremiumDialog if Premium Purchase was not purchased. Shows a SnackBar if
     * there is no Internet Connectivity.
     *
     * @param v A view in which the click action performed.
     * @return whether the Premium Feature Purchased or not.
     */
    private boolean checkIsPremiumPurchased(View v) {
        boolean isPurchased =
                isPremiumPurchased.getValue() != null ? isPremiumPurchased.getValue() : false;
        if (!isPurchased && !NetworkManager.isOnline(v.getContext())) {
            BaseActivity.showSnackBar(v, R.string.err_no_internet);
            return false;
        }
        if (!isPurchased) {
            BillingPremiumDialog.show(v.getContext());
            return false;
        }
        return true;
    }
}
