package com.kkmalysa.clamtransactions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ClaimTransactionsTest {

    @Autowired
    private ClaimTransactionsExampleService claimTransactionsExampleService;

    @Autowired
    private ClaimService claimService;

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
}