package com.unimatrix.view.components;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JLabel;

/** Circular presentation of the existing account icon, without changing its source. */
public final class CircularAvatarLabel extends JLabel {
    public CircularAvatarLabel(Icon icon) {
        super(icon);
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        BufferedImage content = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = content.createGraphics();
        super.paintComponent(imageGraphics);
        imageGraphics.dispose();

        BufferedImage circle = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskGraphics = circle.createGraphics();
        maskGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int diameter = Math.min(width, height);
        maskGraphics.fillOval((width - diameter) / 2, (height - diameter) / 2, diameter, diameter);
        maskGraphics.setComposite(AlphaComposite.SrcIn);
        maskGraphics.drawImage(content, 0, 0, null);
        maskGraphics.dispose();
        graphics.drawImage(circle, 0, 0, null);
    }
}
