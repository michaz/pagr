package com.pagr.backend;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class LinkPassage {

    @Id
    Long id;

    @Parent
    Ref<Route> routeRef;

    Long wayId;
    Long srcNodeId;
    Long dstNodeId;
    String geometry;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWayId() {
        return wayId;
    }

    public void setWayId(Long wayId) {
        this.wayId = wayId;
    }

    public Long getSrcNodeId() {
        return srcNodeId;
    }

    public void setSrcNodeId(Long srcNodeId) {
        this.srcNodeId = srcNodeId;
    }

    public Long getDstNodeId() {
        return dstNodeId;
    }

    public void setDstNodeId(Long dstNodeId) {
        this.dstNodeId = dstNodeId;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public Ref<Route> getRouteRef() {
        return routeRef;
    }

    public void setRouteRef(Ref<Route> routeRef) {
        this.routeRef = routeRef;
    }
}
