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
import org.acme.infra.repo.ClientRepository;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;


@ApplicationScoped
@Path("/api/v1/clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {
    private static final Logger logger = Logger.getLogger(ClientResource.class.getName());

    @Inject
    MQProducer transactionEmitter; 

    @Channel("categories")
    @Inject
    Emitter<ClientCategory> categoryEventEmitter;
    
    @Inject
    ClientRepository clientRepository;


    @POST
    public Client createNewClient(Client client) {
        logger.info("In createNewClient client: " + client.toString());
         if (client.id == null) {
            UUID uuid = UUID.randomUUID();
            client.id = uuid.toString();
         }
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

    @POST
    @Path("/categories")
    public Uni<ClientCategory> createCategory(ClientCategory cc){
        ClientCategory ncc= clientRepository.saveCategory(cc);
        categoryEventEmitter.send(ncc);
        return Uni.createFrom().item(ncc);
    }

    @PUT
    @Path("/categories")
    public Uni<ClientCategory> updateCategory(ClientCategory cc){
        ClientCategory ncc= clientRepository.saveCategory(cc);
        categoryEventEmitter.send(ncc);
        return Uni.createFrom().item(ncc);
    }

    @GET
    @Path("/relationTypes")
    public Multi<ClientRelationType> getRelationTypes(){
        List<ClientRelationType> l = clientRepository.getRelationTypes();
        return Multi.createFrom().items(l.stream());
    }

    public void onStart(@Observes StartupEvent ev) {
        transactionEmitter.preProcessing();
        for (ClientCategory category : clientRepository.getListOfCategory()) {
            categoryEventEmitter.send(KafkaRecord.of(category.id, category));
        }
        
    }
}