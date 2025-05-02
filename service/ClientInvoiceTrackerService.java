package com.moeving.fleet.finance.ClientInvoiceTracker.service;

import com.moeving.fleet.common.communication.CommunicationService;
import com.moeving.fleet.common.config.aws.AwsProperties;
import com.moeving.fleet.common.dto.rest.RestApiResponse;
import com.moeving.fleet.common.dynamicProperties.service.DynamicPropertiesService;
import com.moeving.fleet.common.enums.MasterCategoryDetailEnum;
import com.moeving.fleet.common.exception.FleetException;
import com.moeving.fleet.common.exception.FleetExceptionEnum;
import com.moeving.fleet.common.utils.AwsS3Utils;
import com.moeving.fleet.common.utils.CSVUtils;
import com.moeving.fleet.common.utils.CommonUtils;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.AddClientInvoiceRequest;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.UpdateClientInvoice;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.PendingInvoiceSearch;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.SearchInvoiceCriteria;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.InvoiceTrackerResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.PendingInvoiceResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.entity.ClientInvoiceTracker;
import com.moeving.fleet.finance.ClientInvoiceTracker.entity.ClientInvoiceTrackerReminder;
import com.moeving.fleet.finance.ClientInvoiceTracker.repository.ClientInvoiceTrackerReminderRepository;
import com.moeving.fleet.finance.ClientInvoiceTracker.repository.ClientInvoiceTrackerRepository;
import com.moeving.fleet.finance.ClientInvoiceTracker.repository.IClientInvoiceTrackerCustomRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@AllArgsConstructor
@Slf4j
public class ClientInvoiceTrackerService implements IClientInvoiceTrackerService {
    private final IClientInvoiceTrackerCustomRepository clientInvoiceTracker;
    private final CommunicationService communicationService;
    private final ClientInvoiceTrackerReminderRepository clientInvoiceTrackerReminderRepository;
    private final ClientInvoiceTrackerRepository clientInvoiceTrackerRepository;
    private final DynamicPropertiesService dynamicPropertiesService;
    private final AwsS3Utils awsS3Utils;
    private final AwsProperties awsProperties;


    @Override
    public List<InvoiceTrackerResponse> fetchInvoiceDetails(SearchInvoiceCriteria searchInvoice) {
        return clientInvoiceTracker.fetchInvoiceDetails(searchInvoice);
    }

    @Override
    public List<PendingInvoiceResponse> getPendingInvoices(PendingInvoiceSearch pendingInvoiceSearch) {
        return clientInvoiceTracker.getPendingInvoices(pendingInvoiceSearch);
    }

    @Override
    public List<PendingInvoiceResponse> getPendingInvoices() {
        return clientInvoiceTracker.getPendingInvoices();
    }


    private static @NotNull Map<String, String> getPlaceholderMappingForPendingInvoice(PendingInvoiceResponse pendingInvoiceResponse) {
        LocalDate dueDate = pendingInvoiceResponse.getInvoiceDate().plusDays(pendingInvoiceResponse.getDueDays());
        LocalDate invoiceDate = pendingInvoiceResponse.getInvoiceDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String formattedDueDate = dueDate.format(formatter);
        String formattedInvoiceDate = invoiceDate.format(formatter);
        BigDecimal pendingAmount = pendingInvoiceResponse.getInvoiceAmount().subtract(pendingInvoiceResponse.getPaidAmount());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("invoiceNumber", pendingInvoiceResponse.getInvoiceNumber());
        placeholders.put("invoiceDate", formattedInvoiceDate);
        placeholders.put("dueDate", formattedDueDate);
        placeholders.put("invoiceAmount", pendingInvoiceResponse.getInvoiceAmount().toString());
        placeholders.put("paidAmount", pendingInvoiceResponse.getPaidAmount().toString());
        placeholders.put("pendingAmount", pendingAmount.toString());
        placeholders.put("vendorName", pendingInvoiceResponse.getClientName());
        return placeholders;
    }

