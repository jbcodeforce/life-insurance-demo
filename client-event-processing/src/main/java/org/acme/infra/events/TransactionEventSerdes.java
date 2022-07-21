package org.acme.infra.events;

import org.apache.kafka.common.serialization.Serde;

public class TransactionEventSerdes {

    public static Serde<TransactionEvent> TransactionSerde() {
        return new JSONSerde<TransactionEvent>(TransactionEvent.class.getCanonicalName());
    }

    public static Serde<ClientCategory> ClientCategorySerde() {
        return new JSONSerde<ClientCategory>(ClientCategory.class.getCanonicalName());
    }
}
