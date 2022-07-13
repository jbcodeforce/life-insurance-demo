package org.acme.infra.repo;

import java.util.List;

import org.acme.domain.ClientCategory;
import org.acme.domain.ClientRelationType;

public interface ClientRepository {
    
    public List<ClientCategory> getListOfCategory();

    public List<ClientRelationType> getRelationTypes();
}
