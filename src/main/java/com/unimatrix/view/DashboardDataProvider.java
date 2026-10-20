package com.unimatrix.view;

/**
 * Seam between the UI and the DAO layer. Implement this with your DAOs later;
 * the view never touches a DAO directly. Called off the EDT, so blocking JDBC calls are fine.
 */
@FunctionalInterface
public interface DashboardDataProvider {
    DashboardStats load() throws Exception;
}