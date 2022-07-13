package org.acme.infra.messages;

import java.util.Date;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransactionEvent {
    public static String TX_CLIENT_CREATED = "ClientCreated";
    public static String TX_CLIENT_UPDATED = "ClientUpdated";

    public String txid;
    public String type;
    public Date timestamp;
    public Object payload;

    public TransactionEvent() {}
}
