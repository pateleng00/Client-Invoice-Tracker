package com.moeving.fleet.finance.ClientInvoiceTracker.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class AddClientInvoiceRequest {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String clientName;
    private String clientHubName;
    private BigDecimal invoiceAmount;
    private Integer invoiceStatus;
    private String remarks;
    private String toEmailAddress;
    private String ccEmailAddress;
    private String bccEmailAddress;
    private Integer dueDays;
    private String cityName;
    private Short invoiceMonth;
    private Integer invoiceYear;

}
