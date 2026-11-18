# UniMatrix

**A desktop examination scheduling and management system built with Java Swing and MySQL.**

UniMatrix helps academic staff organize exam schedules, assign rooms and faculty, and manage course and block-section records. Users can enter exams manually or generate a draft from specific exam requirements, review schedules in a weekly calendar, and export them to PDF.

## Features

| Feature | What it does |
| --- | --- |
| Dashboard | Displays database statistics and a weekly examination calendar with week navigation. |
| Manage Data | Adds, edits, searches, and deletes rooms, courses, block sections, and faculty records. Supports undoing unsaved changes. |
| Manual scheduling | Assigns a course, room, block section, faculty member, date, start time, and end time to each exam. Includes filtering, search, and saving an existing schedule as a new copy. |
| Automatic scheduling | Builds a draft from course/block/faculty requirements, exam lengths, date limits, and daily hours. Selects dates, times, and rooms subject to the implemented rules. |
| Track and term matching | Filters course and block-section choices using a shared track-and-term catalog, including CPE, CSSE, ECE, and General entries. |
| Schedule organization | Creates folders, renames or moves schedules and folders, and searches displayed schedule entries. |
| Recovery | Keeps saved schedule revisions and provides history restoration, trash recovery, and permanent deletion. |
| PDF export | Exports a selected schedule to a PDF file. |
| Accounts | Provides sign-in, account creation, and account settings, including profile pictures for user-created accounts. |

## Requirements

| Requirement | Details |
| --- | --- |
| Java Development Kit | JDK 21; the Maven project targets Java 21. |
| Apache Maven | Needed to resolve dependencies, compile the source, and run the test suite. |
| MySQL Server | MySQL 8.0.16 or later, as required by the supplied schema and its enforced check constraints. |
| Desktop environment | Required to display the Swing application. |
| Database access | A MySQL user with access to the application database. Setup requires schema-creation privileges; the first compatibility check also creates the accounts table. |

The project declares these dependencies in `pom.xml`: MySQL Connector/J **8.3.0**, FlatLaf **3.4**, and JUnit **4.13.2** for tests. Maven downloads them automatically when they are not already cached.

## Installation and setup

### 1. Open the project

Extract the program archive and open the `unimatrix` folder containing `pom.xml`. Run the following commands from that folder, or import it as an existing Maven project in your Java IDE.

Confirm that Java and Maven use the intended JDK:

```sh
java -version
mvn -version
```

### 2. Prepare the database

Start MySQL Server and choose the setup route that matches your database.

| Database state | SQL file to run |
| --- | --- |
| New installation with no existing application tables | `database/001_full_setup_v4.sql` |
| Existing UniMatrix version-3 database | `database/004_track_terms.sql` |

For a new installation, open `001_full_setup_v4.sql` in MySQL Workbench and execute the complete script. It creates `unimatrix_db`, its application tables, and the initial track-and-term catalog. Rooms, courses, sections, and faculty must then be added through the application.

Alternatively, start the MySQL command-line client from the project folder:

```sh
mysql -u root -p
```

Then run:

```sql
SOURCE database/001_full_setup_v4.sql;
```

For an existing version-3 database, back it up and close UniMatrix before executing `database/004_track_terms.sql`. This migration converts legacy track labels into shared track-and-term records while retaining existing courses, sections, and schedules. Do not run the full setup script over an existing installation.

Confirm the schema version after setup or migration:

```sql
USE unimatrix_db;
SELECT MAX(version) AS schema_version FROM schema_version;
```

The result must be **4**.

The other SQL files are legacy resources:

| File | Purpose |
| --- | --- |
| `database/001_full_setup_v3.sql` | Older schema used as a migration starting point and by migration tests. New installations should use version 4. |
| `database/unimatrix_demo_data_v3.sql` | Legacy version-3 database dump containing sample records and `DROP TABLE` statements. Use only in a separate demo environment, then apply migration `004_track_terms.sql`. It is not an extra seed script for an existing version-4 installation. |

### 3. Configure the database connection

The built-in connection defaults in `src/main/java/com/unimatrix/dao/DatabaseConnection.java` are:

| Setting | Default value |
| --- | --- |
| JDBC URL | `jdbc:mysql://localhost:3306/unimatrix_db` |
| MySQL username | `root` |
| MySQL password | `admin123` |

You can override these defaults without editing source code:

