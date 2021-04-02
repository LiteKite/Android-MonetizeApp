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
package com.litekite.monetize.util;

import androidx.annotation.NonNull;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A Date and Time Utility Class, Provides features that handles Date Time Conversion, Gives
 * Formatted Date and Time and other Date and Time based Operations.
 *
 * @author Vignesh S
 * @version 1.0, 04/03/2018
 * @since 1.0
 */
public class DateTimeUtil {

    public static long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public static long THIRTY_DAYS_IN_MILLIS = 30 * 24 * 60 * 60 * 1000L;

    private DateTimeUtil() {}

    /**
     * Gets Date in Millis and returns a Formatted Date String.
     *
     * @param dateInMillis Date is represented as Millis.
     * @return formatted Date of the given Date in Millis.
     */
    @NonNull
    public static String getDateTime(long dateInMillis) {
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault());
        return dateFormat.format(new Date(dateInMillis));
    }

    /**
     * Checks whether the given Date in Millis is a past, present or future.
     *
     * @param dateInMillis Date is represented as Millis.
     * @return a boolean value of whether the given Date in Millis is ended or not by comparing it
     *     with the Current Date.
     */
    public static boolean isDateTimePast(long dateInMillis) {
        int result = new Date(dateInMillis).compareTo(new Date());
        return result < 0;
    }
}
