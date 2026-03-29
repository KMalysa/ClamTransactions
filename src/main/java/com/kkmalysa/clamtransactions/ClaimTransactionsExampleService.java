package com.kkmalysa.clamtransactions;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
public class ClaimTransactionsExampleService {

    private final ClaimService claimService;

    public ClaimTransactionsExampleService(ClaimService claimService) {
        this.claimService = claimService;
    }

    // this is public! - it works
    @Transactional
    public void updateClaimStatusImplicitly(UUID id, ClaimStatus newStatus) {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);

        // I intentionally don't call:
        // claimService.updateClaim(claim);
        // in a transaction, after reading the entity, setXxx(...) is enough to save the changes.

    }

    // this is NOT public! - it won't work for older spring version
    // so as for now it doesn't have sense, but I leave it to remember that something has changed
    @Transactional
    void updateClaimStatusImplicitlyNonPublic(UUID id, ClaimStatus newStatus) {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);
    }

    // runtime exception → rollback
    @Transactional
    public void updateClaimStatusWithRuntimeException(UUID id, ClaimStatus newStatus) {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);

        throw new ClaimRuntimeException();
    }

    //CheckedException → commit
    @Transactional
    public void updateClaimStatusWithCheckedException(UUID id, ClaimStatus newStatus) throws Exception {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);

        throw new ClaimCheckedException();
    }

    //NoRollbackFor → force commit
    @Transactional(noRollbackFor = ClaimRuntimeException.class)
    public void updateClaimStatusWithNoRollbackFor(UUID id, ClaimStatus newStatus) {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);

        throw new ClaimRuntimeException();
    }

    // rollbackFor → force rollback
    @Transactional(rollbackFor = ClaimCheckedException.class)
    public void updateClaimStatusWithRollbackFor(UUID id, ClaimStatus newStatus) throws Exception {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);

        throw new ClaimCheckedException();
    }

    /**
     *
     * @return true SUPPORTS work inside existing transaction OR false if it has been invoked without transaction
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean isTransactionActiveInSupports() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean isTransactionActiveInMandatory() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean isTransactionActiveInNotSupported() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Transactional(propagation = Propagation.NEVER)
    public boolean isTransactionActiveInNever() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    public static class ClaimRuntimeException extends RuntimeException {
    }

    public static class ClaimCheckedException extends Exception {
    }
}
