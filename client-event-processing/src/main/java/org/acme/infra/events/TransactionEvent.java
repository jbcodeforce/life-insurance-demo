package org.acme.infra.events;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class TransactionEvent {
    public static String TX_CLIENT_CREATED = "ClientCreated";
    public static String TX_CLIENT_UPDATED = "ClientUpdated";

    public String txid;
    public String type;
    public long timestamp;
    public Client payload;

    public TransactionEvent() {}

    public String toString(){
        return "@@@@ " + txid + " type: " + type + " categor_id: " + payload.client_category_id;
    }
}
