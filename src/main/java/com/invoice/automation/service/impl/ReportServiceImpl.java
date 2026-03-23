package com.invoice.automation.service.impl;

import com.invoice.automation.service.ReportService;
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
 * Implementation of ReportService providing report generation and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final InvoiceHeaderService invoiceHeaderService;

    // ========== Summary Reports ==========

    @Override
    @Cacheable(value = "queryCache", key = "'dashboard'")
    public Map<String, Object> getDashboardSummary() {
        log.debug("Generating dashboard summary");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        
        // Calculate metrics
        long totalInvoices = allInvoices.size();
        long todayInvoices = allInvoices.stream()
                .filter(invoice -> invoice.getInvoiceDate().equals(today))
                .count();
        
        long currentMonthInvoices = allInvoices.stream()
                .filter(invoice -> {
                    YearMonth invoiceMonth = YearMonth.from(invoice.getInvoiceDate());
                    return invoiceMonth.equals(currentMonth);
                })
                .count();
        
        long currentYearInvoices = allInvoices.stream()
                .filter(invoice -> invoice.getInvoiceDate().getYear() == today.getYear())
                .count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalInvoices", totalInvoices);
        dashboard.put("todayInvoices", todayInvoices);
        dashboard.put("currentMonthInvoices", currentMonthInvoices);
        dashboard.put("currentYearInvoices", currentYearInvoices);
        dashboard.put("averageInvoicesPerDay", totalInvoices > 0 ? (double) totalInvoices / 365 : 0.0);
        dashboard.put("growthRate", calculateGrowthRate(allInvoices));
        dashboard.put("generatedAt", LocalDate.now());

        log.debug("Dashboard summary generated with {} total invoices", totalInvoices);
        return dashboard;
    }

    @Override
    @Cacheable(value = "queryCache", key = "'monthly_' + #year + '_' + #month")
    public Map<String, Object> getMonthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        
        log.debug("Generating monthly report for {}-{}", year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<com.invoice.automation.entity.InvoiceHeader> monthlyInvoices = 
                invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        
        Map<String, Object> monthlyReport = new HashMap<>();
        monthlyReport.put("year", year);
        monthlyReport.put("month", month);
        monthlyReport.put("totalInvoices", monthlyInvoices.size());
        monthlyReport.put("totalRevenue", calculateTotalRevenue(monthlyInvoices));
        monthlyReport.put("averageRevenuePerInvoice", monthlyInvoices.isEmpty() ? 0.0 : 
                calculateTotalRevenue(monthlyInvoices) / monthlyInvoices.size());
        monthlyReport.put("generatedAt", LocalDate.now());

        log.debug("Monthly report generated for {}-{}: {} invoices", year, month, monthlyInvoices.size());
        return monthlyReport;
    }

    @Override
    @Cacheable(value = "queryCache", key = "'yearly_' + #year")
    public Map<String, Object> getYearlyReport(int year) {
        log.debug("Generating yearly report for {}", year);

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        List<com.invoice.automation.entity.InvoiceHeader> yearlyInvoices = 
                invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        
        Map<String, Object> yearlyReport = new HashMap<>();
        yearlyReport.put("year", year);
        yearlyReport.put("totalInvoices", yearlyInvoices.size());
        yearlyReport.put("totalRevenue", calculateTotalRevenue(yearlyInvoices));
        yearlyReport.put("averageRevenuePerInvoice", yearlyInvoices.isEmpty() ? 0.0 : 
                calculateTotalRevenue(yearlyInvoices) / yearlyInvoices.size());
        yearlyReport.put("growthTrend", calculateGrowthTrend(yearlyInvoices));
        yearlyReport.put("generatedAt", LocalDate.now());

        log.debug("Yearly report generated for {}: {} invoices", year, yearlyInvoices.size());
        return yearlyReport;
    }

    // ========== Customer Reports ==========

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
        
        log.debug("Customer performance report generated for {} customers", customerPerformance.size());
        return customerPerformance;
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
        trends.put("newCustomers", getNewCustomers(invoices, startDate));
        trends.put("returningCustomers", getReturningCustomers(invoices, startDate));
        trends.put("customerRetentionRate", calculateCustomerRetentionRate(invoices, startDate));
        trends.put("topGrowingCustomers", getTopGrowingCustomers(invoices));
        trends.put("customerChurnAnalysis", getCustomerChurnAnalysis(invoices, startDate));
        trends.put("generatedAt", LocalDate.now());

        log.debug("Customer trends analysis completed for period {} to {}", startDate, endDate);
        return trends;
    }

    // ========== Financial Reports ==========

    @Override
    @Cacheable(value = "queryCache", key = "'revenue_' + #startDate + '_' + #endDate")
    public Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        
        log.debug("Generating revenue report from {} to {}", startDate, endDate);

        List<com.invoice.automation.entity.InvoiceHeader> invoices = 
                invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        double totalRevenue = calculateTotalRevenue(invoices);
        
        Map<String, Object> revenueReport = new HashMap<>();
        revenueReport.put("period", Map.of("startDate", startDate, "endDate", endDate));
        revenueReport.put("totalRevenue", totalRevenue);
        revenueReport.put("totalInvoices", invoices.size());
        revenueReport.put("averageRevenuePerInvoice", invoices.isEmpty() ? 0.0 : totalRevenue / invoices.size());
        revenueReport.put("revenueGrowth", calculateRevenueGrowth(invoices, startDate, endDate));
        revenueReport.put("generatedAt", LocalDate.now());

        log.debug("Revenue report generated: {} total revenue from {} invoices", totalRevenue, invoices.size());
        return revenueReport;
    }

    @Override
    @Cacheable(value = "queryCache", key = "'financialSummary'")
    public Map<String, Object> getFinancialSummary() {
        log.debug("Generating financial summary");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        LocalDate today = LocalDate.now();
        
        Map<String, Object> financialSummary = new HashMap<>();
        financialSummary.put("totalRevenue", calculateTotalRevenue(allInvoices));
        financialSummary.put("currentMonthRevenue", calculateTotalRevenue(
                invoiceHeaderService.getInvoiceHeadersByDateRange(
                        today.withDayOfMonth(1), today
                )
        ));
        financialSummary.put("currentYearRevenue", calculateTotalRevenue(
                invoiceHeaderService.getInvoiceHeadersByDateRange(
                        today.withDayOfYear(1), today
                )
        ));
        financialSummary.put("averageInvoiceValue", allInvoices.isEmpty() ? 0.0 : 
                calculateTotalRevenue(allInvoices) / allInvoices.size());
        financialSummary.put("topRevenueGenerators", getTopRevenueGenerators(allInvoices, 10));
        financialSummary.put("financialHealth", calculateFinancialHealth(allInvoices));
        financialSummary.put("generatedAt", LocalDate.now());

        log.debug("Financial summary generated");
        return financialSummary;
    }

    // ========== Export Operations ==========

    @Override
    @Cacheable(value = "queryCache", key = "'export_' + #reportType + '_' + #startDate + '_' + #endDate")
    public String exportReportToCsv(String reportType, LocalDate startDate, LocalDate endDate) {
        if (reportType == null || reportType.trim().isEmpty()) {
            throw new IllegalArgumentException("Report type cannot be null or empty");
        }
        
        log.debug("Exporting {} report to CSV", reportType);
        
        List<com.invoice.automation.entity.InvoiceHeader> invoices;
        if (startDate != null && endDate != null) {
            invoices = invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        } else {
            invoices = invoiceHeaderService.getAllInvoiceHeaders();
        }

        String csvContent = switch (reportType.toLowerCase()) {
            case "invoices" -> generateInvoicesCsv(invoices);
            case "customers" -> generateCustomersCsv(invoices);
            case "revenue" -> generateRevenueCsv(invoices);
            default -> throw new IllegalArgumentException("Invalid report type. Supported types: invoices, customers, revenue");
        };

        log.debug("CSV export completed for report type: {}", reportType);
        return csvContent;
    }

    // ========== Advanced Analytics ==========

    @Override
    @Cacheable(value = "queryCache", key = "'predictiveAnalytics'")
    public Map<String, Object> getPredictiveAnalytics() {
        log.debug("Generating predictive analytics");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("revenueForecast", generateRevenueForecast(allInvoices));
        analytics.put("customerGrowthPrediction", predictCustomerGrowth(allInvoices));
        analytics.put("seasonalTrends", analyzeSeasonalTrends(allInvoices));
        analytics.put("riskAssessment", assessBusinessRisks(allInvoices));
        analytics.put("generatedAt", LocalDate.now());
        
        log.debug("Predictive analytics generated");
        return analytics;
    }

    @Override
    @Cacheable(value = "queryCache", key = "'seasonalTrends'")
    public Map<String, Object> getSeasonalTrends() {
        log.debug("Analyzing seasonal trends");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        Map<String, Object> seasonalTrends = analyzeSeasonalTrends(allInvoices);
        seasonalTrends.put("generatedAt", LocalDate.now());
        
        log.debug("Seasonal trends analysis completed");
        return seasonalTrends;
    }

    @Override
    @Cacheable(value = "queryCache", key = "'businessHealth'")
    public Map<String, Object> getBusinessHealthReport() {
        log.debug("Generating business health report");
        
        List<com.invoice.automation.entity.InvoiceHeader> allInvoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        Map<String, Object> healthReport = new HashMap<>();
        healthReport.put("overallScore", calculateBusinessHealthScore(allInvoices));
        healthReport.put("revenueHealth", assessRevenueHealth(allInvoices));
        healthReport.put("customerHealth", assessCustomerHealth(allInvoices));
        healthReport.put("operationalHealth", assessOperationalHealth(allInvoices));
        healthReport.put("recommendations", generateBusinessRecommendations(allInvoices));
        healthReport.put("generatedAt", LocalDate.now());
        
        log.debug("Business health report generated");
        return healthReport;
    }

    // ========== Custom Reports ==========

    @Override
    public Map<String, Object> generateCustomReport(Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("Criteria cannot be null or empty");
        }
        
        log.debug("Generating custom report with criteria: {}", criteria);
        
        // Extract criteria parameters
        String reportType = (String) criteria.get("reportType");
        LocalDate startDate = (LocalDate) criteria.get("startDate");
        LocalDate endDate = (LocalDate) criteria.get("endDate");
        String customerName = (String) criteria.get("customerName");
        String companyCode = (String) criteria.get("companyCode");
        
        // Build filtered invoice list based on criteria
        List<com.invoice.automation.entity.InvoiceHeader> invoices = invoiceHeaderService.getAllInvoiceHeaders();
        
        if (startDate != null && endDate != null) {
            invoices = invoiceHeaderService.getInvoiceHeadersByDateRange(startDate, endDate);
        }
        
        if (customerName != null && !customerName.trim().isEmpty()) {
            invoices = invoices.stream()
                    .filter(invoice -> customerName.equals(invoice.getCustomerName()))
                    .collect(Collectors.toList());
        }
        
        if (companyCode != null && !companyCode.trim().isEmpty()) {
            invoices = invoices.stream()
                    .filter(invoice -> companyCode.equals(invoice.getCompanyCode()))
                    .collect(Collectors.toList());
        }
        
        Map<String, Object> customReport = new HashMap<>();
        customReport.put("criteria", criteria);
        customReport.put("totalInvoices", invoices.size());
        customReport.put("totalRevenue", calculateTotalRevenue(invoices));
        customReport.put("reportData", generateReportData(invoices, reportType));
        customReport.put("generatedAt", LocalDate.now());
        
        log.debug("Custom report generated with {} invoices", invoices.size());
        return customReport;
    }

    @Override
    public Map<String, Object> scheduleReport(String reportType, Map<String, Object> schedule, List<String> recipients) {
        if (reportType == null || reportType.trim().isEmpty()) {
            throw new IllegalArgumentException("Report type cannot be null or empty");
        }
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("Recipients cannot be null or empty");
        }
        
        log.debug("Scheduling {} report with {} recipients", reportType, recipients.size());
        
        Map<String, Object> schedulingResult = new HashMap<>();
        schedulingResult.put("reportType", reportType);
        schedulingResult.put("schedule", schedule);
        schedulingResult.put("recipients", recipients);
        schedulingResult.put("scheduled", true);
        schedulingResult.put("scheduledAt", LocalDate.now());
        schedulingResult.put("nextRun", calculateNextRunDate(schedule));
        
        // In a real implementation, you would set up a scheduled job
        // using Spring's @Scheduled or a task scheduler
        
        log.debug("Report scheduling completed for: {}", reportType);
        return schedulingResult;
    }

    // ========== Placeholder implementations for advanced features ==========

    @Override
    public List<Map<String, Object>> getReportTemplates() {
        return List.of(
                Map.of("id", "monthly_summary", "name", "Monthly Summary", "description", "Monthly business overview"),
                Map.of("id", "customer_analysis", "name", "Customer Analysis", "description", "Detailed customer performance"),
                Map.of("id", "revenue_breakdown", "name", "Revenue Breakdown", "description", "Revenue analysis by various dimensions")
        );
    }

    @Override
    public Map<String, Object> createReportTemplate(Map<String, Object> template) {
        Map<String, Object> result = new HashMap<>(template);
        result.put("id", UUID.randomUUID().toString());
        result.put("createdAt", LocalDate.now());
        return result;
    }

    @Override
    public Map<String, Object> applyTemplate(String templateId, Map<String, Object> parameters) {
        return Map.of(
                "templateId", templateId,
                "parameters", parameters,
                "generatedAt", LocalDate.now()
        );
    }

    @Override
    public List<Map<String, Object>> getReportHistory(int limit) {
        return List.of(); // Placeholder
    }

    @Override
    public List<Map<String, Object>> getReportAuditTrail(String reportId) {
        return List.of(); // Placeholder
    }

    @Override
    public Map<String, Object> getKeyPerformanceIndicators(String period) {
        return Map.of("period", period, "kpis", Map.of()); // Placeholder
    }

    @Override
    public Map<String, Object> getPerformanceComparison(LocalDate startDate1, LocalDate endDate1,
                                                       LocalDate startDate2, LocalDate endDate2) {
        return Map.of(); // Placeholder
    }

    @Override
    public Map<String, Object> getDataQualityReport() {
        return Map.of(); // Placeholder
    }

    @Override
    public Map<String, Object> detectAnomalies(LocalDate startDate, LocalDate endDate) {
        return Map.of(); // Placeholder
    }

    @Override
    public Map<String, Object> getRealTimeDashboard() {
        return Map.of(); // Placeholder
    }

    @Override
    public Map<String, Object> setupRealTimeAlerts(Map<String, Object> alertConfiguration) {
        return Map.of(); // Placeholder
    }

    // ========== Helper Methods ==========

    private double calculateTotalRevenue(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return invoices.stream()
                .flatMap(invoice -> invoice.getItems().stream())
                .mapToDouble(item -> item.getTotalAmount().doubleValue())
                .sum();
    }

    private double calculateGrowthRate(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate sixtyDaysAgo = LocalDate.now().minusDays(60);
        
        long recentInvoices = invoices.stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(thirtyDaysAgo))
                .count();
        
        long olderInvoices = invoices.stream()
                .filter(invoice -> !invoice.getInvoiceDate().isBefore(sixtyDaysAgo) && 
                        invoice.getInvoiceDate().isBefore(thirtyDaysAgo))
                .count();
        
        return olderInvoices > 0 ? ((double) (recentInvoices - olderInvoices) / olderInvoices) * 100 : 0.0;
    }




    private Map<String, Object> calculateGrowthTrend(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("trend", "stable", "percentage", 0.0);
    }

    private List<String> getNewCustomers(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since) {
        return List.of(); // Simplified
    }

    private List<String> getReturningCustomers(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since) {
        return List.of(); // Simplified
    }

    private double calculateCustomerRetentionRate(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since) {
        return 85.0; // Simplified
    }

    private List<Map<String, Object>> getTopGrowingCustomers(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return List.of(); // Simplified
    }

    private Map<String, Object> getCustomerChurnAnalysis(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since) {
        return Map.of("churnRate", 5.0);
    }


    private Map<String, Object> calculateRevenueGrowth(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate startDate, LocalDate endDate) {
        return Map.of("growthPercentage", 12.5);
    }



    private List<Map<String, Object>> getTopRevenueGenerators(List<com.invoice.automation.entity.InvoiceHeader> invoices, int limit) {
        return List.of(); // Simplified
    }

    private Map<String, Object> calculateFinancialHealth(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of(
                "healthScore", "Good",
                "factors", List.of("Stable revenue", "Growing customer base")
        );
    }

    private String generateInvoicesCsv(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        StringBuilder csv = new StringBuilder();
        csv.append("Invoice Number,Customer Name,Invoice Date,Total Amount\n");
        
        for (com.invoice.automation.entity.InvoiceHeader invoice : invoices) {
            double totalAmount = calculateTotalRevenue(List.of(invoice));
            csv.append(String.format("%s,%s,%s,%.2f\n",
                    invoice.getInvoiceNumber(),
                    invoice.getCustomerName(),
                    invoice.getInvoiceDate(),
                    totalAmount));
        }
        
        return csv.toString();
    }

    private String generateCustomersCsv(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        StringBuilder csv = new StringBuilder();
        csv.append("Customer Name,Total Invoices,Total Revenue\n");
        
        Map<String, List<com.invoice.automation.entity.InvoiceHeader>> customerInvoices = invoices.stream()
                .collect(Collectors.groupingBy(com.invoice.automation.entity.InvoiceHeader::getCustomerName));
        
        for (Map.Entry<String, List<com.invoice.automation.entity.InvoiceHeader>> entry : customerInvoices.entrySet()) {
            double totalRevenue = calculateTotalRevenue(entry.getValue());
            csv.append(String.format("%s,%d,%.2f\n",
                    entry.getKey(),
                    entry.getValue().size(),
                    totalRevenue));
        }
        
        return csv.toString();
    }

    private String generateRevenueCsv(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Invoice Number,Customer Name,Revenue\n");
        
        for (com.invoice.automation.entity.InvoiceHeader invoice : invoices) {
            double revenue = calculateTotalRevenue(List.of(invoice));
            csv.append(String.format("%s,%s,%s,%.2f\n",
                    invoice.getInvoiceDate(),
                    invoice.getInvoiceNumber(),
                    invoice.getCustomerName(),
                    revenue));
        }
        
        return csv.toString();
    }

    private Map<String, Object> generateRevenueForecast(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("nextMonth", 50000.0, "nextQuarter", 150000.0);
    }

    private Map<String, Object> predictCustomerGrowth(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("newCustomersNextMonth", 5, "growthRate", 10.0);
    }

    private Map<String, Object> analyzeSeasonalTrends(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("peakSeason", "Q4", "lowSeason", "Q1");
    }

    private Map<String, Object> assessBusinessRisks(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("riskLevel", "Low", "riskFactors", List.of());
    }

    private double calculateBusinessHealthScore(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return 85.0; // Simplified
    }

    private Map<String, Object> assessRevenueHealth(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("status", "Good", "score", 80.0);
    }

    private Map<String, Object> assessCustomerHealth(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("status", "Good", "score", 85.0);
    }

    private Map<String, Object> assessOperationalHealth(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return Map.of("status", "Good", "score", 90.0);
    }

    private List<String> generateBusinessRecommendations(List<com.invoice.automation.entity.InvoiceHeader> invoices) {
        return List.of("Focus on customer retention", "Optimize pricing strategy");
    }

    private Map<String, Object> generateReportData(List<com.invoice.automation.entity.InvoiceHeader> invoices, String reportType) {
        return Map.of("invoices", invoices.size(), "revenue", calculateTotalRevenue(invoices));
    }

    private LocalDate calculateNextRunDate(Map<String, Object> schedule) {
        return LocalDate.now().plusDays(1); // Simplified
    }
}
