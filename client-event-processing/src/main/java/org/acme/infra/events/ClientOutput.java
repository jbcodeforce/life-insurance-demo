package org.acme.infra.events;

public class ClientOutput {
    public String client_id;
    public String client_code;
    public String client_category_name;
    public String first_name;
    public String last_name;
    public String address;
    public String phone;
    public String mobile;
    public String email;

    public ClientOutput() {}

    public ClientOutput(Client c, Person p) {

        this.client_id = c.id;
        this.client_code = c.code;
        this.first_name = p.first_name;
        this.last_name = p.last_name;
        this.client_category_name = "Business";
    }



}