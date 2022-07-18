package org.acme.infra.messages;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransactionEvent {
    public static String TX_CLIENT_CREATED = "ClientCreated";
    public static String TX_CLIENT_UPDATED = "ClientUpdated";

    public String txid;
    public String type;
    public long timestamp;
    public Object payload;

    public TransactionEvent() {}
}
