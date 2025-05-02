package com.moeving.fleet.finance.ClientInvoiceTracker.dto.response;

import jakarta.persistence.Tuple;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class InvoiceTrackerResponse {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String invoiceStatus;
    private BigDecimal invoiceAmount;
    private String clientName;
    private String clientHubName;
    private BigDecimal paidAmount;
    private LocalDateTime invoicePaidOn;
    private String remark;



    public static List<InvoiceTrackerResponse> from(List<Tuple> clientInvoiceTrackerList) {
        List<InvoiceTrackerResponse> response = new ArrayList<>();
        for (Tuple tuple : clientInvoiceTrackerList) {
            response.add(InvoiceTrackerResponse.builder()
                    .invoiceNumber((String) tuple.get("invoiceNumber"))
                    .invoiceDate((LocalDate) tuple.get("invoiceDate"))
                    .invoiceStatus((String) tuple.get("invoiceStatus"))
                    .invoiceAmount((BigDecimal) tuple.get("invoiceAmount"))
                    .clientName((String) tuple.get("clientName"))
                    .clientHubName(tuple.get("clientHubName") != null ? (String) tuple.get("clientHubName") : "")
                    .paidAmount(tuple.get("paidAmount") != null ? (BigDecimal) tuple.get("paidAmount") : BigDecimal.valueOf(0.0))
                    .invoicePaidOn((LocalDateTime) tuple.get("invoicePaidOn"))
                    .remark(tuple.get("remark") != null ? (String) tuple.get("remark") : "")
                    .build());
        }
        return response;
    }
}
