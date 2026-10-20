package com.unimatrix.view.components;

import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import java.awt.*;

/** Metric tile with loading ("—") and error states. */
public class StatCard extends Card {

    private final JLabel valueLabel = new JLabel("—");

    public StatCard(String caption) {
        super(new BorderLayout(0, 6));
        JLabel cap = UniMatrixTheme.muted(caption);
        valueLabel.setFont(UniMatrixTheme.font(Font.BOLD, 30f));
        valueLabel.setForeground(UniMatrixTheme.TEXT);
        add(cap, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setLoading() {
        valueLabel.setForeground(UniMatrixTheme.TEXT_MUTED);
        valueLabel.setText("—");
    }

    public void setValue(int value) {
        valueLabel.setForeground(UniMatrixTheme.TEXT);
        valueLabel.setText(String.valueOf(value));
    }

    public void setError() {
        valueLabel.setForeground(UniMatrixTheme.DANGER);
        valueLabel.setText("n/a");
    }
}