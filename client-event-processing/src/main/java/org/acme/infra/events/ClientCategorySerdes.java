package org.acme.infra.events;

import org.apache.kafka.common.serialization.Serde;

public class ClientCategorySerdes {
    
    public static Serde<ClientCategory> ClientCategorySerde() {
        return new JSONSerde<ClientCategory>(ClientCategory.class.getCanonicalName());
    }
}
