package com.pagr.backend;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

import java.util.List;


@Entity
public class Route {

    @Id
    Long id;

    @Ignore
    private List<LinkPassage> linkPassages;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<LinkPassage> getLinkPassages() {
        return linkPassages;
    }

    public void setLinkPassages(List<LinkPassage> linkPassages) {
        this.linkPassages = linkPassages;
    }
}
