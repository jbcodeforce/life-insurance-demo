package org.acme.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ClientRelated {
    public int id;
    public Client client;
    public Person relatedPerson;
    public int client_relation_type;
    public String details;
    public boolean isActive;

    public ClientRelated() {}

    public ClientRelated(int id, Client client, Person relatedPerson, int client_relation_type, boolean isActive) {
        this.id = id;
        this.client = client;
        this.relatedPerson = relatedPerson;
        this.client_relation_type = client_relation_type;
        this.isActive = isActive;
    }
    
    
}
