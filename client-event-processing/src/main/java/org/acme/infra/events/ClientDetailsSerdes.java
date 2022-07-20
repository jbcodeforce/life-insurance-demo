package org.acme.infra.events;

import org.apache.kafka.common.serialization.Serde;
import org.acme.domain.*;

public class ClientDetailsSerdes {

    public static Serde<Client> ClientSerde() {
        return new JSONSerde<Client>(Client.class.getCanonicalName());
    }

    public static Serde<ClientOutput> ClientOutputSerde() {
        return new JSONSerde<ClientOutput>(ClientOutput.class.getCanonicalName());
    }

}
