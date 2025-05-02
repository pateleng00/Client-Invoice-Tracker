package com.moeving.fleet.finance.ClientInvoiceTracker.dto.request;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class UpdateClientInvoice {
    private String invoiceNumber;
    private String invoiceStatus;
    private BigDecimal paidAmount;
    private String invoiceRemarks;
}
