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
package com.litekite.monetize.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.snackbar.Snackbar;
import com.litekite.monetize.R;

/**
 * BaseActivity, Provides common features and functionality available for all activities.
 *
 * @author Vignesh S
 * @version 1.0, 08/03/2018
 * @since 1.0
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    /**
     * Starts Activity animation.
     *
     * @param context An activity context.
     */
    protected static void startActivityAnimation(@NonNull Context context) {
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context)
                    .overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    /**
     * @param v A View in which the SnackBar should be displayed at the bottom of the screen.
     * @param stringResID A message that to be displayed inside a SnackBar.
     */
    public static void showSnackBar(@NonNull View v, @StringRes int stringResID) {
        Snackbar.make(v, stringResID, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Sets Toolbar, Toolbar Title and Back Navigation Button.
     *
     * @param toolbar Toolbar widget.
     * @param backBtnVisible A boolean value whether to display Toolbar back navigation button.
     * @param toolbarTitle The title of a Toolbar.
     * @param tvToolbarTitle A TextView in which the title of a Toolbar is displayed.
     */
    protected void setToolbar(
            @NonNull Toolbar toolbar,
            boolean backBtnVisible,
            @NonNull String toolbarTitle,
            @NonNull TextView tvToolbarTitle) {
        toolbar.setTitle("");
        if (backBtnVisible) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        }
        toolbar.setContentInsetsAbsolute(0, 0);
        tvToolbarTitle.setText(toolbarTitle);
        setSupportActionBar(toolbar);
        if (backBtnVisible) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
