package Infastructure;

import Domain.OwnerManagerTree.Manager;
import Domain.OwnerManagerTree.Owner;
import Domain.OwnerManagerTree.Permission;
import Domain.OwnerManagerTree.iTreeOfRoleRepository;

import java.util.*;
import java.util.stream.Collectors;

public class TreeOfRoleRepositoryImpl implements iTreeOfRoleRepository {
    private Map<String, Owner> owners=new HashMap<String,Owner>();
    private Map<String, Manager> managers=new HashMap<String,Manager>();

    @Override
    public void storeOwner(String owner, String company, String appointer) {
        Owner o=new Owner(owner,company,appointer);
        if(appointer.equals("Administrator"))
        {
            o.acceptAppointment();
        }
        owners.put(owner+company,o);
    }



}
