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
package com.litekite.monetize.worker;

import androidx.annotation.NonNull;
import com.litekite.monetize.app.MonetizeApp;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An Executor that uses {@link ThreadPoolExecutor} with the available thread pool size and runs
 * work in background.
 *
 * @author Vignesh S
 * @version 1.0, 26/02/2021
 * @since 1.0
 */
@Singleton
public class WorkExecutor implements Executor {

    private static final String TAG = WorkExecutor.class.getName();

    // A thread pool executor instance
    private final ThreadPoolExecutor pool;

    /**
     * Creates a new instance of {@link WorkExecutor} and it creates a new {@link
     * ThreadPoolExecutor}
     */
    @Inject
    public WorkExecutor() {
        // Gets the number of available cores (not always the same as the maximum number of cores)
        final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        // Sets the Time Unit to seconds
        final int KEEP_ALIVE_TIME = 1;
        final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        // Creates a thread pool executor
        pool =
                new ThreadPoolExecutor(
                        NUMBER_OF_CORES, // Initial pool size
                        NUMBER_OF_CORES, // Max pool size
                        KEEP_ALIVE_TIME,
                        KEEP_ALIVE_TIME_UNIT,
                        new LinkedBlockingQueue<>());
        // clears thread pool when the jvm exits or gets terminated.
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownAndAwaitTermination));
    }

    @Override
    public void execute(@NonNull Runnable command) {
        pool.execute(command);
    }

    /**
     * This Executor {@link ThreadPoolExecutor} will be kept in memory and it needs to be cleared by
     * ourselves when there was no work or when it's necessary.
     */
    private void shutdownAndAwaitTermination() {
        final int TERMINATION_AWAIT_TIMEOUT = 60;
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS)) {
                    MonetizeApp.printLog(TAG, "Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
        }
    }
}
