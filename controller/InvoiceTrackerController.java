package com.moeving.fleet.finance.ClientInvoiceTracker.controller;


import com.moeving.fleet.common.dto.rest.RestApiResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.AddClientInvoiceRequest;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.PendingInvoiceSearch;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.SearchInvoiceCriteria;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.UpdateClientInvoice;
import com.moeving.fleet.finance.ClientInvoiceTracker.service.IClientInvoiceTrackerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "finance/invoice-tracker")
@AllArgsConstructor
@Slf4j
public class InvoiceTrackerController {
    private final IClientInvoiceTrackerService invoiceTrackerService;

    @PostMapping(value = "/fetch-invoice-details")
    public RestApiResponse<Object> fetchInvoiceDetails(@RequestBody SearchInvoiceCriteria searchInvoice) {
        return RestApiResponse.buildSuccess(invoiceTrackerService.fetchInvoiceDetails(searchInvoice));
    }


    @GetMapping(value = "/get-pending-invoices")
    public RestApiResponse<Object> getPendingInvoices(@RequestBody PendingInvoiceSearch pendingInvoiceSearch) {
        return RestApiResponse.buildSuccess(invoiceTrackerService.getPendingInvoices(pendingInvoiceSearch));
    }

    @GetMapping(value = "/get-all-pending-invoices")
    public RestApiResponse<Object> getPendingInvoices() {
        return RestApiResponse.buildSuccess(invoiceTrackerService.getPendingInvoices());
    }

    @PostMapping(value = "/send-reminder-by-invoice")
    public RestApiResponse<Object> sendMailToPendingInvoice(@RequestParam String invoiceNumber) {
        invoiceTrackerService.sendMailToPendingInvoice(invoiceNumber);
        return RestApiResponse.buildSuccess("Email sent to Client's Pending Invoice");
    }


    @PostMapping(value = "/send-all-pending-invoice-reminders")
    public RestApiResponse<Object> sendMailToAllPendingInvoice() {
        invoiceTrackerService.sendMailToPendingInvoice();
        return RestApiResponse.buildSuccess("Sending Email to Client's Pending Invoices");
    }

    @PostMapping(value = "/add-client-invoice")
    public RestApiResponse<Object> addClientInvoice(@RequestBody AddClientInvoiceRequest clientInvoiceRequest) {
        invoiceTrackerService.addClientInvoice(clientInvoiceRequest);
        return RestApiResponse.buildSuccess("Client Invoice Added Successfully");

    }

    @PutMapping(value = "/update-invoice-status")
    public RestApiResponse<Object> updateInvoiceStatus(@RequestBody UpdateClientInvoice updateClientInvoice) {
        invoiceTrackerService.updateInvoiceStatus(updateClientInvoice);
        return RestApiResponse.buildSuccess("Invoice Status Updated Successfully");
    }

    @PostMapping("/upload-client-invoice-sheet")
    public RestApiResponse<Object> createClientInvoice(@RequestParam("file") MultipartFile file, @RequestParam Integer month, @RequestParam Integer year) throws IOException {
        return invoiceTrackerService.processExcel(file, month, year);
    }

    @GetMapping("/download-sample-excel-sheet")
    public RestApiResponse<Object> downloadSampleExcel(){
        return RestApiResponse.buildSuccess(invoiceTrackerService.downloadSampleExcel());
    }

}
