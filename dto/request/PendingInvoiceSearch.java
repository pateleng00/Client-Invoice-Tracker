package com.moeving.fleet.finance.ClientInvoiceTracker.dto.request;

import lombok.Data;

import java.util.Optional;

@Data
public class PendingInvoiceSearch {
    private String clientName;
    private Optional<Integer> month;
    private Optional<Integer> year;
}
