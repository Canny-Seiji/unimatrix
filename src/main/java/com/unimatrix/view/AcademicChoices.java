package com.unimatrix.view;

import java.util.List;
import java.util.Locale;
import javax.swing.JComboBox;

/** Fixed schedule metadata choices shared by manual and automatic entry. */
final class AcademicChoices {
    private static final String SELECT = "Select...";
    private static final List<String> YEARS = List.of(
            "2025-2026", "2026-2027", "2027-2028",
            "2028-2029", "2029-2030", "2030-2031");
    private static final List<String> SEMESTERS = List.of("1st", "2nd", "3rd");

    private AcademicChoices() { }

    static JComboBox<String> years() {
        JComboBox<String> box = choices(YEARS);
        box.setSelectedItem("2026-2027");
        return box;
    }

    static JComboBox<String> semesters() {
        return choices(SEMESTERS);
    }

    private static JComboBox<String> choices(List<String> values) {
        JComboBox<String> box = new JComboBox<>();
        box.addItem(SELECT);
        values.forEach(box::addItem);
        box.setEditable(false);
        return box;
    }

    static void loadYear(JComboBox<String> box, String saved) {
        String value = saved == null ? "" : saved.trim();
        box.setSelectedItem(YEARS.contains(value) ? value : SELECT);
    }

    static void loadSemester(JComboBox<String> box, String saved) {
        String value = saved == null ? "" : saved.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        String normalized = switch (value) {
            case "1st", "first", "1st semester", "first semester", "1st term", "first term" -> "1st";
            case "2nd", "second", "2nd semester", "second semester", "2nd term", "second term" -> "2nd";
            case "3rd", "third", "3rd semester", "third semester", "3rd term", "third term" -> "3rd";
            default -> SELECT;
        };
        box.setSelectedItem(normalized);
    }

    static String requireYear(JComboBox<String> box) {
        Object value = box.getSelectedItem();
        if (!(value instanceof String text) || !YEARS.contains(text))
            throw new IllegalArgumentException("Select an academic year from 2025-2026 through 2030-2031.");
        return (String) value;
    }

    static String requireSemester(JComboBox<String> box) {
        Object value = box.getSelectedItem();
        if (!(value instanceof String text) || !SEMESTERS.contains(text))
            throw new IllegalArgumentException("Select a semester: 1st, 2nd, or 3rd.");
        return (String) value;
    }
}
