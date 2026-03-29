package com.kkmalysa.clamtransactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ClaimTransactionsTest {

    @Autowired
    private ClaimTransactionsExampleService claimTransactionsExampleService;

    @Autowired
    private ClaimService claimService;

    @Autowired
    private TransactionHelper transactionHelper;

    private UUID claimId;

    @BeforeEach
    void setUp() {
        claimService.removeAll();

        Claim claim = new Claim(
                "CLM-001",
                "Broken windshield",
                ClaimStatus.NEW
        );

        claimId = claim.getId(); //new
        claimService.createClaim(claim); //new
    }

    @Test
    void shouldCommitChangeForPublicTransactionalMethod() {
        // when
        claimTransactionsExampleService.updateClaimStatusImplicitly(claimId, ClaimStatus.IN_REVIEW);

        // then
        Optional<Claim> updatedClaim = claimService.findByClaimNumber("CLM-001");

        assertThat(updatedClaim).isPresent(); //in_review
        assertThat(updatedClaim.get().getStatus()).isEqualTo(ClaimStatus.IN_REVIEW); //in_review
    }

    /** starting from Spring 6.0 protected and package-private methods could be transactional for class-based proxies by default.
    * it shouldn't work for older versions, but here works.
    **/
    @Test
    void shouldNotCommitChangeForNonPublicTransactionalMethod() {
        // when
        claimTransactionsExampleService.updateClaimStatusImplicitlyNonPublic(claimId, ClaimStatus.IN_REVIEW);

        // then
        Optional<Claim> claimAfterUpdateAttempt = claimService.findByClaimNumber("CLM-001");

        assertThat(claimAfterUpdateAttempt).isPresent();
//        assertThat(claimAfterUpdateAttempt.get().getStatus()).isEqualTo(ClaimStatus.NEW);
        assertThat(claimAfterUpdateAttempt.get().getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
    }

    @Test
    void shouldRollbackOnRuntimeException() {
        // when
        try {
            claimTransactionsExampleService
                    .updateClaimStatusWithRuntimeException(claimId, ClaimStatus.IN_REVIEW);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }

        // then
        Optional<Claim> claimAfterUpdateAttempt = claimService.findByClaimNumber("CLM-001");

        assertThat(claimAfterUpdateAttempt).isPresent();
        assertThat(claimAfterUpdateAttempt.get().getStatus()).isEqualTo(ClaimStatus.NEW);
    }

    @Test
    void shouldNotRollbackOnCheckedException() {
        // when
        try {
            claimTransactionsExampleService
                    .updateClaimStatusWithCheckedException(claimId, ClaimStatus.IN_REVIEW);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // then
        Optional<Claim> updatedClaim = claimService.findByClaimNumber("CLM-001");

        assertThat(updatedClaim).isPresent();
        assertThat(updatedClaim.get().getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
    }

    @Test
    void shouldNotRollbackWhenNoRollbackForIsConfigured() {
        // when
        try {
            claimTransactionsExampleService
                    .updateClaimStatusWithNoRollbackFor(claimId, ClaimStatus.IN_REVIEW);
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }

        // then
        Optional<Claim> updatedClaim = claimService.findByClaimNumber("CLM-001");

        assertThat(updatedClaim).isPresent();
        assertThat(updatedClaim.get().getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
    }

    @Test
    void shouldRollbackWhenRollbackForIsConfigured() {
        // when
        try {
            claimTransactionsExampleService
                    .updateClaimStatusWithRollbackFor(claimId, ClaimStatus.IN_REVIEW);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // then
        Optional<Claim> claimAfterUpdateAttempt = claimService.findByClaimNumber("CLM-001");

        assertThat(claimAfterUpdateAttempt).isPresent();
        assertThat(claimAfterUpdateAttempt.get().getStatus()).isEqualTo(ClaimStatus.NEW);
    }

    @Test
    void shouldUseReadOnlyTransaction() {
        // when
        Claim claim = transactionHelper.withReadOnlyTransaction(() -> claimService.readClaim(claimId));

        // then
        assertThat(claim).isNotNull();
        assertThat(claim.getClaimNumber()).isEqualTo("CLM-001");
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.NEW);
    }

    @Test
    void shouldNotPersistChangesInsideReadOnlyTransaction() {
        // when
        transactionHelper.withReadOnlyTransaction(() -> {
            Claim claim = claimService.readClaim(claimId);
            claim.setStatus(ClaimStatus.IN_REVIEW);
            claimService.updateClaim(claim);
        });

        // then
        Optional<Claim> claimAfterAttempt = claimService.findByClaimNumber("CLM-001");

        assertThat(claimAfterAttempt).isPresent();
        assertThat(claimAfterAttempt.get().getStatus()).isEqualTo(ClaimStatus.NEW);
    }

    @Test
    void shouldUseRequiredPropagationInTheSameTransaction() {
        // when
        transactionHelper.withTransaction(() -> {
            claimTransactionsExampleService.updateClaimStatusImplicitly(claimId, ClaimStatus.IN_REVIEW);
            claimTransactionsExampleService.updateClaimStatusImplicitly(claimId, ClaimStatus.APPROVED);
        });

        // then
        Optional<Claim> updatedClaim = claimService.findByClaimNumber("CLM-001");

        assertThat(updatedClaim).isPresent();
        assertThat(updatedClaim.get().getStatus()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void shouldRollbackAllChangesInsideRequiredPropagation() {
        // when
        try {
            transactionHelper.withTransaction(() -> {
                claimTransactionsExampleService.updateClaimStatusImplicitly(claimId, ClaimStatus.IN_REVIEW);
                claimTransactionsExampleService.updateClaimStatusWithRuntimeException(claimId, ClaimStatus.APPROVED);
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // then
        Optional<Claim> claimAfterRollback = claimService.findByClaimNumber("CLM-001");

        assertThat(claimAfterRollback).isPresent();
        assertThat(claimAfterRollback.get().getStatus()).isEqualTo(ClaimStatus.NEW);
    }

    @Test
    void shouldNotRollbackChangesFromDifferentRequiresNewTransactions() {
        // when
        try {
            transactionHelper.withNewTransaction(() -> {
                claimTransactionsExampleService.updateClaimStatusImplicitly(claimId, ClaimStatus.IN_REVIEW);
            });

            transactionHelper.withNewTransaction(() -> {
                claimTransactionsExampleService.updateClaimStatusWithRuntimeException(claimId, ClaimStatus.APPROVED);
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // then
        Optional<Claim> updatedClaim = claimService.findByClaimNumber("CLM-001");

        assertThat(updatedClaim).isPresent();
        assertThat(updatedClaim.get().getStatus()).isEqualTo(ClaimStatus.IN_REVIEW);
    }

    @Test
    void shouldWorkWithoutTransactionForSupports() {
        // when
        boolean transactionActive = claimTransactionsExampleService.isTransactionActiveInSupports();

        // then
        assertThat(transactionActive).isFalse();
    }

    @Test
    void shouldJoinExistingTransactionForSupports() {
        // when
        boolean transactionActive = transactionHelper.withTransaction(
                () -> claimTransactionsExampleService.isTransactionActiveInSupports()
        );

        // then
        assertThat(transactionActive).isTrue();
    }

    @Test
    void shouldFailWithoutExistingTransactionForMandatory() {
        assertThatThrownBy(() -> claimTransactionsExampleService.isTransactionActiveInMandatory())
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void shouldUseExistingTransactionForMandatory() {
        boolean transactionActive = transactionHelper.withTransaction(
                () -> claimTransactionsExampleService.isTransactionActiveInMandatory()
        );

        assertThat(transactionActive).isTrue();
    }

    @Test
    void shouldWorkWithoutTransactionForNotSupported() {
        boolean transactionActive = claimTransactionsExampleService.isTransactionActiveInNotSupported();

        assertThat(transactionActive).isFalse();
    }

    @Test
    void shouldSuspendExistingTransactionForNotSupported() {
        boolean transactionActive = transactionHelper.withTransaction(
                () -> claimTransactionsExampleService.isTransactionActiveInNotSupported()
        );

        assertThat(transactionActive).isFalse();
    }

    @Test
    void shouldWorkWithoutTransactionForNever() {
        boolean transactionActive = claimTransactionsExampleService.isTransactionActiveInNever();

        assertThat(transactionActive).isFalse();
    }

    @Test
    void shouldFailWhenTransactionExistsForNever() {
        assertThatThrownBy(() ->
                transactionHelper.withTransaction(
                        () -> claimTransactionsExampleService.isTransactionActiveInNever()
                )
        ).isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

}