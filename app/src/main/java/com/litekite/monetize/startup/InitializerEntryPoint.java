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
package com.litekite.monetize.startup;

import android.content.Context;
import com.litekite.monetize.billing.BillingManager;
import com.litekite.monetize.network.NetworkManager;
import com.litekite.monetize.room.database.AppDatabase;
import com.litekite.monetize.worker.WorkExecutor;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides dependencies to the components that are not directly works with Android.
 *
 * @author Vignesh S
 * @version 1.0, 29/03/2021
 * @since 1.0
 */
@EntryPoint
@InstallIn(SingletonComponent.class)
interface InitializerEntryPoint {

    static InitializerEntryPoint getEntryPoint(Context context) {
        return EntryPointAccessors.fromApplication(context, InitializerEntryPoint.class);
    }

    BillingManager getBillingManager();

    AppDatabase getAppDatabase();

    NetworkManager getNetworkManager();

    WorkExecutor getWorkExecutor();
}