    @Override
    public void sendMailToPendingInvoice(String invoiceNumber) {
        List<PendingInvoiceResponse> pendingInvoiceResponseList = clientInvoiceTracker.getPendingInvoices(invoiceNumber);
        log.info("PendingInvoiceResponse {]", pendingInvoiceResponseList);
        if (pendingInvoiceResponseList.isEmpty()){
            throw new FleetException(FleetExceptionEnum.CIT_015);
        }
        try {
            String subject = dynamicPropertiesService.getStringProperty("invoice_tracker_email_subject");
            String body = dynamicPropertiesService.getStringProperty("invoice_tracker_email_body");
            for (PendingInvoiceResponse pendingInvoiceResponse : pendingInvoiceResponseList) {
                Map<String, String> placeholders = getPlaceholderMappingForPendingInvoice(pendingInvoiceResponse);
                String[] emailContent = CommonUtils.getProcessedEmailTemplate(placeholders, subject, body).split("\\|\\|");
                subject = emailContent[0];
                body = emailContent[1];
                ClientInvoiceTrackerReminder clientInvoiceTrackerReminder = new ClientInvoiceTrackerReminder();
                if (pendingInvoiceResponse.getInvoiceNumber().equals(invoiceNumber)) {
                    communicationService.sendMailsToCcAndBcc(pendingInvoiceResponse.getToEmailAddress(),
                            pendingInvoiceResponse.getCcEmailAddress(), pendingInvoiceResponse.getBccEmailAddress(), subject, body);

                    clientInvoiceTrackerReminder.setInvoiceNumber(pendingInvoiceResponse.getInvoiceNumber());
                    clientInvoiceTrackerReminder.setEmailSentTo(pendingInvoiceResponse.getToEmailAddress().toString());
                    clientInvoiceTrackerReminderRepository.save(clientInvoiceTrackerReminder);
                }

                Integer reminderSent = pendingInvoiceResponse.getReminderSent() + 1;
                ClientInvoiceTracker clientInvoiceTracker = new ClientInvoiceTracker();
                clientInvoiceTracker.setReminderSent(reminderSent);
                clientInvoiceTrackerRepository.updateReminderCountByInvoiceNumber(pendingInvoiceResponse.getInvoiceNumber(), reminderSent);
            }
        } catch (Exception e) {
            throw new FleetException(FleetExceptionEnum.CIT_010);
        }
    }


    @Override
    public void sendMailToPendingInvoice() {
        try {
            String subject = dynamicPropertiesService.getStringProperty("invoice_tracker_email_subject");
            String body = dynamicPropertiesService.getStringProperty("invoice_tracker_email_body");
            List<PendingInvoiceResponse> pendingInvoiceResponseList = clientInvoiceTracker.getPendingInvoices();
            for (PendingInvoiceResponse pendingInvoiceResponse : pendingInvoiceResponseList) {
                if (pendingInvoiceResponse.getInvoiceDate().isAfter(LocalDate.now())) {
                    continue;
                }
                Map<String, String> placeholders = getPlaceholderMappingForPendingInvoice(pendingInvoiceResponse);
                String[] emailContent = CommonUtils.getProcessedEmailTemplate(placeholders, subject, body).split("\\|\\|");
                subject = emailContent[0];
                body = emailContent[1];
                ClientInvoiceTrackerReminder clientInvoiceTrackerReminder = new ClientInvoiceTrackerReminder();
                communicationService.sendMailsToCcAndBcc(pendingInvoiceResponse.getToEmailAddress(),
                        pendingInvoiceResponse.getCcEmailAddress(), pendingInvoiceResponse.getBccEmailAddress(), subject, body);

                clientInvoiceTrackerReminder.setInvoiceNumber(pendingInvoiceResponse.getInvoiceNumber());
                clientInvoiceTrackerReminder.setEmailSentTo(pendingInvoiceResponse.getToEmailAddress().toString());
                clientInvoiceTrackerReminderRepository.save(clientInvoiceTrackerReminder);

                Integer reminderSent = pendingInvoiceResponse.getReminderSent() + 1;
                ClientInvoiceTracker clientInvoiceTracker = new ClientInvoiceTracker();
                clientInvoiceTracker.setReminderSent(reminderSent);
                clientInvoiceTrackerRepository.updateReminderCountByInvoiceNumber(pendingInvoiceResponse.getInvoiceNumber(), reminderSent);
            }
        } catch (Exception e) {
            throw new FleetException(FleetExceptionEnum.CIT_010);
        }
    }


