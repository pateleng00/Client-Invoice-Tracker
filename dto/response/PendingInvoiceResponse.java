package com.moeving.fleet.finance.ClientInvoiceTracker.dto.response;

import jakarta.persistence.Tuple;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class PendingInvoiceResponse {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String invoiceStatus;
    private BigDecimal invoiceAmount;
    private String clientName;
    private String clientHubName;
    private BigDecimal paidAmount;
    private LocalDateTime invoicePaidOn;
    private String remark;
    private Integer dueDays;
    private Integer reminderSent;
    private Integer daysDiff;
    private List<String> toEmailAddress;
    private List<String> ccEmailAddress;
    private List<String> bccEmailAddress;

    public static List<PendingInvoiceResponse> from(List<Tuple> clientInvoiceTrackerList) {
        List<PendingInvoiceResponse> response = new ArrayList<>();
        for (Tuple tuple : clientInvoiceTrackerList) {
            response.add(PendingInvoiceResponse.builder()
                    .invoiceNumber((String) tuple.get("invoiceNumber"))
                    .invoiceDate((LocalDate) tuple.get("invoiceDate"))
                    .invoiceStatus((String) tuple.get("invoiceStatus"))
                    .invoiceAmount((BigDecimal) tuple.get("invoiceAmount"))
                    .clientName((String) tuple.get("clientName"))
                    .clientHubName(tuple.get("clientHubName") != null ? (String) tuple.get("clientHubName") : "")
                    .paidAmount(tuple.get("paidAmount") != null ? (BigDecimal) tuple.get("paidAmount") : BigDecimal.valueOf(0.0))
                    .invoicePaidOn((LocalDateTime) tuple.get("invoicePaidOn"))
                    .remark(tuple.get("remark") != null ? (String) tuple.get("remark") : "")
                    .dueDays(tuple.get("dueDays") != null ? (Integer) tuple.get("dueDays") : 0)
                    .reminderSent(tuple.get("reminderSent") != null ? (Integer) tuple.get("reminderSent") : 0)
                    .daysDiff(tuple.get("daysDiff") != null ? (Integer) tuple.get("daysDiff") : 0)
                    .toEmailAddress(tuple.get("toEmailAddress") != null ? Arrays.asList(tuple.get("toEmailAddress").toString().split(",")) : Collections.emptyList())
                    .ccEmailAddress(tuple.get("ccEmailAddress") != null ? Arrays.asList(tuple.get("ccEmailAddress").toString().split(",")) : Collections.emptyList())
                    .bccEmailAddress(tuple.get("bccEmailAddress") != null ? Arrays.asList(tuple.get("bccEmailAddress").toString().split(",")) : Collections.emptyList())
                    .build());
        }
        return response;
    }
}
