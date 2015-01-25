/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Backend with Google Cloud Messaging" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/GcmEndpoints
*/

package com.pagr.backend;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.pagr.backend.OfyService.ofy;

/**
 * An endpoint to send messages to devices registered with the backend
 * <p/>
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 * <p/>
 * NOTE: This endpoint does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Api(name = "messaging", version = "v1", namespace = @ApiNamespace(ownerDomain = "backend.pagr.com", ownerName = "backend.pagr.com", packagePath = ""))
public class MessagingEndpoint {
    private static final Logger log = Logger.getLogger(MessagingEndpoint.class.getName());

    /**
     * Api Keys can be obtained from the google cloud console
     */
    private static final String API_KEY = System.getProperty("gcm.api.key");

    /**
     * Send to the first 10 devices (You can modify this to send to any number of devices or a specific device)
     *
     * @param message The message to send
     */
    public void sendMessage(@Named("message") String message) throws IOException {
        if (message == null || message.trim().length() == 0) {
            log.warning("Not sending message because it is empty");
            return;
        }

        Alarm alarm = new Alarm();
        alarm.setAlarmDate(new Date());
        alarm.setMessage(message);

        List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).list();
        for (RegistrationRecord record : records) {
            alarm.getPendingReplies().add(Ref.create(record));
        }
        ofy().save().entity(alarm).now();
        sendAll(alarm);
    }

    @ApiMethod(
            name = "alarms.get",
            path = "alarms/{id}",
            httpMethod = ApiMethod.HttpMethod.GET
    )
    public Alarm getAlarm(@Named("id") long id) {
        return ofy().load().key(Key.create(Alarm.class, id)).now();
    }

    @ApiMethod(
            name = "alarms.postStatus",
            path = "alarms/{id}/status",
            httpMethod = ApiMethod.HttpMethod.POST
    )
    public void postStatus(@Named("id") long alarmId, @Named("regId") String regId) {
        Alarm alarm = ofy().load().key(Key.create(Alarm.class, alarmId)).now();
        RegistrationRecord registrationRecord = RegistrationEndpoint.findRecord(regId);
        alarm.getPendingReplies().remove(Ref.create(registrationRecord));
        ofy().save().entity(alarm).now();
    }

    private void sendAll(Alarm alarm) throws IOException {
        String alarmId = Long.toString(alarm.id);
        Message msg = new Message.Builder()
                .addData("message", alarm.getMessage())
                .addData("alarmId", alarmId)
                .build();
        Sender sender = new Sender(API_KEY);
        for (Ref<RegistrationRecord> recordRef : alarm.getPendingReplies()) {
            RegistrationRecord record = recordRef.get();
            Result result = sender.send(msg, record.getRegId(), 5);
            if (result.getMessageId() != null) {
                log.info("Message sent to " + record.getRegId());
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    log.info("Registration Id changed for " + record.getRegId() + " updating to " + canonicalRegId);
                    record.setRegId(canonicalRegId);
                    ofy().save().entity(record).now();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    ofy().delete().entity(record).now();
                } else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
    }
}
