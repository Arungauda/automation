package com.invoice.automation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for generating reports and analytics.
 * Provides methods for various business reports and data analysis.
 */
public interface ReportService {

    // ========== Summary Reports ==========

    /**
     * Generates a comprehensive dashboard summary.
     *
     * @return dashboard summary data
     */
    Map<String, Object> getDashboardSummary();

    /**
     * Generates a monthly report for a specific month and year.
     *
     * @param year  the year
     * @param month the month (1-12)
     * @return monthly report data
     */
    Map<String, Object> getMonthlyReport(int year, int month);

    /**
     * Generates a yearly report for a specific year.
     *
     * @param year the year
     * @return yearly report data
     */
    Map<String, Object> getYearlyReport(int year);

    // ========== Customer Reports ==========

    /**
     * Generates a performance report for all customers.
     *
     * @return customer performance report
     */
    List<Map<String, Object>> getCustomerPerformanceReport();

    /**
     * Analyzes customer trends over time.
     *
     * @param startDate start date
     * @param endDate   end date
     * @return customer trends analysis
     */
    Map<String, Object> getCustomerTrends(LocalDate startDate, LocalDate endDate);

    // ========== Financial Reports ==========

    /**
     * Generates a detailed revenue report.
     *
     * @param startDate start date
     * @param endDate   end date
     * @return revenue report data
     */
    Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate);

    /**
     * Generates a comprehensive financial summary.
     *
     * @return financial summary data
     */
    Map<String, Object> getFinancialSummary();

    // ========== Export Operations ==========

    /**
     * Exports a report to CSV format.
     *
     * @param reportType the type of report to export
     * @param startDate  optional start date for filtering
     * @param endDate    optional end date for filtering
     * @return CSV content as string
     */
    String exportReportToCsv(String reportType, LocalDate startDate, LocalDate endDate);

    // ========== Advanced Analytics ==========

    /**
     * Generates predictive analytics for business forecasting.
     *
     * @return predictive analytics data
     */
    Map<String, Object> getPredictiveAnalytics();

    /**
     * Analyzes seasonal trends in the business.
     *
     * @return seasonal trends analysis
     */
    Map<String, Object> getSeasonalTrends();

    /**
     * Generates a comprehensive business health report.
     *
     * @return business health metrics
     */
    Map<String, Object> getBusinessHealthReport();

    // ========== Custom Reports ==========

    /**
     * Generates a custom report based on specified criteria.
     *
     * @param criteria the report criteria
     * @return custom report data
     */
    Map<String, Object> generateCustomReport(Map<String, Object> criteria);

    /**
     * Schedules a report to be generated periodically.
     *
     * @param reportType   the type of report
     * @param schedule     the schedule configuration
     * @param recipients   list of email recipients
     * @return scheduling result
     */
    Map<String, Object> scheduleReport(String reportType, Map<String, Object> schedule, List<String> recipients);

    // ========== Report Templates ==========

    /**
     * Gets available report templates.
     *
     * @return list of report templates
     */
    List<Map<String, Object>> getReportTemplates();

    /**
     * Creates a new report template.
     *
     * @param template the template configuration
     * @return created template information
     */
    Map<String, Object> createReportTemplate(Map<String, Object> template);

    /**
     * Applies a template to generate a report.
     *
     * @param templateId the template ID
     * @param parameters parameters for the template
     * @return generated report
     */
    Map<String, Object> applyTemplate(String templateId, Map<String, Object> parameters);

    // ========== Report History and Audit ==========

    /**
     * Gets the history of generated reports.
     *
     * @param limit maximum number of records to return
     * @return report generation history
     */
    List<Map<String, Object>> getReportHistory(int limit);

    /**
     * Gets audit trail for a specific report.
     *
     * @param reportId the report ID
     * @return audit trail information
     */
    List<Map<String, Object>> getReportAuditTrail(String reportId);

    // ========== Performance Metrics ==========

    /**
     * Gets key performance indicators (KPIs).
     *
     * @param period the time period for KPIs
     * @return KPI data
     */
    Map<String, Object> getKeyPerformanceIndicators(String period);

    /**
     * Generates a performance comparison report.
     *
     * @param startDate1 first period start date
     * @param endDate1   first period end date
     * @param startDate2 second period start date
     * @param endDate2   second period end date
     * @return comparison report
     */
    Map<String, Object> getPerformanceComparison(
            LocalDate startDate1, LocalDate endDate1,
            LocalDate startDate2, LocalDate endDate2);

    // ========== Data Quality and Validation ==========

    /**
     * Validates data quality for reporting.
     *
     * @return data quality report
     */
    Map<String, Object> getDataQualityReport();

    /**
     * Identifies anomalies in the data.
     *
     * @param startDate start date for analysis
     * @param endDate   end date for analysis
     * @return anomaly detection results
     */
    Map<String, Object> detectAnomalies(LocalDate startDate, LocalDate endDate);

    // ========== Real-time Reporting ==========

    /**
     * Gets real-time dashboard data.
     *
     * @return real-time dashboard data
     */
    Map<String, Object> getRealTimeDashboard();

    /**
     * Sets up real-time alerts for specific metrics.
     *
     * @param alertConfiguration the alert configuration
     * @return alert setup result
     */
    Map<String, Object> setupRealTimeAlerts(Map<String, Object> alertConfiguration);
}