    @Override
    @Transactional
    public void addClientInvoice(AddClientInvoiceRequest clientInvoiceRequest) {
        ClientInvoiceTracker clientInvoiceData = clientInvoiceTrackerRepository.findByInvoiceNumber(clientInvoiceRequest.getInvoiceNumber());
        if (Objects.nonNull(clientInvoiceData)) {
            throw new FleetException(FleetExceptionEnum.CIT_002);
        } else {
            try {
                ClientInvoiceTracker clientInvoiceTracker = new ClientInvoiceTracker();
                clientInvoiceTracker.setInvoiceNumber(clientInvoiceRequest.getInvoiceNumber());
                clientInvoiceTracker.setInvoiceDate(clientInvoiceRequest.getInvoiceDate());
                clientInvoiceTracker.setInvoiceAmount(clientInvoiceRequest.getInvoiceAmount());
                clientInvoiceTracker.setDueDays(clientInvoiceRequest.getDueDays());
                clientInvoiceTracker.setClientName(clientInvoiceRequest.getClientName());
                clientInvoiceTracker.setClientHubName(clientInvoiceRequest.getClientHubName());
                clientInvoiceTracker.setRemarks(clientInvoiceRequest.getRemarks() != null ? clientInvoiceRequest.getRemarks() : null);
                clientInvoiceTracker.setToEmailId(clientInvoiceRequest.getToEmailAddress());
                clientInvoiceTracker.setCcEmailId(clientInvoiceRequest.getCcEmailAddress());
                clientInvoiceTracker.setBccEmailId(clientInvoiceRequest.getBccEmailAddress());
                clientInvoiceTracker.setInvoiceStatus(MasterCategoryDetailEnum.Un_Paid.getId());
                clientInvoiceTracker.setInvoiceMonth(clientInvoiceRequest.getInvoiceMonth());
                clientInvoiceTracker.setInvoiceYear(clientInvoiceRequest.getInvoiceYear());
                clientInvoiceTracker.setCity(clientInvoiceRequest.getCityName());
                clientInvoiceTrackerRepository.save(clientInvoiceTracker);
                sendInvoiceGenerationMail(clientInvoiceTracker);
            } catch (Exception e) {
                throw new FleetException(FleetExceptionEnum.F_001);
            }
        }

    }

