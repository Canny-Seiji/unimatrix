package com.unimatrix;

import org.junit.Test;
public class FeatureRegressionTest {
    @Test public void explicitPlanningAndConstraints() throws Exception {
        com.unimatrix.service.FeatureRegressionChecks.main(new String[]{"target/pdf-check.pdf"});
    }
    @Test public void gridEditingAndFilteredSave() throws Exception {
        com.unimatrix.view.GridRegressionChecks.main(new String[0]);
    }
}
