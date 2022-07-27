package org.acme.infra.events;

import org.apache.kafka.common.serialization.Serde;

public class ClientOutputSerdes {
    
    public static Serde<ClientOutput> ClientOutputSerde() {
        return new JSONSerde<ClientOutput>(ClientOutput.class.getCanonicalName());
    }
}
