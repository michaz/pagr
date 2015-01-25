/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pagr.pagr;

import android.content.Context;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.pagr.backend.registration.Registration;

import java.io.IOException;

import static com.pagr.pagr.CommonUtilities.TAG;
import static com.pagr.pagr.CommonUtilities.displayMessage;

/**
 * Helper class used to communicate with the demo server.
 */
public final class ServerUtilities {

    private Registration registrationService;

    public ServerUtilities() {
        Registration.Builder builder = new Registration.Builder(
                AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null)
                .setRootUrl("https://pagrff.appspot.com/_ah/api/")
                .setGoogleClientRequestInitializer(
                        new GoogleClientRequestInitializer() {
                            @Override
                            public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest)
                                    throws IOException {
                                abstractGoogleClientRequest.setDisableGZipContent(true);
                            }
                        }
                );
        registrationService = builder.build();
    }

    /**
     * Register this account/device pair with the server.
     */
    void register(final Context context, final String regId) {
        Log.i(TAG, "registering device (regId = " + regId + ")");
        try {
            displayMessage(context, context.getString(R.string.server_registering));
            registrationService.register(regId).execute();
            String message = context.getString(R.string.server_registered);
            CommonUtilities.displayMessage(context, message);
        } catch (IOException e) {
            String message = context.getString(R.string.server_register_error);
            CommonUtilities.displayMessage(context, message);
        }
    }

    /**
     * Unregister this account/device pair within the server.
     */
    void unregister(final Context context, final String regId) {
        Log.i(TAG, "unregistering device (regId = " + regId + ")");
        try {
            registrationService.unregister(regId);
            String message = context.getString(R.string.server_unregistered);
            CommonUtilities.displayMessage(context, message);
        } catch (IOException e) {
            // At this point the device is unregistered from GCM, but still
            // registered in the server.
            // We could try to unregister again, but it is not necessary:
            // if the server tries to send a message to the device, it will get
            // a "NotRegistered" error message and should unregister the device.
            String message = context.getString(R.string.server_unregister_error, e.getMessage());
            CommonUtilities.displayMessage(context, message);
        }
    }

}
