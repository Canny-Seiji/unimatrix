package com.unimatrix.model;

/** Display names shared by the track catalog and scheduling rules. */
public enum AcademicTerm {
    FIRST(1, "1st term"), SECOND(2, "2nd term"), THIRD(3, "3rd term");

    private final int number;
    private final String label;

    AcademicTerm(int number, String label) {
        this.number = number;
        this.label = label;
    }

    public int number() { return number; }
    @Override public String toString() { return label; }
}
