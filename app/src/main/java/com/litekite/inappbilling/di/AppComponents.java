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

package com.litekite.inappbilling.di;

import android.content.Context;

import com.litekite.inappbilling.network.NetworkManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Provides dependencies for the whole application components
 * and lives as long as application is running.
 *
 * @author Vignesh S
 * @version 1.0, 15/01/2020
 * @since 1.0
 */
@Module
@InstallIn(ApplicationComponent.class)
class AppComponents {

	@Singleton
	@Provides
	static NetworkManager provideNetworkManager(@ApplicationContext Context context) {
		return new NetworkManager(context);
	}

}