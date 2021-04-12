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
package com.litekite.monetize.app;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.litekite.monetize.BuildConfig;
import dagger.hilt.android.HiltAndroidApp;

/**
 * Application class.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @see <a href="https://developer.android.com/topic/libraries/architecture/index.html">This app
 *     uses Google Play Billing Library, Architecture Components (LiveData, ViewModel, Room
 *     Persistance Library) and Data Binding Library</a>
 * @see <a href="https://developer.android.com/reference/android/os/StrictMode">Strict Mode</a>
 * @since 1.0
 */
@HiltAndroidApp
public class MonetizeApp extends Application {

    private static final String TAG = MonetizeApp.class.getName();

    /**
     * Logs messages for Debugging Purposes.
     *
     * @param tag TAG is a class name in which the log come from.
     * @param message Type of a Log Message.
     */
    public static void printLog(@NonNull String tag, @NonNull String message) {
        Log.d(tag, message);
    }

    /**
     * @param context An Activity or Application Context.
     * @param resId A message resId that to be displayed inside a Toast.
     */
    public static void showToast(@NonNull Context context, @StringRes int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .detectDiskReads()
                            .detectDiskWrites()
                            .detectNetwork()
                            .penaltyLog()
                            .build());
            StrictMode.setVmPolicy(
                    new StrictMode.VmPolicy.Builder()
                            .detectLeakedSqlLiteObjects()
                            .detectLeakedClosableObjects()
                            .penaltyLog()
                            .penaltyDeath()
                            .build());
        }
        super.onCreate();
        printLog(TAG, "onCreate:");
    }

    @Override
    protected void attachBaseContext(@Nullable Context base) {
        super.attachBaseContext(base);
        printLog(TAG, "attachBaseContext:");
    }
}
