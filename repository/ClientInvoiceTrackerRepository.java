package com.moeving.fleet.finance.ClientInvoiceTracker.repository;

import com.moeving.fleet.finance.ClientInvoiceTracker.entity.ClientInvoiceTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ClientInvoiceTrackerRepository extends JpaRepository<ClientInvoiceTracker, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE ClientInvoiceTracker c SET c.reminderSent = :count WHERE c.invoiceNumber = :invoiceNumber")
    int updateReminderCountByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber,
                                           @Param("count") Integer count);

    ClientInvoiceTracker findByInvoiceNumber(String invoiceNumber);
}
