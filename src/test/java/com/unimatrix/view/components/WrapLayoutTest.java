package com.unimatrix.view.components;

import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WrapLayoutTest {

    @Test
    public void preferredHeightReflowsWhenContainerWidthChanges() {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 5));
        JPanel[] items = new JPanel[3];
        for (int i = 0; i < 3; i++) {
            items[i] = new JPanel();
            items[i].setPreferredSize(new Dimension(100, 20));
            panel.add(items[i]);
        }

        panel.setSize(400, 200);
        panel.doLayout();
        int wideHeight = panel.getPreferredSize().height;
        int wideLastItemY = items[2].getY();

        panel.setSize(230, 200);
        panel.doLayout();
        int narrowHeight = panel.getPreferredSize().height;

        assertEquals(30, wideHeight);
        assertEquals(55, narrowHeight);
        assertTrue(items[2].getY() > wideLastItemY);
    }
}
