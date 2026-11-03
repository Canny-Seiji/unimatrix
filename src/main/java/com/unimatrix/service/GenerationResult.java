package com.unimatrix.service;

import java.util.List;

/** Outcome of a generation run. error != null means nothing was saved. */
public record GenerationResult(int totalCourses, int scheduled, List<String> skipped, String error) {

    public boolean ok() { return error == null; }

    public static GenerationResult failed(String message) {
        return new GenerationResult(0, 0, List.of(), message);
    }
}