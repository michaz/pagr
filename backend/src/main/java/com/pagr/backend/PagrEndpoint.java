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
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.pagr.backend.OfyService.ofy;

@Api(name = "pagr", version = "v1", namespace = @ApiNamespace(ownerDomain = "backend.pagr.com", ownerName = "backend.pagr.com", packagePath = ""))
public class PagrEndpoint {
    private static final Logger log = Logger.getLogger(PagrEndpoint.class.getName());

    private static final String API_KEY = System.getProperty("gcm.api.key");

    @ApiMethod(
            name = "cellupdate.post",
            path = "cellupdate/post",
            httpMethod = ApiMethod.HttpMethod.POST
    )
    public void postCell(CellUpdate cellUpdate) {
        cellUpdate.setDone(false);
        ofy().save().entity(cellUpdate).now();
    }

    @ApiMethod(
            name = "cellupdate.enqueue",
            path = "cellupdate/enqueue",
            httpMethod = ApiMethod.HttpMethod.POST
    )
    public void enqueueCellUpdates() {
        Queue queue = QueueFactory.getDefaultQueue();
        queue.add(TaskOptions.Builder.withUrl("/mapmatch"));
    }

    @ApiMethod(
            name = "alarms.post",
            path = "alarms/post",
            httpMethod = ApiMethod.HttpMethod.POST
    )
    public void postAlarm(@Named("message") String message, @Named("topic") String topicId) throws IOException {
        if (message == null || message.trim().length() == 0) {
            log.warning("Not sending message because it is empty");
            return;
        }

        Alarm alarm = new Alarm();
        alarm.setAlarmDate(new Date());
        alarm.setMessage(message);

        List<RegistrationRecord> records;
        if (topicId == null || topicId.trim().length() == 0) {
            records = ofy().load().type(RegistrationRecord.class).list();
        } else {
            records = new ArrayList<>();
            Topic topic = ofy().load().key(Key.create(Topic.class, topicId)).now();
            for (Ref<RegistrationRecord> ref : topic.getSubscriptions()) {
                records.add(ref.get());
            }
        }

        for (RegistrationRecord record : records) {
            alarm.getPendingReplies().add(Ref.create(record));
        }
        ofy().save().entity(alarm).now();
        sendAll(alarm);
    }

    @ApiMethod(
            name = "alarms.subscribe",
            path = "alarms/subscribe",
            httpMethod = ApiMethod.HttpMethod.POST
    )
    public void subscribe(@Named("topic") String topicId, @Named("regId") String regId) {
        Topic topic = ofy().load().key(Key.create(Topic.class, topicId)).now();
        if (topic == null) {
            topic = new Topic();
            topic.setId(topicId);
        }
        RegistrationRecord registrationRecord = findRecord(regId);
        topic.getSubscriptions().add(Ref.create(registrationRecord));
        ofy().save().entity(topic);
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
            name = "alarms.list",
            path = "alarms/list",
            httpMethod = ApiMethod.HttpMethod.GET
    )
    public Collection<Alarm> listAlarms() {
        return ofy().load().type(Alarm.class).list();
    }

    @ApiMethod(
            name = "alarms.postStatus",
            path = "alarms/{id}/status",
            httpMethod = ApiMethod.HttpMethod.POST
    )
    public void postStatus(@Named("id") long alarmId, @Named("regId") String regId) throws IOException {
        Alarm alarm = ofy().load().key(Key.create(Alarm.class, alarmId)).now();
        RegistrationRecord registrationRecord = findRecord(regId);
        alarm.getPendingReplies().remove(Ref.create(registrationRecord));
        ofy().save().entity(alarm).now();
        notifyDataChanged(registrationRecord);
    }

    private void notifyDataChanged(RegistrationRecord registrationRecord) throws IOException {
        Message msg = new Message.Builder().build();
        Sender sender = new Sender(API_KEY);
        sender.send(msg, registrationRecord.getRegId(), 5);
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
            handleResult(record, result);
        }
    }

    private void handleResult(RegistrationRecord record, Result result) {
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

    /**
     * Register a device to the backend
     *
     * @param regId The Google Cloud Messaging registration Id to add
     */
    @ApiMethod(name = "devices.register")
    public void registerDevice(@Named("regId") String regId) {
        if (findRecord(regId) != null) {
            log.info("Device " + regId + " already registered, skipping register");
            return;
        }
        RegistrationRecord record = new RegistrationRecord();
        record.setRegId(regId);
        ofy().save().entity(record).now();
    }

    /**
     * Unregister a device from the backend
     *
     * @param regId The Google Cloud Messaging registration Id to remove
     */
    @ApiMethod(name = "devices.unregister")
    public void unregisterDevice(@Named("regId") String regId) {
        RegistrationRecord record = findRecord(regId);
        if (record == null) {
            log.info("Device " + regId + " not registered, skipping unregister");
            return;
        }
        ofy().delete().entity(record).now();
    }

    /**
     * Return a collection of registered devices
     *
     * @param count The number of devices to list
     * @return a list of Google Cloud Messaging registration Ids
     */
    @ApiMethod(name = "devices.list")
    public CollectionResponse<RegistrationRecord> listDevices(@Named("count") int count) {
        List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).limit(count).list();
        return CollectionResponse.<RegistrationRecord>builder().setItems(records).build();
    }

    static RegistrationRecord findRecord(String regId) {
        return ofy().load().type(RegistrationRecord.class).filter("regId", regId).first().now();
    }
}
