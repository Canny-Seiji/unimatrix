package com.unimatrix;

import org.junit.Test;

public class TrackRegressionTest {
    @Test public void trackAndTermValidation() {
        com.unimatrix.service.TrackRegressionChecks.main(new String[0]);
    }
    @Test public void manageDataDropdowns() throws Exception {
        com.unimatrix.view.ManageDataTrackChecks.main(new String[0]);
    }
    @Test public void mysqlMigrationAndForeignKeys() throws Exception {
        org.junit.Assume.assumeTrue("Set UNIMATRIX_TRACK_TEST_DB_URL to opt in to MySQL checks.",
                System.getenv("UNIMATRIX_TRACK_TEST_DB_URL")!=null);
        com.unimatrix.service.TrackDatabaseChecks.main(new String[0]);
    }
}
