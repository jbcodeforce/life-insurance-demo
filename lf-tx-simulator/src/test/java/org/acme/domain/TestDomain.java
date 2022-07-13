package org.acme.domain;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDomain {
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void createAClient() throws JsonProcessingException{
        Person bob = new Person(1,"P01","bob","thebuilder");
        bob.address = "1 main street, CA, San Franciso";
        bob.email = "bobbuilder@email.com";
        ClientCategory cat1 = new ClientCategory(1,"Primary");
        Client client = new Client("101010","C01",bob,1);
        System.out.println(mapper.writeValueAsString(client));

    }
}
