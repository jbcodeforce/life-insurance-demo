package org.acme.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ClientRelated {
    public int id;
    public int client_id;
    public int person_id;
    public int client_relation_type;
    public String details;
    public boolean isActive;

    public ClientRelated() {}
    
}
