package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.Owner;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.RoleKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaOwnerRepository extends JpaRepository<Owner, RoleKey> {
    List<Owner> findByCompanyName(String companyName);
    List<Owner> findByUserID(String userID);

    @Transactional
    @Modifying
    void deleteByCompanyName(String companyName);

    @Transactional
    @Modifying
    void deleteByUserID(String userID);

    @Transactional
    @Modifying
    void deleteByUserIDAndCompanyName(String userID, String companyName);
}