| Environment variable | Equivalent Java system property |
| --- | --- |
| `UNIMATRIX_DB_URL` | `unimatrix.db.url` |
| `UNIMATRIX_DB_USER` | `unimatrix.db.user` |
| `UNIMATRIX_DB_PASSWORD` | `unimatrix.db.password` |

Java system properties take precedence over environment variables, which take precedence over the built-in defaults. There is no automatic `.env` file loader.

For example, in Windows PowerShell, replace the values below with your MySQL credentials before launching the application from the same terminal:

```powershell
$env:UNIMATRIX_DB_URL = "jdbc:mysql://localhost:3306/unimatrix_db"
$env:UNIMATRIX_DB_USER = "your_mysql_username"
$env:UNIMATRIX_DB_PASSWORD = "your_mysql_password"
```

On macOS or Linux:

```sh
export UNIMATRIX_DB_URL='jdbc:mysql://localhost:3306/unimatrix_db'
export UNIMATRIX_DB_USER='your_mysql_username'
export UNIMATRIX_DB_PASSWORD='your_mysql_password'
```

For IDE launches, set these environment variables in the run configuration or supply the equivalent `-D` options as JVM arguments.

### 4. Build and launch

Compile the application and copy its runtime dependencies:

```sh
mvn clean compile dependency:copy-dependencies
```

Launch on Windows:

```powershell
java -cp "target/classes;target/dependency/*" com.unimatrix.App
```

Launch on macOS or Linux:

```sh
java -cp "target/classes:target/dependency/*" com.unimatrix.App
```

In a Java IDE, select **JDK 21**, load the Maven dependencies, and run `com.unimatrix.App`. Set the working directory to the project folder so profile-picture paths resolve consistently.

The supplied Maven configuration does not create an executable JAR containing all dependencies. Use the classpath launch above or the IDE's run command.

### 5. Sign in

The built-in application login is:

| Field | Value |
| --- | --- |
| Username | `admin` |
| Password | `unimatrix123` |

These are application credentials, separate from the MySQL credentials.

Sign in once with the built-in account after database setup. The sign-in compatibility check creates the `accounts` table when needed. You can then use **Create Account** on the login screen to register another user. Usernames must contain 3–30 letters, numbers, dots, dashes, or underscores; passwords must contain at least six characters.

The built-in administrator login is hardcoded. Its credentials cannot be changed or removed through the Account page. User-created passwords are stored using salted PBKDF2-HMAC-SHA256 hashes.

## Using UniMatrix

### Prepare reference records

1. Open **Manage Data**.
2. Add rooms and their capacities, courses, block sections and student counts, and faculty members.
3. Select appropriate track-and-term labels for courses and sections.
4. Click **Save Changes** in each edited tab.

### Create a manual schedule

1. Open **Manage Schedules** and select a destination folder, if desired.
2. Click **New Manual Schedule**.
3. Enter a schedule name, examination period, academic year, and semester.
4. Complete one row per exam: course, room, block section, faculty, date, start time, and end time.
5. Click **Save Schedule**. Correct any highlighted or reported errors before saving again.

Dates use `YYYY-MM-DD`. Times accept values such as `08:30` or `2:30 PM`. **Change Destination** selects another folder. When editing an existing schedule, **Save As New** creates a separate copy.

### Generate a schedule automatically

1. Open **Manage Schedules** and click **New Schedule** to open **Auto-Generate Config**.
2. Enter the schedule metadata, first and last exam dates, and daily start and end times.
3. Add one requirement row for every course/block exam, selecting its faculty member and exam length in minutes.
4. Click **Generate Draft**.
5. Review the saved draft and use **Open / Edit** to make any needed adjustments.

The generator selects rooms and time placements for the requirements you provide. Faculty members are selected in the requirement rows. If any requirement cannot fit, generation stops without saving a partial schedule.

### Organize, recover, and export

Use **New Folder** and **Rename / Move** to organize saved schedules. **History / Undo** restores an earlier saved revision. **Delete** moves an item to the trash; **Trash / Restore** provides recovery or permanent deletion. A recovered schedule with resource conflicts may have conflicting exam entries cleared for correction.

To export, select one saved schedule and click **Export PDF**. To view another week on the dashboard calendar, use its previous-week and next-week controls.

## Scheduling rules and scope

The current implementation checks the following rules when generating or saving schedules:

- Exam dates must fall on **Monday through Saturday**.
- Exam times must stay between **7:00 AM and 9:00 PM**, with the end after the start on the same date.
- A schedule may contain at most **three exams on one date**, counted across all its sections.
- Exams sharing a room or block section require at least **60 minutes** between them. Checks also consider exams in other active, non-deleted schedules, including drafts.
- Room capacity must accommodate the block section's student count.
- A course/block pair may appear only once in the same schedule.
- Course and section tracks and terms must be compatible. A General course with a specified term can serve different tracks in that same term; the legacy bare `General` course label remains unrestricted by track or term.
- Faculty availability and excluded dates are read from the `faculty_availability` and `excluded_dates` database tables. Date-specific availability replaces weekly availability for that date. These tables have no dedicated management screen in the current application.

Automatic exam lengths must be **30-minute multiples from 30 to 840 minutes**. The generator tries placements in requirement order and does not guarantee an optimal arrangement; a different requirement order or wider date range may produce a different result.

Resource conflict checks cover **rooms and block sections**. Assigning the same faculty member to overlapping exams is not independently rejected solely because the faculty ID matches. User-created accounts also share the same application workspace; the current schema does not separate schedules by account.

## Project structure

| Path | Contents |
| --- | --- |
| `pom.xml` | Maven project settings, dependencies, and Java target. |
| `src/main/java/com/unimatrix/App.java` | Main entry point; opens the login screen. |
| `src/main/java/com/unimatrix/model/` | Courses, rooms, faculty, sections, track terms, and schedule models. |
| `src/main/java/com/unimatrix/dao/` | Database connections, SQL access, schema compatibility, and transaction helpers. |
| `src/main/java/com/unimatrix/service/` | Planning, validation, schedule editing, history, workspace operations, PDF export, and profile-picture handling. |
| `src/main/java/com/unimatrix/view/` | Swing screens, calendar, editor panels, reusable components, and theme. |
| `src/test/java/` | JUnit tests and additional regression/integration check classes. |
| `database/` | Database setup scripts, migration, and legacy demo dump. |
| `profilepics/` | Local profile-picture files. |
| `target/` | Generated build output. |

## Tests

Run the JUnit suite from the project folder:

```sh
mvn test
```

The suite includes checks for planning constraints, PDF generation, table editing, track matching, and track dropdowns. The MySQL migration test is skipped unless `UNIMATRIX_TRACK_TEST_DB_URL` is set. To enable it, supply an **empty, dedicated database named `unimatrix_track_test`**, along with `UNIMATRIX_TRACK_TEST_DB_USER` and `UNIMATRIX_TRACK_TEST_DB_PASSWORD`. The test creates tables and sample records; it must not use the application database.

The separate `MySqlIntegrationChecks` utility references older SQL files absent from this archive and is not a ready-to-run integration check for this distribution.

## Troubleshooting

| Problem | What to check |
| --- | --- |
| Java release or class-version error | Ensure both the project and Maven use JDK 21. Check `java -version` and `mvn -version`. |
| Cannot connect to MySQL | Start MySQL Server and verify the JDBC host, port, database name, username, and password. |
| Access denied | Check the MySQL credentials and database privileges. The application login does not configure MySQL access. |
| Database compatibility check fails | Verify that the schema version is 4 and that the appropriate setup or migration script completed. |
| Account creation reports a missing `accounts` table | Complete one sign-in attempt with the built-in administrator after database setup so the compatibility check can create the table. |
| Courses, rooms, sections, or faculty choices are empty | Save reference records in Manage Data, then reload or refresh the editor choices. |
| A course or section is absent from a dropdown | Check its track-and-term compatibility with the other selected value. |
| Automatic generation cannot place an exam | Check room capacity, dates, daily hours, the three-exam daily limit, required gaps, existing schedules, and availability restrictions. |
| A save reports that data changed elsewhere | Refresh or reopen the affected record or schedule before retrying the edit. |
| Dependencies cannot be found at launch | Run the build/dependency command and use the correct platform-specific classpath separator. |

## Data storage and portability

UniMatrix uses a **separate MySQL database**. The SQL scripts define and populate database structures; they do not bundle or start MySQL Server. Copying the project folder or ZIP alone does not copy the live database.

To move the complete installation, export the MySQL database, import it on the destination server, update the connection settings, and copy `profilepics/`. Keep the project working directory consistent or update stored profile-picture paths when moving files. PDF exports are saved to the location selected during export.
