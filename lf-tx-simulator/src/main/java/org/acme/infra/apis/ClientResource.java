package org.acme.infra.apis;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.domain.Client;
import org.acme.domain.ClientCategory;
import org.acme.domain.ClientRelationType;
import org.acme.infra.messages.MQProducer;
import org.acme.infra.messages.TransactionEvent;
import org.acme.infra.repo.ClientRepository;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;


@ApplicationScoped
@Path("/api/v1/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {
    private static final Logger logger = Logger.getLogger(ClientResource.class.getName());

    @Inject
    MQProducer transactionEmitter; 

    @Inject
    ClientRepository clientRepository;


    @POST
    public Client createNewClient(Client client) {
        logger.info("In createNewClient client: " + client.toString());
         // todo add persistence 
        UUID uuid = UUID.randomUUID();
        client.id = uuid.toString();
        transactionEmitter.send(client,true);
        return client;
    }

    @PUT
    public Client updateClient(Client client) {
        logger.info("In updateClient client: " + client.toString());
        // todo add persistence 
        
        transactionEmitter.send(client,false);
        return client;
    }

    @GET
    @Path("/categories")
    public Multi<ClientCategory> getClientCategories(){
        List<ClientCategory> l = clientRepository.getListOfCategory();
        return Multi.createFrom().items(l.stream());
    }


    @GET
    @Path("/relationTypes")
    public Multi<ClientRelationType> getRelationTypes(){
        List<ClientRelationType> l = clientRepository.getRelationTypes();
        return Multi.createFrom().items(l.stream());
    }

    public void onStart(@Observes StartupEvent ev) {
        transactionEmitter.preProcessing();
    }
}