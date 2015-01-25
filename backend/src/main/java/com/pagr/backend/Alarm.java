package com.pagr.backend;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Alarm {

    @Id
    Long id;

    private Date alarmDate;
    private String message;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Set<Ref<RegistrationRecord>> pendingReplies = new HashSet<>();

    public Date getAlarmDate() {
        return alarmDate;
    }

    public void setAlarmDate(Date alarmDate) {
        this.alarmDate = alarmDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<Ref<RegistrationRecord>> getPendingReplies() {
        return pendingReplies;
    }

    public Long getId() {
        return id;
    }

}
