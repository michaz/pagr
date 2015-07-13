package com.pagr.backend;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class CellDevice {

    @Id
    String cellId;

}
