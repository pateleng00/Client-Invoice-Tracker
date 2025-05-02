package com.moeving.fleet.finance.ClientInvoiceTracker.repository;


import com.moeving.fleet.common.enums.MasterCategoryDetailEnum;
import com.moeving.fleet.common.services.category.entity.MasterCategoryDetail;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.PendingInvoiceSearch;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.request.SearchInvoiceCriteria;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.InvoiceTrackerResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.dto.response.PendingInvoiceResponse;
import com.moeving.fleet.finance.ClientInvoiceTracker.entity.ClientInvoiceTracker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
@AllArgsConstructor
public class ClientInvoiceTrackerCustomRepository implements IClientInvoiceTrackerCustomRepository {
    private EntityManager entityManager;

    public List<InvoiceTrackerResponse> fetchInvoiceDetails(SearchInvoiceCriteria searchInvoice) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ClientInvoiceTracker> root = query.from(ClientInvoiceTracker.class);
        Join<ClientInvoiceTracker, MasterCategoryDetail> masterCategoryDetailJoin = root.join("masterCategoryDetail");
//        Join<ClientInvoiceTracker, Client> clientJoin = root.join("client");
//        Join<ClientInvoiceTracker, ClientReportingLocation> clientHubJoin = root.join("clientHub", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(searchInvoice.getClientName())) {
            predicates.add(cb.like(root.get("clientName"), searchInvoice.getClientName()));
        }
        if (Objects.nonNull(searchInvoice.getInvoiceNumber())) {
            predicates.add(cb.equal(root.get("invoiceNumber"), searchInvoice.getInvoiceNumber()));
        }
        if (Objects.nonNull(searchInvoice.getClientHubName())) {
            predicates.add(cb.like(root.get("clientHubName"), searchInvoice.getClientHubName()));
        }
        if (Objects.nonNull(searchInvoice.getStatus())) {
            predicates.add(cb.equal(root.get("invoiceStatus"), searchInvoice.getStatus()));
        }

        query.multiselect(
                root.get("invoiceNumber").alias("invoiceNumber"),
                root.get("invoiceAmount").alias("invoiceAmount"),
                root.get("invoiceDate").alias("invoiceDate"),
                root.get("paidAmount").alias("paidAmount"),
                root.get("invoicePaidOn").alias("invoicePaidOn"),
                root.get("clientName").alias("clientName"),
                root.get("clientHubName").alias("clientHubName"),
                masterCategoryDetailJoin.get("name").alias("invoiceStatus"),
                root.get("remarks").alias("remark")
        ).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

        List<Tuple> results = typedQuery.getResultList();

        return InvoiceTrackerResponse.from(results);
    }

    @Override
    public List<PendingInvoiceResponse> getPendingInvoices(PendingInvoiceSearch pendingInvoiceSearch) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ClientInvoiceTracker> root = query.from(ClientInvoiceTracker.class);
        Join<ClientInvoiceTracker, MasterCategoryDetail> masterCategoryDetailJoin = root.join("masterCategoryDetail");

        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(pendingInvoiceSearch.getClientName())) {
            predicates.add(cb.like(root.get("clientName"), pendingInvoiceSearch.getClientName()));
        }
        if (pendingInvoiceSearch.getMonth().isPresent()) {
            predicates.add(cb.equal(
                    cb.function("MONTH", Integer.class, root.get("invoiceDate")),
                    pendingInvoiceSearch.getMonth().get()
            ));
        }
        if (pendingInvoiceSearch.getYear().isPresent()) {
            predicates.add(cb.equal(
                    cb.function("YEAR", Integer.class, root.get("invoiceDate")),
                    pendingInvoiceSearch.getYear().get()
            ));
        }
        predicates.add(cb.greaterThan(cb.function("DATEDIFF", Integer.class, cb.currentDate(), root.get("invoiceDate")), root.get("dueDays")));
        predicates.add(root.get("invoiceStatus").in(MasterCategoryDetailEnum.Un_Paid.getId(), MasterCategoryDetailEnum.Partial_Paid.getId()));
        query.multiselect(
                root.get("invoiceNumber").alias("invoiceNumber"),
                root.get("invoiceAmount").alias("invoiceAmount"),
                root.get("invoiceDate").alias("invoiceDate"),
                root.get("paidAmount").alias("paidAmount"),
                root.get("invoicePaidOn").alias("invoicePaidOn"),
                root.get("toEmailId").alias("toEmailAddress"),
                root.get("ccEmailId").alias("ccEmailAddress"),
                root.get("bccEmailId").alias("bccEmailAddress"),
                root.get("clientName").alias("clientName"),
                root.get("clientHubName").alias("clientHubName"),
                masterCategoryDetailJoin.get("name").alias("invoiceStatus"),
                root.get("remarks").alias("remark"),
                root.get("dueDays").alias("dueDays"),
                root.get("reminderSent").alias("reminderSent"),
                cb.function("DATEDIFF", Integer.class, cb.currentDate(), root.get("invoiceDate")).alias("daysDiff")
        ).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

        List<Tuple> results = typedQuery.getResultList();

        return PendingInvoiceResponse.from(results);
    }

    @Override
    public List<PendingInvoiceResponse> getPendingInvoices() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ClientInvoiceTracker> root = query.from(ClientInvoiceTracker.class);
        Join<ClientInvoiceTracker, MasterCategoryDetail> masterCategoryDetailJoin = root.join("masterCategoryDetail");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.greaterThan(cb.function("DATEDIFF", Integer.class, cb.currentDate(), root.get("invoiceDate")), root.get("dueDays")));
        predicates.add(root.get("invoiceStatus").in(MasterCategoryDetailEnum.Un_Paid.getId(), MasterCategoryDetailEnum.Partial_Paid.getId()));
        query.multiselect(
                root.get("invoiceNumber").alias("invoiceNumber"),
                root.get("invoiceAmount").alias("invoiceAmount"),
                root.get("invoiceDate").alias("invoiceDate"),
                root.get("paidAmount").alias("paidAmount"),
                root.get("invoicePaidOn").alias("invoicePaidOn"),
                root.get("toEmailId").alias("toEmailAddress"),
                root.get("ccEmailId").alias("ccEmailAddress"),
                root.get("bccEmailId").alias("bccEmailAddress"),
                root.get("clientName").alias("clientName"),
                root.get("clientHubName").alias("clientHubName"),
                masterCategoryDetailJoin.get("name").alias("invoiceStatus"),
                root.get("remarks").alias("remark"),
                root.get("dueDays").alias("dueDays"),
                root.get("reminderSent").alias("reminderSent"),
                cb.function("DATEDIFF", Integer.class, cb.currentDate(), root.get("invoiceDate")).alias("daysDiff")
        ).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

        List<Tuple> results = typedQuery.getResultList();

        return PendingInvoiceResponse.from(results);
    }


    @Override
    public List<PendingInvoiceResponse> getPendingInvoices(String invoiceNumber) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<ClientInvoiceTracker> root = query.from(ClientInvoiceTracker.class);
        Join<ClientInvoiceTracker, MasterCategoryDetail> masterCategoryDetailJoin = root.join("masterCategoryDetail");

        List<Predicate> predicates = new ArrayList<>();
        if (Objects.nonNull(invoiceNumber)) {
            predicates.add(cb.equal(root.get("invoiceNumber"), invoiceNumber));
        }
        predicates.add(cb.greaterThan(cb.function("DATEDIFF", Integer.class, cb.currentDate(), root.get("invoiceDate")), root.get("dueDays")));
        predicates.add(root.get("invoiceStatus").in(MasterCategoryDetailEnum.Un_Paid.getId(), MasterCategoryDetailEnum.Partial_Paid.getId()));
        query.multiselect(
                root.get("invoiceNumber").alias("invoiceNumber"),
                root.get("invoiceAmount").alias("invoiceAmount"),
                root.get("invoiceDate").alias("invoiceDate"),
                root.get("paidAmount").alias("paidAmount"),
                root.get("invoicePaidOn").alias("invoicePaidOn"),
                root.get("toEmailId").alias("toEmailAddress"),
                root.get("ccEmailId").alias("ccEmailAddress"),
                root.get("bccEmailId").alias("bccEmailAddress"),
                root.get("clientName").alias("clientName"),
                root.get("clientHubName").alias("clientHubName"),
                masterCategoryDetailJoin.get("name").alias("invoiceStatus"),
                root.get("remarks").alias("remark"),
                root.get("dueDays").alias("dueDays"),
                root.get("reminderSent").alias("reminderSent"),
                cb.function("DATEDIFF", Integer.class, cb.currentDate(), root.get("invoiceDate")).alias("daysDiff")
        ).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

        List<Tuple> results = typedQuery.getResultList();

        return PendingInvoiceResponse.from(results);
    }
}
