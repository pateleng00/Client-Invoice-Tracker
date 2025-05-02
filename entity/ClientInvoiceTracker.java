package com.moeving.fleet.finance.ClientInvoiceTracker.entity;

import com.moeving.fleet.common.model.entity.LegacyEntityMetaData;
import com.moeving.fleet.common.services.category.entity.MasterCategoryDetail;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_invoice_tracker")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ClientInvoiceTracker extends LegacyEntityMetaData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;


    @Column(name = "client_name", length = 255)
    private String clientName;


    @Column(name = "client_hub_name", length = 255)
    private String clientHubName;

    @Column(name = "invoice_amount", nullable = false)
    private BigDecimal invoiceAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "invoice_paid_on")
    private LocalDateTime invoicePaidOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_status", referencedColumnName = "id", updatable = false, insertable = false)
    private MasterCategoryDetail masterCategoryDetail;

    @Column(name = "invoice_status")
    private Short invoiceStatus;


    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "to_email_id", length = 255, columnDefinition = "TEXT")
    private String toEmailId;

    @Column(name = "cc_email_id", length = 255, columnDefinition = "TEXT")
    private String ccEmailId;

    @Column(name = "bcc_email_id", length = 255, columnDefinition = "TEXT")
    private String bccEmailId;

    @Column(name = "due_days")
    private Integer dueDays;

    @Column(name = "reminder_sent")
    private Integer reminderSent;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "invoice_month")
    private Short invoiceMonth;

    @Column(name = "invoice_year")
    private Integer invoiceYear;

}
