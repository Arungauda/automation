package com.invoice.automation.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for customer-related business operations.
 * Provides methods for customer management, analytics, and reporting.
 */
public interface CustomerService {

    // ========== Customer Information ==========

    /**
     * Retrieves all unique customer names.
     *
     * @return list of customer names sorted alphabetically
     */
    List<String> getAllCustomers();

    /**
     * Checks if a customer exists.
     *
     * @param customerName the customer name to check
     * @return true if customer exists, false otherwise
     */
    boolean customerExists(String customerName);

    /**
     * Searches customers by name (partial match).
     *
     * @param name the name to search for
     * @return list of matching customer names
     */
    List<String> searchCustomersByName(String name);

    // ========== Customer Invoice Operations ==========

    /**
     * Retrieves all invoices for a specific customer.
     *
     * @param customerName the customer name
     * @return list of invoice headers for the customer
     */
    List<com.invoice.automation.entity.InvoiceHeader> getCustomerInvoices(String customerName);

    /**
     * Retrieves customer invoices within a date range.
     *
     * @param customerName the customer name
     * @param startDate    the start date
     * @param endDate      the end date
     * @return list of invoices within the date range
     */
    List<com.invoice.automation.entity.InvoiceHeader> getCustomerInvoicesByDateRange(
            String customerName, LocalDate startDate, LocalDate endDate);

    // ========== Customer Analytics ==========

    /**
     * Calculates comprehensive statistics for a customer.
     *
     * @param customerName the customer name
     * @return map containing customer statistics
     */
    Map<String, Object> getCustomerStatistics(String customerName);

    /**
     * Retrieves top customers by invoice count.
     *
     * @param limit maximum number of customers to return
     * @return list of top customers with their invoice counts
     */
    List<Map<String, Object>> getTopCustomers(int limit);

    /**
     * Calculates total revenue for a customer within a date range.
     *
     * @param customerName the customer name
     * @param startDate    the start date (optional)
     * @param endDate      the end date (optional)
     * @return revenue data for the customer
     */
    Map<String, Object> getCustomerRevenue(String customerName, LocalDate startDate, LocalDate endDate);

    /**
     * Generates a comprehensive activity summary for a customer.
     *
     * @param customerName the customer name
     * @return activity summary data
     */
    Map<String, Object> getCustomerActivitySummary(String customerName);

    // ========== Customer Trends and Analysis ==========

    /**
     * Analyzes customer trends over a specific period.
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return customer trends analysis
     */
    Map<String, Object> getCustomerTrends(LocalDate startDate, LocalDate endDate);

    /**
     * Generates performance report for all customers.
     *
     * @return list of customer performance data
     */
    List<Map<String, Object>> getCustomerPerformanceReport();

    /**
     * Identifies new customers since a specific date.
     *
     * @param since the date to check from
     * @return list of new customer names
     */
    List<String> getNewCustomers(LocalDate since);

    /**
     * Identifies returning customers since a specific date.
     *
     * @param since the date to check from
     * @return list of returning customer names
     */
    List<String> getReturningCustomers(LocalDate since);

    /**
     * Calculates customer retention rate.
     *
     * @param invoices list of invoices to analyze
     * @param since    the date to calculate from
     * @return retention rate percentage
     */
    double calculateCustomerRetentionRate(List<com.invoice.automation.entity.InvoiceHeader> invoices, LocalDate since);

    // ========== Customer Segmentation ==========

    /**
     * Segments customers based on their activity and revenue.
     *
     * @return map of customer segments
     */
    Map<String, List<String>> segmentCustomers();

    /**
     * Identifies high-value customers based on revenue.
     *
     * @param threshold the minimum revenue threshold
     * @return list of high-value customer names
     */
    List<String> getHighValueCustomers(double threshold);

    /**
     * Identifies at-risk customers (no recent activity).
     *
     * @param daysInactive number of days of inactivity
     * @return list of at-risk customer names
     */
    List<String> getAtRiskCustomers(int daysInactive);

    // ========== Customer Communication ==========

    /**
     * Retrieves customers for marketing campaigns.
     *
     * @param campaignType the type of campaign
     * @return list of target customer names
     */
    List<String> getCustomersForCampaign(String campaignType);

    /**
     * Generates customer contact list with recent activity.
     *
     * @param lastActivityDays days of recent activity to consider
     * @return map of customer contact information
     */
    Map<String, Object> generateContactList(int lastActivityDays);
}
