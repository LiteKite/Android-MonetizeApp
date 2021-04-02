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

import androidx.annotation.NonNull;

/**
 * A generic way of adding and removing callbacks.
 *
 * @author Vignesh S
 * @version 1.0, 31/08/2020
 * @since 1.0
 */
public interface CallbackProvider<T> {
    void addCallback(@NonNull T cb);

    void removeCallback(@NonNull T cb);
}
