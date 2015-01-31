package com.pagr.backend;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Topic {

    public void setId(String id) {
        this.id = id;
    }

    @Id
    String id;

    @Load
    private Set<Ref<RegistrationRecord>> subscriptions = new HashSet<>();

    public Set<Ref<RegistrationRecord>> getSubscriptions() {
        return subscriptions;
    }

}
