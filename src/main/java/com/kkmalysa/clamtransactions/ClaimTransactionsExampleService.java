package com.kkmalysa.clamtransactions;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ClaimTransactionsExampleService {

    private final ClaimService claimService;

    public ClaimTransactionsExampleService(ClaimService claimService) {
        this.claimService = claimService;
    }

    @Transactional
    public void updateClaimStatusImplicitly(UUID id, ClaimStatus newStatus) {
        Claim claim = claimService.readClaim(id);
        claim.setStatus(newStatus);

        // celowo nie wywołuję:
        // claimService.updateClaim(claim);
    }
}
