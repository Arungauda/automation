package com.invoice.automation.service.impl;

import com.invoice.automation.service.CustomerService;
import com.invoice.automation.service.InvoiceHeaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of CustomerService providing customer-related business operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final InvoiceHeaderService invoiceHeaderService;

    @Override
    @Cacheable(value = "customerStats", key = "'allCustomers'")
    public List<String> getAllCustomers() {
        log.debug("Retrieving all unique customers");
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        List<String> customers = allInvoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getCustomerName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        log.debug("Found {} unique customers", customers.size());
        return customers;
    }

    @Override
    public boolean customerExists(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            return false;
        }
        
        List<com.invoice.automation.entity.InvoiceHeader> invoices = 
                invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        return !invoices.isEmpty();
    }

    @Override
    @Cacheable(value = "queryCache", key = "'searchCustomers_' + #name")
    public List<String> searchCustomersByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        log.debug("Searching customers by name: {}", name);
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        List<String> matchingCustomers = allInvoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getCustomerName)
                .distinct()
                .filter(customerName -> customerName.toLowerCase().contains(name.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
        
        log.debug("Found {} matching customers for search term: {}", matchingCustomers.size(), name);
        return matchingCustomers;
    }

    @Override
    public List<com.invoice.automation.entity.InvoiceHeader> getCustomerInvoices(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        
        log.debug("Retrieving invoices for customer: {}", customerName);
        return invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
    }

    @Override
    public List<com.invoice.automation.entity.InvoiceHeader> getCustomerInvoicesByDateRange(
            String customerName, LocalDate startDate, LocalDate endDate) {
        
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        log.debug("Retrieving invoices for customer {} between {} and {}", customerName, startDate, endDate);
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = 
                invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        
        return allInvoices.stream()
                .filter(invoice -> customerName.equals(invoice.getCustomerName()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "customerStats", key = "'stats_' + #customerName")
    public Map<String, Object> getCustomerStatistics(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        
        log.debug("Calculating statistics for customer: {}", customerName);
        
        List<com.invoice.automation.entity.InvoiceHeader> customerInvoices = 
                invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        
        LocalDate today = LocalDate.now();
        long totalInvoices = customerInvoices.size();
        long currentYearInvoices = customerInvoices.stream()
                .filter(invoice -> invoice.getInvoiceDate().getYear() == today.getYear())
                .count();
        
        long currentMonthInvoices = customerInvoices.stream()
                .filter(invoice -> {
                    LocalDate date = invoice.getInvoiceDate();
                    return date.getYear() == today.getYear() && 
                           date.getMonth() == today.getMonth();
                })
                .count();

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("customerName", customerName);
        statistics.put("totalInvoices", totalInvoices);
        statistics.put("currentYearInvoices", currentYearInvoices);
        statistics.put("currentMonthInvoices", currentMonthInvoices);
        statistics.put("firstInvoiceDate", customerInvoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                .min(LocalDate::compareTo)
                .orElse(null));
        statistics.put("lastInvoiceDate", customerInvoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                .max(LocalDate::compareTo)
                .orElse(null));
        
        log.debug("Generated statistics for customer {}: {} total invoices", customerName, totalInvoices);
        return statistics;
    }

    @Override
    @Cacheable(value = "customerStats", key = "'topCustomers_' + #limit")
    public List<Map<String, Object>> getTopCustomers(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        log.debug("Retrieving top {} customers by invoice count", limit);
        
        List<com.invoice.automation.entity.InvoiceHeader> topCustomersInvoices = 
                invoiceHeaderService.getTopCustomersByInvoiceCount(limit);
        
        List<Map<String, Object>> topCustomers = topCustomersInvoices.stream()
                .collect(Collectors.groupingBy(com.invoice.automation.entity.InvoiceHeader::getCustomerName, 
                                              Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> customerData = new HashMap<>();
                    customerData.put("customerName", entry.getKey());
                    customerData.put("invoiceCount", entry.getValue());
                    return customerData;
                })
                .collect(Collectors.toList());
        
        log.debug("Retrieved {} top customers", topCustomers.size());
        return topCustomers;
    }

    @Override
    @Cacheable(value = "customerStats", key = "'revenue_' + #customerName + '_' + #startDate + '_' + #endDate")
    public Map<String, Object> getCustomerRevenue(String customerName, LocalDate startDate, LocalDate endDate) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        
        // Use provided dates or default to all time
        LocalDate start = startDate != null ? startDate : LocalDate.of(2000, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        
        log.debug("Calculating revenue for customer {} from {} to {}", customerName, start, end);
        
        List<com.invoice.automation.entity.InvoiceHeader> customerInvoices = 
                invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        List<com.invoice.automation.entity.InvoiceHeader> filteredInvoices = customerInvoices.stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(start) && !invoice.getInvoiceDate().isAfter(end))
                .collect(Collectors.toList());

        // Calculate revenue from invoice items
        double totalRevenue = filteredInvoices.stream()
                .flatMap(invoice -> invoice.getItems().stream())
                .mapToDouble(item -> item.getTotalAmount().doubleValue())
                .sum();

        Map<String, Object> revenueData = new HashMap<>();
        revenueData.put("customerName", customerName);
        revenueData.put("totalRevenue", totalRevenue);
        revenueData.put("invoiceCount", filteredInvoices.size());
        revenueData.put("startDate", start);
        revenueData.put("endDate", end);
        revenueData.put("averageRevenuePerInvoice", 
                filteredInvoices.isEmpty() ? 0.0 : totalRevenue / filteredInvoices.size());
        
        log.debug("Calculated revenue for customer {}: {} total revenue from {} invoices", 
                 customerName, totalRevenue, filteredInvoices.size());
        return revenueData;
    }

    @Override
    @Cacheable(value = "customerStats", key = "'activity_' + #customerName")
    public Map<String, Object> getCustomerActivitySummary(String customerName) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        
        log.debug("Generating activity summary for customer: {}", customerName);
        
        List<com.invoice.automation.entity.InvoiceHeader> customerInvoices = 
                invoiceHeaderService.getInvoiceHeadersByCustomerName(customerName);
        
        long totalInvoices = customerInvoices.size();
        long invoicesWithPdf = customerInvoices.stream()
                .filter(invoice -> invoice.getInvoicePDF() != null)
                .count();
        
        Map<String, Long> companyCodeDistribution = customerInvoices.stream()
                .collect(Collectors.groupingBy(
                        invoice -> invoice.getCompanyCode() != null ? invoice.getCompanyCode() : "N/A",
                        Collectors.counting()
                ));

        Map<String, Object> activitySummary = new HashMap<>();
        activitySummary.put("customerName", customerName);
        activitySummary.put("totalInvoices", totalInvoices);
        activitySummary.put("invoicesWithPdf", invoicesWithPdf);
        activitySummary.put("pdfAttachmentRate", totalInvoices > 0 ? (double) invoicesWithPdf / totalInvoices * 100 : 0.0);
        activitySummary.put("companyCodeDistribution", companyCodeDistribution);
        activitySummary.put("hasMultipleCompanyCodes", companyCodeDistribution.size() > 1);
        activitySummary.put("isRegularCustomer", totalInvoices > 5);
        activitySummary.put("lastActivityDate", customerInvoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                .max(LocalDate::compareTo)
                .orElse(null));
        
        log.debug("Generated activity summary for customer: {}", customerName);
        return activitySummary;
    }

    @Override
    @Cacheable(value = "customerStats", key = "'trends_' + #startDate + '_' + #endDate")
    public Map<String, Object> getCustomerTrends(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        log.debug("Analyzing customer trends from {} to {}", startDate, endDate);
        
        List<com.invoice.automation.entity.InvoiceHeader> invoices = 
                invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("period", Map.of("startDate", startDate, "endDate", endDate));
        trends.put("newCustomers", getNewCustomers(startDate));
        trends.put("returningCustomers", getReturningCustomers(startDate));
        trends.put("customerRetentionRate", calculateCustomerRetentionRate(invoices, startDate));
        trends.put("topGrowingCustomers", getTopGrowingCustomers(invoices));
        trends.put("customerChurnAnalysis", getCustomerChurnAnalysis(invoices, startDate));
        
        log.debug("Generated customer trends analysis for period {} to {}", startDate, endDate);
        return trends;
    }

    @Override
    @Cacheable(value = "customerStats", key = "'performance'")
    public List<Map<String, Object>> getCustomerPerformanceReport() {
        log.debug("Generating customer performance report");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        List<Map<String, Object>> customerPerformance = allInvoices.stream()
                .collect(Collectors.groupingBy(com.invoice.automation.entity.InvoiceHeader::getCustomerName))
                .entrySet().stream()
                .map(entry -> {
                    List<com.invoice.automation.entity.InvoiceHeader> customerInvoices = entry.getValue();
                    
                    Map<String, Object> performance = new HashMap<>();
                    performance.put("customerName", entry.getKey());
                    performance.put("totalInvoices", customerInvoices.size());
                    performance.put("totalRevenue", calculateTotalRevenue(customerInvoices));
                    performance.put("averageRevenuePerInvoice", 
                            customerInvoices.isEmpty() ? 0.0 : calculateTotalRevenue(customerInvoices) / customerInvoices.size());
                    performance.put("firstInvoiceDate", customerInvoices.stream()
                            .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                            .min(LocalDate::compareTo)
                            .orElse(null));
                    performance.put("lastInvoiceDate", customerInvoices.stream()
                            .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                            .max(LocalDate::compareTo)
                            .orElse(null));
                    performance.put("companyCodes", customerInvoices.stream()
                            .map(invoice -> invoice.getCompanyCode() != null ? invoice.getCompanyCode() : "N/A")
                            .distinct()
                            .collect(Collectors.toList()));
                    performance.put("pdfAttachmentRate", customerInvoices.isEmpty() ? 0.0 :
                            (double) customerInvoices.stream()
                                    .filter(invoice -> invoice.getInvoicePDF() != null)
                                    .count() / customerInvoices.size() * 100);
                    
                    return performance;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("totalRevenue"), (Double) a.get("totalRevenue")))
                .collect(Collectors.toList());
        
        log.debug("Generated performance report for {} customers", customerPerformance.size());
        return customerPerformance;
    }

    @Override
    public List<String> getNewCustomers(LocalDate since) {
        if (since == null) {
            throw new IllegalArgumentException("Since date cannot be null");
        }
        
        log.debug("Identifying new customers since: {}", since);
        
        // Find customers whose first invoice is after the given date
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        return allInvoices.stream()
                .collect(Collectors.groupingBy(com.invoice.automation.entity.InvoiceHeader::getCustomerName))
                .entrySet().stream()
                .filter(entry -> {
                    LocalDate firstInvoiceDate = entry.getValue().stream()
                            .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                            .min(LocalDate::compareTo)
                            .orElse(null);
                    return firstInvoiceDate != null && !firstInvoiceDate.isBefore(since);
                })
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getReturningCustomers(LocalDate since) {
        if (since == null) {
            throw new IllegalArgumentException("Since date cannot be null");
        }
        
        log.debug("Identifying returning customers since: {}", since);
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        LocalDate cutoffDate = since.minusMonths(6); // Look back 6 months to determine returning customers
        
        return allInvoices.stream()
                .collect(Collectors.groupingBy(com.invoice.automation.entity.InvoiceHeader::getCustomerName))
                .entrySet().stream()
                .filter(entry -> {
                    List<LocalDate> invoiceDates = entry.getValue().stream()
                            .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                            .sorted()
                            .collect(Collectors.toList());
                    
                    boolean hasOldInvoice = invoiceDates.stream().anyMatch(date -> !date.isAfter(cutoffDate));
                    boolean hasRecentInvoice = invoiceDates.stream().anyMatch(date -> !date.isBefore(since));
                    
                    return hasOldInvoice && hasRecentInvoice;
                })
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public double calculateCustomerRetentionRate(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since) {
        if (invoices == null || since == null) {
            throw new IllegalArgumentException("Invoices list and since date cannot be null");
        }
        
        log.debug("Calculating customer retention rate since: {}", since);
        
        Set<String> allCustomers = invoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getCustomerName)
                .collect(Collectors.toSet());
        
        if (allCustomers.isEmpty()) {
            return 0.0;
        }
        
        Set<String> activeCustomers = invoices.stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(since))
                .map(com.invoice.automation.entity.InvoiceHeader::getCustomerName)
                .collect(Collectors.toSet());
        
        double retentionRate = (double) activeCustomers.size() / allCustomers.size() * 100;
        log.debug("Customer retention rate: {}% ({} out of {} customers)", 
                 retentionRate, activeCustomers.size(), allCustomers.size());
        
        return retentionRate;
    }

    @Override
    @Cacheable(value = "customerStats", key = "'segments'")
    public Map<String, List<String>> segmentCustomers() {
        log.debug("Segmenting customers based on activity and revenue");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        
        Map<String, List<String>> segments = new HashMap<>();
        
        Map<String, List<com.invoice.automation.entity.InvoiceHeader>> customerInvoices = allInvoices.stream()
                .collect(Collectors.groupingBy(com.invoice.automation.entity.InvoiceHeader::getCustomerName));
        
        for (Map.Entry<String, List<com.invoice.automation.entity.InvoiceHeader>> entry : customerInvoices.entrySet()) {
            String customerName = entry.getKey();
            List<com.invoice.automation.entity.InvoiceHeader> invoices = entry.getValue();
            
            LocalDate lastInvoiceDate = invoices.stream()
                    .map(com.invoice.automation.entity.InvoiceHeader::getInvoiceDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            
            double totalRevenue = calculateTotalRevenue(invoices);
            
            if (lastInvoiceDate != null) {
                if (!lastInvoiceDate.isBefore(threeMonthsAgo) && totalRevenue > 10000) {
                    segments.computeIfAbsent("VIP", k -> new ArrayList<>()).add(customerName);
                } else if (!lastInvoiceDate.isBefore(threeMonthsAgo)) {
                    segments.computeIfAbsent("Active", k -> new ArrayList<>()).add(customerName);
                } else if (!lastInvoiceDate.isBefore(oneYearAgo)) {
                    segments.computeIfAbsent("At Risk", k -> new ArrayList<>()).add(customerName);
                } else {
                    segments.computeIfAbsent("Inactive", k -> new ArrayList<>()).add(customerName);
                }
            }
        }
        
        log.debug("Segmented customers into {} categories", segments.size());
        return segments;
    }

    @Override
    public List<String> getHighValueCustomers(double threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        
        log.debug("Identifying high-value customers with threshold: {}", threshold);
        
        List<Map<String, Object>> performanceReport = getCustomerPerformanceReport();
        
        return performanceReport.stream()
                .filter(performance -> (Double) performance.get("totalRevenue") >= threshold)
                .map(performance -> (String) performance.get("customerName"))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAtRiskCustomers(int daysInactive) {
        if (daysInactive < 0) {
            throw new IllegalArgumentException("Days inactive must be non-negative");
        }
        
        log.debug("Identifying at-risk customers inactive for {} days", daysInactive);
        
        LocalDate cutoffDate = LocalDate.now().minusDays(daysInactive);
        Map<String, List<String>> segments = segmentCustomers();
        
        List<String> atRiskCustomers = new ArrayList<>();
        atRiskCustomers.addAll(segments.getOrDefault("At Risk", Collections.emptyList()));
        atRiskCustomers.addAll(segments.getOrDefault("Inactive", Collections.emptyList()));
        
        return atRiskCustomers.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<String> getCustomersForCampaign(String campaignType) {
        if (campaignType == null || campaignType.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign type cannot be null or empty");
        }
        
        log.debug("Getting customers for campaign type: {}", campaignType);
        
        Map<String, List<String>> segments = segmentCustomers();
        
        return switch (campaignType.toLowerCase()) {
            case "reactivation" -> segments.getOrDefault("Inactive", Collections.emptyList());
            case "retention" -> segments.getOrDefault("At Risk", Collections.emptyList());
            case "upsell" -> segments.getOrDefault("Active", Collections.emptyList());
            case "vip" -> segments.getOrDefault("VIP", Collections.emptyList());
            default -> throw new IllegalArgumentException("Unknown campaign type: " + campaignType);
        };
    }

    @Override
    public Map<String, Object> generateContactList(int lastActivityDays) {
        if (lastActivityDays < 0) {
            throw new IllegalArgumentException("Last activity days must be non-negative");
        }
        
        log.debug("Generating contact list for customers active in last {} days", lastActivityDays);
        
        LocalDate cutoffDate = LocalDate.now().minusDays(lastActivityDays);
        List<com.invoice.automation.entity.InvoiceHeader> recentInvoices = 
                invoiceHeaderService.getInvoiceHeadersByDateRange(cutoffDate, LocalDate.now());
        
        Set<String> activeCustomers = recentInvoices.stream()
                .map(com.invoice.automation.entity.InvoiceHeader::getCustomerName)
                .collect(Collectors.toSet());
        
        Map<String, Object> contactList = new HashMap<>();
        contactList.put("activeCustomers", new ArrayList<>(activeCustomers));
        contactList.put("totalActiveCustomers", activeCustomers.size());
        contactList.put("generatedDate", LocalDate.now());
        contactList.put("activityPeriod", Map.of("startDate", cutoffDate, "endDate", LocalDate.now()));
        
        log.debug("Generated contact list with {} active customers", activeCustomers.size());
        return contactList;
    }

    // ========== Helper Methods ==========

    private double calculateTotalRevenue(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return invoices.stream()
                .flatMap(invoice -> invoice.getItems().stream())
                .mapToDouble(item -> item.getTotalAmount().doubleValue())
                .sum();
    }

    private List<Map<String, Object>> getTopGrowingCustomers(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        // Simplified implementation - in real scenario, compare growth rates
        return List.of();
    }

    private Map<String, Object> getCustomerChurnAnalysis(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since) {
        // Simplified churn analysis
        Map<String, Object> churnAnalysis = new HashMap<>();
        churnAnalysis.put("churnRate", 5.0);
        churnAnalysis.put("analysisDate", LocalDate.now());
        churnAnalysis.put("periodStart", since);
        return churnAnalysis;
    }
}
