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
package com.litekite.monetize.splash;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.litekite.monetize.R;
import com.litekite.monetize.base.BaseActivity;
import com.litekite.monetize.home.HomeActivity;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * This is an app SplashActivity that lasts 3 secs.
 *
 * @author Vignesh S
 * @version 1.0, 08/03/2018
 * @since 1.0
 */
@AndroidEntryPoint
public class SplashActivity extends BaseActivity {

    /**
     * Observes whether the SplashTimeDelay 3 secs of delay has finished. If it was finished, then
     * it moves to the HomeActivity.
     */
    private final Observer<Boolean> splashTimeDelayObserver =
            aBoolean -> {
                if (aBoolean != null && aBoolean) {
                    startActivityHome();
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
    }

    /** Initializes SplashViewModel and LifeCycle Observer for it. Observes the SplashTimeDelay. */
    private void init() {
        SplashVM splashVM = new ViewModelProvider(this).get(SplashVM.class);
        this.getLifecycle().addObserver(splashVM);
        splashVM.getSplashTimeDelay().observe(this, splashTimeDelayObserver);
    }

    /** Launches HomeActivity after the SplashTimeDelay has finished. */
    private void startActivityHome() {
        HomeActivity.start(SplashActivity.this);
        finish();
    }
}
