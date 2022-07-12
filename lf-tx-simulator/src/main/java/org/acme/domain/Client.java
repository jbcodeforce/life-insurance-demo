package org.acme.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Client {
    public int id;
    public String code;
    public int person_id;
    public int client_category_id;

    public Client() {}

    public Client(int id, String code, int person_id, int client_category_id) {
        this.id = id;
        this.code = code;
        this.person_id = person_id;
        this.client_category_id = client_category_id;
    }

    
}
