package org.acme.infra.events;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Client {
    public String id;
    public String code;
    public Person insuredPerson;
    public int client_category_id;

    public Client() {}

    public Client(String id, String code, Person person, int client_category_id) {
        this.id = id;
        this.code = code;
        this.insuredPerson = person;
        this.client_category_id = client_category_id;
    }

    
}
