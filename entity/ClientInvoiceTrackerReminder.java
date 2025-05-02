package com.moeving.fleet.finance.ClientInvoiceTracker.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "client_invoice_tracker_reminder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientInvoiceTrackerReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_number", referencedColumnName = "invoice_number", updatable = false, insertable = false)
    private ClientInvoiceTracker clientInvoiceTracker;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "email_sent_to", length = 255)
    private String emailSentTo;
}
