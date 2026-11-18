package com.unimatrix.view;

import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;

/** Application artwork for native window title bars and task switchers. */
final class AppBranding {
    private AppBranding() { }

    static List<Image> windowIcons() {
        URL resource = AppBranding.class.getResource("/com/unimatrix/icons/unimatrix-logo.png");
        if (resource == null) return List.of();
        Image source = new ImageIcon(resource).getImage();
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 32, 48, 64, 128, 256}) {
            icons.add(new ImageIcon(source.getScaledInstance(size, size, Image.SCALE_SMOOTH)).getImage());
        }
        return icons;
    }
}
