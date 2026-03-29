package com.kkmalysa.clamtransactions;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClaimService {

    private final EntityManager entityManager;

    public ClaimService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public Claim createClaim(Claim claim) {
        entityManager.persist(claim);
        return claim;
    }

    @Transactional(readOnly = true)
    public List<Claim> findAll() {
        return entityManager
                .createQuery("from Claim", Claim.class)
                .getResultList();
    }

    @Transactional(readOnly = true)
    public Optional<Claim> findByClaimNumber(String claimNumber) {
        return entityManager
                .createQuery("from Claim c where c.claimNumber = :claimNumber", Claim.class)
                .setParameter("claimNumber", claimNumber)
                .getResultList()
                .stream()
                .findAny();
    }

    @Transactional(readOnly = true)
    public Claim readClaim(UUID id) {
        return entityManager.find(Claim.class, id);
    }

    @Transactional
    public Claim updateClaim(Claim claim) {
        entityManager.merge(claim);
        return claim;
    }

    @Transactional
    public void removeAll() {
        entityManager.createQuery("delete from Claim")
                .executeUpdate();
    }
}
