package com.pagr.backend;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

import java.util.ArrayList;
import java.util.Collection;

@Entity
public class LinkPassage {

    @Id
    Long id;

    @Parent @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    Ref<Route> routeRef;

    Long wayId;
    Long srcNodeId;
    Long dstNodeId;
    String geometry;
    Integer seq;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    Collection<Key<CellUpdate>> cellUpdates = new ArrayList<>();

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

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Collection<Key<CellUpdate>> getCellUpdates() {
        return cellUpdates;
    }

    public void setCellUpdates(Collection<Key<CellUpdate>> cellUpdates) {
        this.cellUpdates = cellUpdates;
    }

}
