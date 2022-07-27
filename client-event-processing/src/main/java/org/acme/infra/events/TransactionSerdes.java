package org.acme.infra.events;

import org.apache.kafka.common.serialization.Serde;

public class TransactionSerdes {
    
    public static Serde<TransactionEvent> TransactionEventSerde() {
        return new JSONSerde<TransactionEvent>(TransactionEvent.class.getCanonicalName());
    }
}
