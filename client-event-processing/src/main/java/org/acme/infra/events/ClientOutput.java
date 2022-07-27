package org.acme.infra.events;


/**
 * The Client output is a flatten and enriched event from the TransactionEvent, Client and Person information
 * this is to illustrate a common pattern of data transformation
 */
public class ClientOutput  {
    public String client_id;
    public String client_code;
    public Integer client_category_id;
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
    }

    public ClientOutput(ClientOutput oldOutput, String category_name) {
        this.client_id = oldOutput.client_id;
        this.client_code = oldOutput.client_code;
        this.client_category_id = oldOutput.client_category_id;
        this.client_category_name = category_name;
        this.first_name = oldOutput.first_name;
        this.last_name = oldOutput.last_name;
        this.address = oldOutput.address;
        this.phone = oldOutput.phone;
        this.mobile = oldOutput.mobile;
        this.email = oldOutput.email;
    }

    public String toString(){
        return "@@@@ cid:" + client_id + " cname:" + client_category_name;
    }
}