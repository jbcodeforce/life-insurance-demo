package org.acme.infra.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@SuppressWarnings("DefaultAnnotationParam") // being explicit for the example
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_t")
@JsonSubTypes({
                  @JsonSubTypes.Type(value = Client.class, name = "cc"),
                  @JsonSubTypes.Type(value = ClientOutput.class, name = "co"),
                  @JsonSubTypes.Type(value = TransactionEvent.class, name = "te")
              })
public interface JSONSerdeCompatible {
    
}
