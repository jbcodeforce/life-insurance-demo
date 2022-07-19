package org.acme.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ClientRelationType {
    public int id;
    public String relation_type;

    public ClientRelationType(){}

    public ClientRelationType(int id, String relation_type) {
        this.id = id;
        this.relation_type = relation_type;
    }

    
}
