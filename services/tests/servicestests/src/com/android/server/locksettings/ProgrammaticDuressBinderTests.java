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

package com.android.server.locksettings;

import static com.android.internal.widget.LockDomain.Primary;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;
import android.platform.test.annotations.Presubmit;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * T-REMEDIATE-B1-DURESS item 6: every Binder LSKF check/verify must hit
 * {@link DuressPasswordHelper#onVerifyCredentialResult}. Does not exercise
 * {@link SecureWipeEngine} (lockscreen wipe stays {@code Reason.DURESS}).
 *
 * <p>atest FrameworksServicesTests:ProgrammaticDuressBinderTests
 */
@SmallTest
@Presubmit
@RunWith(AndroidJUnit4.class)
public class ProgrammaticDuressBinderTests extends BaseLockSettingsServiceTests {

    @Before
    public void setUp() {
        mService.initializeSyntheticPassword(PRIMARY_USER_ID);
        mService.initializeSyntheticPassword(MANAGED_PROFILE_USER_ID);
    }

    @Test
    public void checkCredential_wrongGuess_invokesDuressHelper() throws RemoteException {
        LockscreenCredential password = newPassword("password");
        setPrimaryCredential(password);
        resetDuressHelper();

        assertTrue(mService.checkCredential(
                newPassword("wrong-password"), Primary, PRIMARY_USER_ID, null).isOtherError());

        verifyDuressHelperInvoked();
    }

    @Test
    public void checkCredential_correctGuess_stillInvokesDuressHelper() throws RemoteException {
        LockscreenCredential password = newPassword("password");
        setPrimaryCredential(password);
        resetDuressHelper();

        assertTrue(mService.checkCredential(password, Primary, PRIMARY_USER_ID, null)
                .isMatched());

        verifyDuressHelperInvoked();
    }

    @Test
    public void verifyCredential_wrongGuess_invokesDuressHelper() throws RemoteException {
        LockscreenCredential password = newPassword("password");
        setPrimaryCredential(password);
        resetDuressHelper();

        assertTrue(mService.verifyCredential(
                newPassword("wrong-password"), PRIMARY_USER_ID, 0).isOtherError());

        verifyDuressHelperInvoked();
    }

    @Test
    public void verifyTiedProfileChallenge_wrongParent_invokesDuressHelper()
            throws RemoteException {
        LockscreenCredential unified = newPassword("unified-password");
        setPrimaryCredential(unified);
        mService.setSeparateProfileChallengeEnabled(MANAGED_PROFILE_USER_ID, false, null);
        resetDuressHelper();

        assertTrue(mService.verifyTiedProfileChallenge(
                newPassword("wrong-parent"), MANAGED_PROFILE_USER_ID, 0).isOtherError());

        verifyDuressHelperInvoked();
    }

    @Test
    public void verifyGatekeeperPasswordHandle_doesNotInvokeDuressHelper() {
        resetDuressHelper();

        VerifyCredentialResponse response =
                mService.verifyGatekeeperPasswordHandle(1L, 0L, PRIMARY_USER_ID);
        assertTrue(response.isOtherError());

        verify(mInjector.mDuressPasswordHelper, never()).onVerifyCredentialResult(
                nullable(VerifyCredentialResponse.class),
                nullable(LockscreenCredential.class));
    }

    @Test
    public void setLockCredential_wrongSaved_invokesDuressHelper() throws RemoteException {
        LockscreenCredential password = newPassword("password");
        setPrimaryCredential(password);
        resetDuressHelper();

        assertFalse(mService.setLockCredential(
                newPassword("new-password"), newPassword("wrong-saved"), PRIMARY_USER_ID));

        verifyDuressHelperInvoked();
    }

    @Test
    public void getHashFactor_wrongCredential_invokesDuressHelper() throws RemoteException {
        LockscreenCredential password = newPassword("password");
        setPrimaryCredential(password);
        resetDuressHelper();

        assertNull(mService.getHashFactor(newPassword("wrong-password"), PRIMARY_USER_ID));

        verifyDuressHelperInvoked();
    }

    private void setPrimaryCredential(LockscreenCredential credential) throws RemoteException {
        assertTrue(mService.setLockCredential(credential, nonePassword(), PRIMARY_USER_ID));
    }

    private void resetDuressHelper() {
        reset(mInjector.mDuressPasswordHelper);
    }

    private void verifyDuressHelperInvoked() {
        verify(mInjector.mDuressPasswordHelper, atLeastOnce()).onVerifyCredentialResult(
                nullable(VerifyCredentialResponse.class),
                nullable(LockscreenCredential.class));
    }
}
