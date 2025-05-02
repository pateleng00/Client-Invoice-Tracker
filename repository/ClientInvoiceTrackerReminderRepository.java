package com.moeving.fleet.finance.ClientInvoiceTracker.repository;


import com.moeving.fleet.finance.ClientInvoiceTracker.entity.ClientInvoiceTrackerReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientInvoiceTrackerReminderRepository extends JpaRepository<ClientInvoiceTrackerReminder, Long> {

    List<ClientInvoiceTrackerReminder> findByInvoiceNumber(String invoiceNumber);
}
