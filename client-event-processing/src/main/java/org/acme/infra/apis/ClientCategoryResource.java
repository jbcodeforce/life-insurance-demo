package org.acme.infra.apis;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.domain.ClientCategory;
import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.reactive.messaging.MutinyEmitter;

@Path("/api/v1/clientagent")
public class ClientCategoryResource {


    public ClientCategoryResource(){
        
    }

}