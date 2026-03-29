package com.kkmalysa.clamtransactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class TransactionHelper {

    //simple transaction with result
    @Transactional
    public <T> T withTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    //simple transaction without result
    @Transactional
    public void withTransaction(Runnable runnable) {
        runnable.run();
    }

    //read only with result
    @Transactional(readOnly = true)
    public <T> T withReadOnlyTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    //read only without result
    @Transactional(readOnly = true)
    public void withReadOnlyTransaction(Runnable runnable) {
        runnable.run();
    }

    //new transaction with result
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T withNewTransaction(Supplier<T> supplier) {
        return supplier.get();
    }

    //new transaction without result
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void withNewTransaction(Runnable runnable) {
        runnable.run();
    }
}
