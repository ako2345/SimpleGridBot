package com.ako2345.simplegridbot.model;

import java.math.BigDecimal;

public class TransactionPair {

    private Transaction openTransaction;

    private Transaction closeTransaction;

    public TransactionPair(Transaction openTransaction) {
        this.openTransaction = openTransaction;
    }

    public Transaction getOpenTransaction() {
        return openTransaction;
    }

    public void setCloseTransaction(Transaction closeTransaction) {
        this.closeTransaction = closeTransaction;
    }

    public boolean isIncomplete() {
        return closeTransaction == null || openTransaction == null;
    }

    public BigDecimal profit() {
        if (isIncomplete()) return BigDecimal.ZERO;
        if (openTransaction.getDirection() == Direction.BUY && closeTransaction.getDirection() == Direction.SELL) {
            return closeTransaction.getBaseCurrencyAmount().subtract(openTransaction.getBaseCurrencyAmount());
        }
        if (openTransaction.getDirection() == Direction.SELL && closeTransaction.getDirection() == Direction.BUY) {
            return openTransaction.getBaseCurrencyAmount().subtract(closeTransaction.getBaseCurrencyAmount());
        }
        throw new RuntimeException("Unexpected direction");
    }

}