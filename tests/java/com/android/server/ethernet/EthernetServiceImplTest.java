/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.ethernet;

import static org.junit.Assert.assertThrows;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.IEthernetNetworkManagementListener;
import android.net.EthernetNetworkUpdateRequest;
import android.net.IpConfiguration;
import android.net.NetworkCapabilities;
import android.net.StaticIpConfiguration;
import android.os.Handler;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EthernetServiceImplTest {
    private static final String TEST_IFACE = "test123";
    private static final EthernetNetworkUpdateRequest UPDATE_REQUEST =
            new EthernetNetworkUpdateRequest(
                    new StaticIpConfiguration(), new NetworkCapabilities.Builder().build());
    private static final IEthernetNetworkManagementListener NULL_LISTENER = null;
    private EthernetServiceImpl mEthernetServiceImpl;
    @Mock private Context mContext;
    @Mock private Handler mHandler;
    @Mock private EthernetTracker mEthernetTracker;
    @Mock private PackageManager mPackageManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mEthernetServiceImpl = new EthernetServiceImpl(mContext, mHandler, mEthernetTracker);
        mEthernetServiceImpl.mStarted.set(true);
        toggleAutomotiveFeature(true);
        shouldTrackIface(TEST_IFACE, true);
    }

    private void toggleAutomotiveFeature(final boolean isEnabled) {
        doReturn(isEnabled)
                .when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    private void shouldTrackIface(@NonNull final String iface, final boolean shouldTrack) {
        doReturn(shouldTrack).when(mEthernetTracker).isTrackingInterface(iface);
    }

    @Test
    public void testSetConfigurationRejectsWhenEthNotStarted() {
        mEthernetServiceImpl.mStarted.set(false);
        assertThrows(IllegalStateException.class, () -> {
            mEthernetServiceImpl.setConfiguration("" /* iface */, new IpConfiguration());
        });
    }

    @Test
    public void testUpdateConfigurationRejectsWhenEthNotStarted() {
        mEthernetServiceImpl.mStarted.set(false);
        assertThrows(IllegalStateException.class, () -> {
            mEthernetServiceImpl.updateConfiguration(
                    "" /* iface */, UPDATE_REQUEST, null /* listener */);
        });
    }

    @Test
    public void testConnectNetworkRejectsWhenEthNotStarted() {
        mEthernetServiceImpl.mStarted.set(false);
        assertThrows(IllegalStateException.class, () -> {
            mEthernetServiceImpl.connectNetwork("" /* iface */, null /* listener */);
        });
    }

    @Test
    public void testDisconnectNetworkRejectsWhenEthNotStarted() {
        mEthernetServiceImpl.mStarted.set(false);
        assertThrows(IllegalStateException.class, () -> {
            mEthernetServiceImpl.disconnectNetwork("" /* iface */, null /* listener */);
        });
    }

    @Test
    public void testUpdateConfigurationRejectsNullIface() {
        assertThrows(NullPointerException.class, () -> {
            mEthernetServiceImpl.updateConfiguration(null, UPDATE_REQUEST, NULL_LISTENER);
        });
    }

    @Test
    public void testConnectNetworkRejectsNullIface() {
        assertThrows(NullPointerException.class, () -> {
            mEthernetServiceImpl.connectNetwork(null /* iface */, NULL_LISTENER);
        });
    }

    @Test
    public void testDisconnectNetworkRejectsNullIface() {
        assertThrows(NullPointerException.class, () -> {
            mEthernetServiceImpl.disconnectNetwork(null /* iface */, NULL_LISTENER);
        });
    }

    @Test
    public void testUpdateConfigurationRejectsWithoutAutomotiveFeature() {
        toggleAutomotiveFeature(false);
        assertThrows(UnsupportedOperationException.class, () -> {
            mEthernetServiceImpl.updateConfiguration(TEST_IFACE, UPDATE_REQUEST, NULL_LISTENER);
        });
    }

    @Test
    public void testConnectNetworkRejectsWithoutAutomotiveFeature() {
        toggleAutomotiveFeature(false);
        assertThrows(UnsupportedOperationException.class, () -> {
            mEthernetServiceImpl.connectNetwork("" /* iface */, NULL_LISTENER);
        });
    }

    @Test
    public void testDisconnectNetworkRejectsWithoutAutomotiveFeature() {
        toggleAutomotiveFeature(false);
        assertThrows(UnsupportedOperationException.class, () -> {
            mEthernetServiceImpl.disconnectNetwork("" /* iface */, NULL_LISTENER);
        });
    }

    @Test
    public void testUpdateConfigurationRejectsWithUntrackedIface() {
        shouldTrackIface(TEST_IFACE, false);
        assertThrows(UnsupportedOperationException.class, () -> {
            mEthernetServiceImpl.updateConfiguration(TEST_IFACE, UPDATE_REQUEST, NULL_LISTENER);
        });
    }

    @Test
    public void testConnectNetworkRejectsWithUntrackedIface() {
        shouldTrackIface(TEST_IFACE, false);
        assertThrows(UnsupportedOperationException.class, () -> {
            mEthernetServiceImpl.connectNetwork(TEST_IFACE, NULL_LISTENER);
        });
    }

    @Test
    public void testDisconnectNetworkRejectsWithUntrackedIface() {
        shouldTrackIface(TEST_IFACE, false);
        assertThrows(UnsupportedOperationException.class, () -> {
            mEthernetServiceImpl.disconnectNetwork(TEST_IFACE, NULL_LISTENER);
        });
    }

    @Test
    public void testUpdateConfiguration() {
        mEthernetServiceImpl.updateConfiguration(TEST_IFACE, UPDATE_REQUEST, NULL_LISTENER);
        verify(mEthernetTracker).updateConfiguration(
                eq(TEST_IFACE),
                eq(UPDATE_REQUEST.getIpConfig()),
                eq(UPDATE_REQUEST.getNetworkCapabilities()), eq(NULL_LISTENER));
    }

    @Test
    public void testConnectNetwork() {
        mEthernetServiceImpl.connectNetwork(TEST_IFACE, NULL_LISTENER);
        verify(mEthernetTracker).connectNetwork(eq(TEST_IFACE), eq(NULL_LISTENER));
    }

    @Test
    public void testDisconnectNetwork() {
        mEthernetServiceImpl.disconnectNetwork(TEST_IFACE, NULL_LISTENER);
        verify(mEthernetTracker).disconnectNetwork(eq(TEST_IFACE), eq(NULL_LISTENER));
    }
}
