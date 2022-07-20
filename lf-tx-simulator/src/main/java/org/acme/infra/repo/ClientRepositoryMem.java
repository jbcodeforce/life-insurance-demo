package org.acme.infra.repo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.acme.domain.ClientCategory;
import org.acme.domain.ClientRelationType;

import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class ClientRepositoryMem implements ClientRepository {
    private static ConcurrentHashMap<Integer,ClientCategory> clientCategories = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer,ClientRelationType> relationTypes = new ConcurrentHashMap<>();

    private static ObjectMapper mapper = new ObjectMapper();
    
    public ClientRepositoryMem(){
        loadClientCategory();
        loadRelationType();
    }

    @Override
    public List<ClientCategory> getListOfCategory() {
        return  new ArrayList<ClientCategory>(clientCategories.values());
    }

    @Override
    public List<ClientRelationType> getRelationTypes() {
        return  new ArrayList<ClientRelationType>(relationTypes.values());
    }
    

    private void loadClientCategory(){
        InputStream is = getClass().getClassLoader().getResourceAsStream("clientCategories.json");
        if (is == null) 
            throw new IllegalAccessError("file not found for clientCategories json");
        try {
            List<ClientCategory> currentDefinitions = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, ClientCategory.class));
            currentDefinitions.stream().forEach( (t) -> { t.updatedTime = new Date().getTime(); clientCategories.put(t.id,t);});
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    private void loadRelationType(){
        InputStream is = getClass().getClassLoader().getResourceAsStream("relationTypes.json");
        if (is == null) 
            throw new IllegalAccessError("file not found for relationTypes json");
        try {
            List<ClientRelationType> currentDefinitions = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, ClientRelationType.class));
            currentDefinitions.stream().forEach( (t) -> relationTypes.put(t.id,t));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ClientCategory saveCategory(ClientCategory cc) {
        if (cc.id == 0 ) {
            cc.id = UUID.randomUUID().hashCode();
        }
        clientCategories.put(cc.id, cc);
        return cc;
    } 
}
