package com.moeving.fleet.finance.ClientInvoiceTracker.dto.request;


import lombok.Data;

@Data
public class SearchInvoiceCriteria {
    private String clientName;
    private String clientHubName;
    private String invoiceNumber;
    private String status;
}
