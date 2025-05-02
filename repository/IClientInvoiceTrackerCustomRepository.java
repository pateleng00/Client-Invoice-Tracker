package com.moeving.fleet.finance.ClientInvoiceTracker.repository;

import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.PendingInvoiceSearch;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.SearchInvoiceCriteria;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.InvoiceTrackerResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.PendingInvoiceResponse;

import java.util.List;
import java.util.Optional;

public interface IClientInvoiceTrackerCustomRepository {
    List<InvoiceTrackerResponse> fetchInvoiceDetails(SearchInvoiceCriteria searchInvoice);

    List<PendingInvoiceResponse> getPendingInvoices(PendingInvoiceSearch pendingInvoices);

    List<PendingInvoiceResponse> getPendingInvoices();

    List<PendingInvoiceResponse> getPendingInvoices(String invoiceNumber);
}