    public void sendInvoiceGenerationMail(ClientInvoiceTracker clientInvoiceTracker) {
        try {
            String subject = dynamicPropertiesService.getStringProperty("client_invoice_generated_subject");
            String body = dynamicPropertiesService.getStringProperty("client_invoice_generated_body");
            Map<String, String> placeholders = getPlaceholderMappingForGeneratedInvoice(clientInvoiceTracker);
            String[] emailContent = CommonUtils.getProcessedEmailTemplate(placeholders, subject, body).split("\\|\\|");
            subject = emailContent[0];
            body = emailContent[1];
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + body);

            List<String> recipientsTo = List.of(clientInvoiceTracker.getToEmailId().split(","));
            List<String> recipientsCC = List.of(clientInvoiceTracker.getCcEmailId().split(","));
            List<String> recipientsBCC = Collections.emptyList();
            communicationService.sendMailsToCcAndBcc(recipientsTo,
                    recipientsCC, recipientsBCC, subject, body);
        } catch (Exception e) {
            log.info("Error sending mail{}", e.getMessage());
        }
    }

    private static @NotNull Map<String, String> getPlaceholderMappingForGeneratedInvoice(ClientInvoiceTracker generatedResponse) {
        LocalDate dueDate = generatedResponse.getInvoiceDate().plusDays(generatedResponse.getDueDays());
        LocalDate invoiceDate = generatedResponse.getInvoiceDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String formattedDueDate = dueDate.format(formatter);
        String formattedInvoiceDate = invoiceDate.format(formatter);
        Map<String, String> placeholders = new HashMap<>();
        int monthNumber = generatedResponse.getInvoiceMonth();
        String monthName = Month.of(monthNumber).name();
        placeholders.put("invoiceNumber", generatedResponse.getInvoiceNumber());
        placeholders.put("invoiceDate", formattedInvoiceDate);
        placeholders.put("dueDate", formattedDueDate);
        placeholders.put("invoiceAmount", generatedResponse.getInvoiceAmount().toString());
        placeholders.put("vendorName", generatedResponse.getClientName());
        placeholders.put("month", monthName);
        placeholders.put("year", generatedResponse.getInvoiceYear().toString());
        return placeholders;
    }

    @Override
    @Transactional
    public void updateInvoiceStatus(UpdateClientInvoice updateClientInvoice) {
        ClientInvoiceTracker clientInvoiceData = clientInvoiceTrackerRepository.findByInvoiceNumber(updateClientInvoice.getInvoiceNumber());
        if (Objects.isNull(clientInvoiceData)) {
            throw new FleetException(FleetExceptionEnum.CIT_001);
        }
        if(clientInvoiceData.getInvoiceStatus().equals(MasterCategoryDetailEnum.Rejected.getId()) || clientInvoiceData.getInvoiceStatus().equals(MasterCategoryDetailEnum.Deactivated.getId())){
            throw new FleetException(FleetExceptionEnum.CIT_014);
        }

        if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Fully_Paid.getType())) {
            clientInvoiceData.setInvoiceNumber(updateClientInvoice.getInvoiceNumber());
            clientInvoiceData.setPaidAmount(clientInvoiceData.getInvoiceAmount());
            clientInvoiceData.setInvoicePaidOn(LocalDateTime.now());
            clientInvoiceData.setInvoiceStatus(MasterCategoryDetailEnum.Fully_Paid.getId());
            clientInvoiceData.setRemarks(updateClientInvoice.getInvoiceRemarks() != null ? updateClientInvoice.getInvoiceRemarks() : null);
        }

        BigDecimal remainingAmount = clientInvoiceData.getInvoiceAmount().subtract(clientInvoiceData.getPaidAmount() != null ? clientInvoiceData.getPaidAmount() : BigDecimal.valueOf(0.0));

        if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Fully_Paid.getType())){
                if(clientInvoiceData.getInvoiceStatus().equals(MasterCategoryDetailEnum.Partial_Paid.getId()) &&
                        updateClientInvoice.getPaidAmount().compareTo(remainingAmount) > 0){
                    throw new FleetException(FleetExceptionEnum.CIT_012);
                }
                if (clientInvoiceData.getInvoiceStatus().equals(MasterCategoryDetailEnum.Un_Paid.getId()) &&
                        updateClientInvoice.getPaidAmount().compareTo(clientInvoiceData.getInvoiceAmount()) != 0) {
                    throw new FleetException(FleetExceptionEnum.CIT_013);
                }
                clientInvoiceData.setInvoiceNumber(updateClientInvoice.getInvoiceNumber());
                clientInvoiceData.setPaidAmount(clientInvoiceData.getInvoiceAmount());
                clientInvoiceData.setInvoicePaidOn(LocalDateTime.now());
                clientInvoiceData.setInvoiceStatus(MasterCategoryDetailEnum.Fully_Paid.getId());
                clientInvoiceData.setRemarks(updateClientInvoice.getInvoiceRemarks() != null ? updateClientInvoice.getInvoiceRemarks() : null);
        }
        if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Partial_Paid.getType())
                && updateClientInvoice.getPaidAmount().compareTo(clientInvoiceData.getInvoiceAmount()) >= 0) {
            throw new FleetException(FleetExceptionEnum.CIT_008);
        } else if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Partial_Paid.getType())
                && updateClientInvoice.getPaidAmount().compareTo(remainingAmount) >= 0) {
            throw new FleetException(FleetExceptionEnum.CIT_011);
        } else if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Fully_Paid.getType())
                && updateClientInvoice.getPaidAmount().compareTo(remainingAmount) > 0) {
            throw new FleetException(FleetExceptionEnum.CIT_012);
        } else {
            clientInvoiceData.setInvoiceNumber(updateClientInvoice.getInvoiceNumber());
            clientInvoiceData.setPaidAmount(updateClientInvoice.getPaidAmount());
            clientInvoiceData.setInvoiceStatus(MasterCategoryDetailEnum.Partial_Paid.getId());
            clientInvoiceData.setInvoicePaidOn(LocalDateTime.now());
            clientInvoiceData.setRemarks(updateClientInvoice.getInvoiceRemarks() != null ? updateClientInvoice.getInvoiceRemarks() : null);
        }
        if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Rejected.getType())) {
            clientInvoiceData.setInvoiceNumber(updateClientInvoice.getInvoiceNumber());
            clientInvoiceData.setInvoiceStatus(MasterCategoryDetailEnum.Rejected.getId());
            clientInvoiceData.setRemarks(updateClientInvoice.getInvoiceRemarks() != null ? updateClientInvoice.getInvoiceRemarks() : null);
        }

        if (updateClientInvoice.getInvoiceStatus().equals(MasterCategoryDetailEnum.Deactivated.getType())) {
            clientInvoiceData.setInvoiceNumber(updateClientInvoice.getInvoiceNumber());
            clientInvoiceData.setInvoiceStatus(MasterCategoryDetailEnum.Deactivated.getId());
            clientInvoiceData.setRemarks(updateClientInvoice.getInvoiceRemarks() != null ? updateClientInvoice.getInvoiceRemarks() : null);
        }

        clientInvoiceTrackerRepository.save(clientInvoiceData);

    }

    public LocalDate getLocalDate(Date date) {
        if (date == null) {
            return null;
        } else {
            long timeStamp = date.toInstant().toEpochMilli();
            return new Date(timeStamp).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
    }


    public BigDecimal getDouble(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        } else {
            return new BigDecimal(value);
        }
    }


    public boolean blankRow(XSSFRow row) {
        try {
            for (int i = 0; i < 44; i++) {
                if (row.getCell(i).getRawValue() != null)
                    return false;
            }
        } catch (Exception e) {
            int i = 0;
        }
        return true;
    }


    @Override
    public RestApiResponse<Object> processExcel(MultipartFile originalFile, int month, int year) throws IOException {
        List<ClientInvoiceTracker> clientInvoiceTrackerList = new ArrayList<>();
        XSSFWorkbook workbook = new XSSFWorkbook(originalFile.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);

        XSSFRow rowError = worksheet.getRow(0);

        rowError.createCell(11).setCellValue("Error");

        List<XSSFRow> deleteRows = new ArrayList<>();
        for (int index = 1; index < worksheet.getPhysicalNumberOfRows(); index++) {
            XSSFRow row = worksheet.getRow(index);
            if (blankRow(row)) {
                worksheet.removeRow(row);
                continue;
            }
            ClientInvoiceTracker clientInvoiceTracker = new ClientInvoiceTracker();
            try {
                clientInvoiceTracker.setInvoiceNumber(row.getCell(0).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setInvoiceDate(getLocalDate(row.getCell(1).getDateCellValue()));
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setClientName(row.getCell(2).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setClientHubName(row.getCell(3).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setInvoiceAmount(getDouble(row.getCell(4).getRawValue()));
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setRemarks(row.getCell(5).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }


            try {
                clientInvoiceTracker.setToEmailId(row.getCell(6).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setCcEmailId(row.getCell(7).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }


            try {
                clientInvoiceTracker.setDueDays(Integer.valueOf(row.getCell(8).getRawValue()));
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setInvoiceMonth(Short.valueOf(row.getCell(9).getRawValue()));
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setInvoiceYear(Integer.valueOf(row.getCell(10).getRawValue()));
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            try {
                clientInvoiceTracker.setCity(row.getCell(11).getStringCellValue());
            } catch (Exception e) {
                row.createCell(11).setCellValue(e.getMessage());
                continue;
            }

            clientInvoiceTracker.setInvoiceStatus(MasterCategoryDetailEnum.Un_Paid.getId());
            clientInvoiceTrackerList.add(clientInvoiceTracker);
            deleteRows.add(row);

        }
        for (Row row : deleteRows) {
            worksheet.removeRow(row);
        }


        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (bos) {
            workbook.write(bos);
        }
        byte[] bytes = bos.toByteArray();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                generalProcessFlow(clientInvoiceTrackerList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return RestApiResponse.buildSuccess("Excel Processed Successfully", null, bytes);

    }

    @Override
    public String downloadSampleExcel() {
        String fileName = "invoice_tracker/client_invoice_tracker_sample_file.xlsx";
        return awsS3Utils.getPreSignedDownloadUrl(awsProperties.getCommonBucketName(), fileName, 20);
    }

    public void generalProcessFlow(List<ClientInvoiceTracker> clientInvoiceTrackerList) throws Exception {

        List<String> duplicateInvoicesFound = processInvoiceCells(clientInvoiceTrackerList);
        log.info("Payout Cells Processed");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet worksheet = workbook.createSheet("Rejected Payout Cells");
        int rownum = 0;
        Row row = worksheet.createRow(rownum++);
        row.createCell(0).setCellValue("Duplicate Invoice Number");


        for (String duplicate : duplicateInvoicesFound) {
            row = worksheet.createRow(rownum++);
            row.createCell(0).setCellValue(duplicate);

        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (bos) {
            workbook.write(bos);
        }
        byte[] bytes = bos.toByteArray();

        MultipartFile duplicateInvoices =
                new CSVUtils.MultipartFileImpl("Duplicate_Invoices.xlsx",
                        bytes);

        awsS3Utils.uploadFile(awsProperties.getTmpBucketName(), duplicateInvoices);
        String url =
                awsS3Utils.getPreSignedDownloadUrl(awsProperties.getTmpBucketName(), duplicateInvoices.getName(), 60 * 24 * 6);
        String addresses = dynamicPropertiesService.getStringProperty(
                "duplicate_client_invoice_mails");
        String subject = "Duplicate Client Invoice Tracker List";

        List<String> addressList = List.of(addresses.split(","));
        communicationService.sendMail(addressList, subject, url);


    }

    public List<String> processInvoiceCells(List<ClientInvoiceTracker> clientInvoiceTrackerList) {
        int successfullySaved = 0;
        int invoiceDuplicate = 0;
        List<String> duplicateInvoices = new ArrayList<>();
        for (ClientInvoiceTracker clientInvoiceTrackerObject : clientInvoiceTrackerList) {
            ClientInvoiceTracker clientInvoiceFound = clientInvoiceTrackerRepository.findByInvoiceNumber(clientInvoiceTrackerObject.getInvoiceNumber());
            if (clientInvoiceFound == null) {
                successfullySaved++;
                clientInvoiceTrackerRepository.save(clientInvoiceTrackerObject);
            } else if (clientInvoiceTrackerObject.getInvoiceNumber().equals(clientInvoiceFound.getInvoiceNumber())) {
                invoiceDuplicate++;
                duplicateInvoices.add(clientInvoiceTrackerObject.getInvoiceNumber());
            }

        }
        log.info("Total Invoice Processed: {}, Successfully Inserted {}, Duplicate invoices {} ", clientInvoiceTrackerList.size(), successfullySaved, invoiceDuplicate);
        return duplicateInvoices;
    }
}

