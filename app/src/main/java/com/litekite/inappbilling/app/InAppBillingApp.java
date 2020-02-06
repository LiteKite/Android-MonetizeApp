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

package com.litekite.inappbilling.app;

import android.app.Application;
import android.content.Context;

import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;

/**
 * Application class.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/topic/libraries/architecture/index.html"> This app
 * uses Google Play Billing Library, Architecture Components (LiveData, ViewModel, Room Persistance
 * Library) and Data Binding Library</a>
 * @since 1.0
 */
public class InAppBillingApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	@Override
	protected void attachBaseContext(@Nullable Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

}