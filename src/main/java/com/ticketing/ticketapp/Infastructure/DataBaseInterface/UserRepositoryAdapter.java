package com.ticketing.ticketapp.Infastructure.DataBaseInterface;

import com.ticketing.ticketapp.Domain.User.IUserRepository;
import com.ticketing.ticketapp.Domain.User.Suspension;
import com.ticketing.ticketapp.Domain.User.User;
import com.ticketing.ticketapp.Infastructure.JpaSuspensionRepository;
import com.ticketing.ticketapp.Infastructure.JpaUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@Primary
public class UserRepositoryAdapter implements IUserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final JpaSuspensionRepository jpaSuspensionRepository;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository,
                                 JpaSuspensionRepository jpaSuspensionRepository) {
        this.jpaUserRepository = jpaUserRepository;
        this.jpaSuspensionRepository = jpaSuspensionRepository;
    }

    @Override
    @Transactional
    public User Store(String username, String password, int age, String email) {
        if (usernameExists(username)) {
            throw new RuntimeException("User already exists");
        }
        User user = new User(username, password, age, email);
        return jpaUserRepository.saveAndFlush(user);
    }

    @Override
    public String getUserPassword(String username) {
        return jpaUserRepository.findByName(username)
                .map(User::getPassword)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Override
    public User getUserByUsername(String username) {
        return jpaUserRepository.findByName(username).orElse(null);
    }

    @Override
    public User getUserByID(String ID) {
        return jpaUserRepository.findById(ID).orElse(null);
    }

    @Override
    public String getUsernameByID(String ID) {
        return jpaUserRepository.findById(ID)
                .map(User::getName)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + ID));
    }

    @Override
    public boolean usernameExists(String username) {
        return jpaUserRepository.existsByName(username);
    }

    @Override
    @Transactional
    public void save(User userToUpdate) {
        // @Version on User.version handles optimistic locking automatically
        jpaUserRepository.save(userToUpdate);
    }

    @Override
    @Transactional
    public void deleteAll() {
        jpaSuspensionRepository.deleteAll();
        jpaUserRepository.deleteAll();
        jpaUserRepository.flush();
    }

    @Override
    @Transactional
    public void deleteUser(String ID) {
        jpaUserRepository.deleteById(ID);
        jpaUserRepository.flush();
    }

    @Override
    @Transactional
    public void addCurrentSuspension(String userID, Suspension suspension) {
        suspension.setActive(true);
        jpaSuspensionRepository.saveAndFlush(suspension);
    }

    @Override
    @Transactional
    public void addHistorySuspension(Suspension suspension) {
        suspension.setActive(false);
        jpaSuspensionRepository.saveAndFlush(suspension);
    }

    @Override
    @Transactional
    public boolean isUserSuspendedNow(String userID) {
        return jpaSuspensionRepository.findByUserIDAndIsActiveTrue(userID)
                .map(suspension -> {
                    if (suspension.isPermanent()) return true;
                    if (!suspension.getEndTime().isBefore(LocalDateTime.now())) return true;
                    // Suspension has expired — move it to history
                    suspension.setActive(false);
                    jpaSuspensionRepository.save(suspension);
                    return false;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void cancelSuspension(String userId) {
        Suspension suspension = jpaSuspensionRepository.findByUserIDAndIsActiveTrue(userId)
                .orElseThrow(() -> new RuntimeException("User is not currently suspended"));
        suspension.setEndTime(LocalDateTime.now());
        suspension.setActive(false);
        jpaSuspensionRepository.save(suspension);
    }

    @Override
    public List<Suspension> getAllSuspensions() {
        return jpaSuspensionRepository.findAll();
    }

    @Override
    public Suspension getCurrentSuspensionByUserID(String userID) {
        return jpaSuspensionRepository.findByUserIDAndIsActiveTrue(userID).orElse(null);
    }
}
