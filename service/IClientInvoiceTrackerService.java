package com.moeving.fleet.finance.ClientInvoiceTracker.service;

import com.moeving.fleet.common.dto.rest.RestApiResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.AddClientInvoiceRequest;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.PendingInvoiceSearch;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.SearchInvoiceCriteria;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.UpdateClientInvoice;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.InvoiceTrackerResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.PendingInvoiceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


public interface IClientInvoiceTrackerService {
    List<InvoiceTrackerResponse> fetchInvoiceDetails(SearchInvoiceCriteria searchInvoice);

    List<PendingInvoiceResponse> getPendingInvoices(PendingInvoiceSearch pendingInvoiceSearch);

    List<PendingInvoiceResponse> getPendingInvoices();

    void sendMailToPendingInvoice(String invoiceNumber);

    void sendMailToPendingInvoice();

    void addClientInvoice(AddClientInvoiceRequest clientInvoiceRequest);

    void updateInvoiceStatus(UpdateClientInvoice updateClientInvoice);

    RestApiResponse<Object> processExcel(MultipartFile originalFile, int month, int year) throws  IOException;

    String downloadSampleExcel();
}
