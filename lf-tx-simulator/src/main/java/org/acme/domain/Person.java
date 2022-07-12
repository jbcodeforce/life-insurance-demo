package org.acme.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Person {
    public int id;
    public String code;
    public String first_name;
    public String last_name;
    public String address;
    public String phone;
    public String mobile;
    public String email;

    /**
    * Default constructor required for Jackson serializer
    */
    public Person() {}

    public Person(int id, String code, String first_name, String last_name) {
        this.id = id;
        this.code = code;
        this.first_name = first_name;
        this.last_name = last_name;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id='" + id + '\'' +
                ", code=" + code +
                ", first_name=" + first_name +
                ", last_name=" + last_name +
                '}';
    }
}
