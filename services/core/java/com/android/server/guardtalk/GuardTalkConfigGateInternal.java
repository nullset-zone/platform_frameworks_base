/*
 * Copyright (C) 2026 The GuardTalkOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.guardtalk;

import android.annotation.NonNull;

/**
 * In-process LocalServices API for GuardTalk Security mutations (fail-closed).
 *
 * <p>Phase-2 services must call {@link #assertMutationAuthorized} before applying
 * GT Config / Security mutations. Missing LocalServices registration ⇒ callers
 * must treat as unauthorized.
 */
public abstract class GuardTalkConfigGateInternal {

    /** @return true when a verified session is active for {@code userId}. */
    public abstract boolean isAuthorized(int userId);

    /**
     * @return true when {@code mutationKey} is allowed for {@code userId}.
     * Unknown keys fail closed.
     */
    public abstract boolean isMutationAuthorized(int userId, @NonNull String mutationKey);

    /**
     * Throws {@link SecurityException} unless authorized (fail-closed).
     */
    public abstract void assertMutationAuthorized(int userId, @NonNull String mutationKey);
}
