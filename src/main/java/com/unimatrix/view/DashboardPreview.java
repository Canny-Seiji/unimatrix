package com.unimatrix.view;

import com.unimatrix.view.theme.UniMatrixTheme;

import java.awt.EventQueue;

/** Temporary launcher with stub data. Replace the lambda with a DAO-backed provider later. */
public class DashboardPreview {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            UniMatrixTheme.install();
            DashboardDataProvider stub = () -> {
                Thread.sleep(400); // simulates a DB round-trip so the loading state is visible
                return new DashboardStats(42, 118, 76, 210, null);
            };
            new MainDashboardView("admin", stub, () -> System.out.println("logged out")).setVisible(true);
        });
    }
}