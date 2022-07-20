package org.acme.domain;

import java.util.Date;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ClientCategory {
    public int id;
    public String category_name;
    public long updatedTime;

    public ClientCategory(){}

    public ClientCategory(int id, String category_name) {
        this.id = id;
        this.category_name = category_name;
        this.updatedTime = new Date().getTime();
    }
}
