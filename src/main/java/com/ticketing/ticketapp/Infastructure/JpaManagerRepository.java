package com.ticketing.ticketapp.Infastructure;

import com.ticketing.ticketapp.Domain.OwnerManagerTree.Manager;
import com.ticketing.ticketapp.Domain.OwnerManagerTree.RoleKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaManagerRepository extends JpaRepository<Manager, RoleKey> {
    List<Manager> findByCompanyName(String companyName);
    List<Manager> findByUserID(String userID);

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
