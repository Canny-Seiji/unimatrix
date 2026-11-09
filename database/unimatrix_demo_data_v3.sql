-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: unimatrix_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `block_sections`
--

DROP TABLE IF EXISTS `block_sections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `block_sections` (
  `id` int NOT NULL AUTO_INCREMENT,
  `section_name` varchar(50) NOT NULL,
  `student_count` int NOT NULL,
  `track` varchar(50) NOT NULL DEFAULT 'General',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `block_sections`
--

LOCK TABLES `block_sections` WRITE;
/*!40000 ALTER TABLE `block_sections` DISABLE KEYS */;
INSERT INTO `block_sections` VALUES (1,'TN01',30,'General'),(2,'TN02',35,'General'),(3,'TN03',40,'General'),(4,'TN04',40,'General'),(5,'TN05',35,'General'),(7,'TN06',29,'General'),(8,'TN21',40,'General'),(9,'TN22',20,'General'),(10,'TN23',15,'General'),(11,'TN24',40,'General'),(12,'TN25',25,'General'),(13,'TN26',32,'General');
/*!40000 ALTER TABLE `block_sections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` int NOT NULL AUTO_INCREMENT,
  `course_code` varchar(20) NOT NULL,
  `course_title` varchar(100) NOT NULL,
  `duration_hours` float NOT NULL DEFAULT '0',
  `track` varchar(50) NOT NULL DEFAULT 'General',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `courses`
--

LOCK TABLES `courses` WRITE;
/*!40000 ALTER TABLE `courses` DISABLE KEYS */;
INSERT INTO `courses` VALUES (1,'CCS0021','Information Management (LEC)',0,'General'),(2,'CCS0021L','Information Management (LAB)',0,'General'),(3,'CS0001','Discrete Structures 1',0,'General'),(4,'CS0003','Computer Systems & Architecture (LEC)',0,'General'),(5,'CS0003L','Computer Systems & Architecture (LAB)',0,'General'),(6,'CS0070','Object Oriented Programming (LEC)',0,'General'),(7,'CS0070L','Object Oriented Programming (LAB)',0,'General'),(8,'GED0021','Specialized English Program 2',0,'General'),(9,'GED0075','Linear Algebra',0,'General'),(10,'GED0081','College Physics 1 (LEC)',0,'General'),(11,'GED0081L','College Physics 1 (LAB)',0,'General'),(12,'CCS0001','Introduction to Computing (LEC)',0,'General'),(13,'CCS0001L','Introduction to Computing (LAB)',0,'General'),(14,'CCS0003','Computer Programming (LEC)',0,'General'),(15,'CCS0003L','Computer Programming (Lab',0,'General'),(16,'GED0004','Physical Education 1',0,'General'),(17,'GED0006','Personal and Professional Effectiveness',0,'General'),(18,'GED0009','Readings in Philippine History',0,'General'),(19,'GED0011','Science, Technology and Society',0,'General');
/*!40000 ALTER TABLE `courses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exam_schedules`
--

DROP TABLE IF EXISTS `exam_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exam_schedules` (
  `id` int NOT NULL AUTO_INCREMENT,
  `course_id` int DEFAULT NULL,
  `room_id` int DEFAULT NULL,
  `block_section_id` int DEFAULT NULL,
  `faculty_id` int DEFAULT NULL,
  `exam_date` date DEFAULT NULL,
  `time_slot` varchar(50) DEFAULT NULL,
  `start_time` time DEFAULT NULL,
  `end_time` time DEFAULT NULL,
  `schedule_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `course_id` (`course_id`),
  KEY `room_id` (`room_id`),
  KEY `block_section_id` (`block_section_id`),
  KEY `faculty_id` (`faculty_id`),
  KEY `fk_exam_schedule_file` (`schedule_id`),
  KEY `ix_exam_scope_date` (`schedule_id`,`exam_date`),
  CONSTRAINT `exam_schedules_ibfk_1` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `exam_schedules_ibfk_2` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `exam_schedules_ibfk_3` FOREIGN KEY (`block_section_id`) REFERENCES `block_sections` (`id`),
  CONSTRAINT `exam_schedules_ibfk_4` FOREIGN KEY (`faculty_id`) REFERENCES `faculty` (`id`),
  CONSTRAINT `fk_exam_schedule_file` FOREIGN KEY (`schedule_id`) REFERENCES `schedule_files` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=87 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exam_schedules`
--

LOCK TABLES `exam_schedules` WRITE;
/*!40000 ALTER TABLE `exam_schedules` DISABLE KEYS */;
INSERT INTO `exam_schedules` VALUES (49,12,5,3,8,'2026-09-22','07:00 - 10:00','07:00:00','10:00:00',6),(50,18,6,4,12,'2026-09-23','07:00 - 09:00','07:00:00','09:00:00',6),(51,19,6,4,13,'2026-09-23','10:00 - 11:30','10:00:00','11:30:00',6),(52,13,5,3,8,'2026-09-23','12:30 - 15:30','12:30:00','15:30:00',6),(53,14,5,3,9,'2026-09-24','13:00 - 15:30','13:00:00','15:30:00',6),(54,15,5,3,9,'2026-09-25','15:30 - 18:00','15:30:00','18:00:00',6),(55,16,5,3,10,'2026-09-25','19:00 - 21:00','19:00:00','21:00:00',6),(56,17,5,3,11,'2026-09-29','07:00 - 08:30','07:00:00','08:30:00',6),(57,12,5,3,8,'2026-09-08','10:00 - 13:00','10:00:00','13:00:00',7),(58,18,7,3,12,'2026-11-04','15:00 - 17:00','15:00:00','17:00:00',7),(59,19,8,3,13,'2026-11-06','09:00 - 10:00','09:00:00','10:00:00',7),(60,13,5,3,8,'2026-11-04','10:00 - 13:00','10:00:00','13:00:00',7),(61,14,6,3,9,'2026-11-06','11:00 - 12:00','11:00:00','12:00:00',7),(62,15,6,3,9,'2026-11-05','10:00 - 12:30','10:00:00','12:30:00',7),(63,16,9,3,10,'2026-11-07','07:00 - 09:00','07:00:00','09:00:00',7),(64,17,5,3,11,'2026-11-06','13:00 - 14:30','13:00:00','14:30:00',7),(76,1,5,11,1,'2026-11-10','07:00 - 09:30','07:00:00','09:30:00',8),(77,2,5,11,1,'2026-11-10','10:30 - 13:30','10:30:00','13:30:00',8),(78,3,5,11,2,'2026-11-10','14:30 - 16:30','14:30:00','16:30:00',8),(79,4,5,11,3,'2026-11-11','07:00 - 09:30','07:00:00','09:30:00',8),(80,5,5,11,3,'2026-11-11','10:30 - 13:30','10:30:00','13:30:00',8),(81,6,5,11,4,'2026-11-11','14:30 - 17:00','14:30:00','17:00:00',8),(82,7,5,11,4,'2026-11-12','07:00 - 10:00','07:00:00','10:00:00',8),(83,8,5,11,5,'2026-11-12','11:00 - 12:30','11:00:00','12:30:00',8),(84,9,5,11,6,'2026-11-12','13:30 - 15:30','13:30:00','15:30:00',8),(85,10,5,11,7,'2026-11-13','07:00 - 09:30','07:00:00','09:30:00',8),(86,11,5,11,7,'2026-11-13','10:30 - 13:30','10:30:00','13:30:00',8);
/*!40000 ALTER TABLE `exam_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `excluded_dates`
--

DROP TABLE IF EXISTS `excluded_dates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `excluded_dates` (
  `exam_date` date NOT NULL,
  `reason` varchar(200) NOT NULL,
  PRIMARY KEY (`exam_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `excluded_dates`
--

LOCK TABLES `excluded_dates` WRITE;
/*!40000 ALTER TABLE `excluded_dates` DISABLE KEYS */;
/*!40000 ALTER TABLE `excluded_dates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faculty`
--

DROP TABLE IF EXISTS `faculty`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faculty` (
  `id` int NOT NULL AUTO_INCREMENT,
  `faculty_name` varchar(100) NOT NULL,
  `department` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faculty`
--

LOCK TABLES `faculty` WRITE;
/*!40000 ALTER TABLE `faculty` DISABLE KEYS */;
INSERT INTO `faculty` VALUES (1,'Alejandro Legaspi','College of Computer Studies & Multimedia Arts'),(2,'Marilyn Sanchez','College of Computer Studies & Multimedia Arts'),(3,'Hadji Tejuco','College of Computer Studies & Multimedia Arts'),(4,'Anthony Aquino','College of Computer Studies & Multimedia Arts'),(5,'Allan Salanguste','General Education'),(6,'Dyn Paulo Dasallas','Mathematics & Physical Sciences Department'),(7,'Noel Sinsioco','Mathematics & Physical Sciences Department'),(8,'Jericho Menguito','College of Computer Studies & Multimedia Arts'),(9,'Demeke Bayyou','College of Computer Studies & Multimedia Arts'),(10,'Gelli Zuleta','General Education'),(11,'Julius Cabasag','College of Computer Studies & Multimedia Arts'),(12,'Zyra De Guzman','General Education'),(13,'Ashley Mae Cuenca','General Education');
/*!40000 ALTER TABLE `faculty` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faculty_availability`
--

DROP TABLE IF EXISTS `faculty_availability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faculty_availability` (
  `id` int NOT NULL AUTO_INCREMENT,
  `faculty_id` int NOT NULL,
  `weekday` int DEFAULT NULL,
  `exception_date` date DEFAULT NULL,
  `available` tinyint(1) NOT NULL DEFAULT '1',
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `faculty_id` (`faculty_id`),
  CONSTRAINT `faculty_availability_ibfk_1` FOREIGN KEY (`faculty_id`) REFERENCES `faculty` (`id`),
  CONSTRAINT `faculty_availability_chk_1` CHECK ((((`weekday` between 1 and 6) and (`exception_date` is null)) or ((`weekday` is null) and (`exception_date` is not null)))),
  CONSTRAINT `faculty_availability_chk_2` CHECK ((`end_time` > `start_time`))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faculty_availability`
--

LOCK TABLES `faculty_availability` WRITE;
/*!40000 ALTER TABLE `faculty_availability` DISABLE KEYS */;
/*!40000 ALTER TABLE `faculty_availability` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rooms`
--

DROP TABLE IF EXISTS `rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rooms` (
  `id` int NOT NULL AUTO_INCREMENT,
  `room_name` varchar(50) NOT NULL,
  `building` varchar(50) NOT NULL,
  `capacity` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rooms`
--

LOCK TABLES `rooms` WRITE;
/*!40000 ALTER TABLE `rooms` DISABLE KEYS */;
INSERT INTO `rooms` VALUES (5,'F401','FEU Tech Building',40),(6,'F402','FEU Tech Building',40),(7,'F403','FEU Tech Building',40),(8,'F404','FEU Tech Building',40),(9,'F405','FEU Tech Building',40),(10,'F501','FEU Tech Building',35),(11,'F502','FEU Tech Building',35),(12,'F503','FEU Tech Building',35),(13,'F504','FEU Tech Building',35),(14,'F505','FEU Tech Building',35),(15,'F601','FEU Tech Building',30),(16,'F602','FEU Tech Building',30),(17,'F603','FEU Tech Building',30),(18,'F604','FEU Tech Building',30),(19,'F605','FEU Tech Building',30),(20,'E601','Engineering Building',30),(21,'E602','Engineering Building',35),(22,'E603','Engineering Building',35),(23,'E604','Engineering Building',40),(24,'E605','Engineering Building',40),(25,'E606','Engineering Building',40),(26,'E607','Engineering Building',30),(27,'E608','Engineering Building',35),(28,'E609','Engineering Building',40),(29,'E610','Engineering Building',40),(30,'E701','Engineering Building',30),(31,'E702','Engineering Building',35),(32,'E703','Engineering Building',40),(33,'E704','Engineering Building',40),(34,'E705','Engineering Building',40);
/*!40000 ALTER TABLE `rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_files`
--

DROP TABLE IF EXISTS `schedule_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_files` (
  `id` int NOT NULL AUTO_INCREMENT,
  `folder_id` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `examination_period` varchar(30) NOT NULL DEFAULT 'Other',
  `academic_year` varchar(20) NOT NULL DEFAULT '',
  `semester` varchar(30) NOT NULL DEFAULT '',
  `revision` bigint NOT NULL DEFAULT '0',
  `published` tinyint(1) NOT NULL DEFAULT '0',
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `folder_id` (`folder_id`),
  CONSTRAINT `schedule_files_ibfk_1` FOREIGN KEY (`folder_id`) REFERENCES `schedule_folders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_files`
--

LOCK TABLES `schedule_files` WRITE;
/*!40000 ALTER TABLE `schedule_files` DISABLE KEYS */;
INSERT INTO `schedule_files` VALUES (6,5,'TN03 Midterm Exam','Midterms','SY2627','Term 1',2,0,0,'2026-11-08 03:56:28'),(7,5,'TN03 Finals Exam','Finals','SY2627','Term 1',1,0,0,'2026-11-08 03:57:36'),(8,6,'TN24 Finals Exam','Finals','SY2627','Term 1',1,0,0,'2026-11-08 04:17:25');
/*!40000 ALTER TABLE `schedule_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_folders`
--

DROP TABLE IF EXISTS `schedule_folders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_folders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `parent_id` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `schedule_folders_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `schedule_folders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_folders`
--

LOCK TABLES `schedule_folders` WRITE;
/*!40000 ALTER TABLE `schedule_folders` DISABLE KEYS */;
INSERT INTO `schedule_folders` VALUES (5,NULL,'TN03 Exam Schedule','2026-11-08 03:52:09',0),(6,NULL,'TN24 Exam Schedule','2026-11-08 04:00:30',0);
/*!40000 ALTER TABLE `schedule_folders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_history`
--

DROP TABLE IF EXISTS `schedule_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schedule_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `schedule_id` int NOT NULL,
  `saved_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reason` varchar(100) NOT NULL,
  `snapshot` longblob NOT NULL,
  PRIMARY KEY (`id`),
  KEY `schedule_id` (`schedule_id`),
  CONSTRAINT `schedule_history_ibfk_1` FOREIGN KEY (`schedule_id`) REFERENCES `schedule_files` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_history`
--

LOCK TABLES `schedule_history` WRITE;
/*!40000 ALTER TABLE `schedule_history` DISABLE KEYS */;
INSERT INTO `schedule_history` VALUES (9,6,'2026-11-08 03:57:08','Before edit',_binary '\0TN03 Midterm Exam\0Prelims\0SY2627\0Term 1\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-03\0\r07:00 - 10:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-03\0\r07:00 - 09:00\0\0\0\0\0\0\0\0\0\0\0\0\r\0\n2026-11-03\0\r09:00 - 10:30\0\0\0\r\0\0\0\0\0\0\0\0\0\0\n2026-11-03\0\r10:00 - 13:00\0\0\0\0\0\0\0\0\0\0\0\0	\0\n2026-11-03\0\r13:00 - 15:30\0\0\0\0\0\0\0\0\0\0\0\0	\0\n2026-11-03\0\r15:30 - 18:00\0\0\0\0\0\0\0\0\0\0\0\0\n\0\n2026-11-03\0\r18:00 - 20:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-04\0\r07:00 - 08:30'),(10,6,'2026-11-08 03:59:23','Before edit',_binary '\0TN03 Midterm Exam\0Midterms\0SY2627\0Term 1\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-03\0\r07:00 - 10:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-03\0\r07:00 - 09:00\0\0\0\0\0\0\0\0\0\0\0\0\r\0\n2026-11-03\0\r09:00 - 10:30\0\0\0\r\0\0\0\0\0\0\0\0\0\0\n2026-11-03\0\r10:00 - 13:00\0\0\0\0\0\0\0\0\0\0\0\0	\0\n2026-11-03\0\r13:00 - 15:30\0\0\0\0\0\0\0\0\0\0\0\0	\0\n2026-11-03\0\r15:30 - 18:00\0\0\0\0\0\0\0\0\0\0\0\0\n\0\n2026-11-03\0\r18:00 - 20:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-04\0\r07:00 - 08:30'),(11,7,'2026-11-08 04:12:50','Before edit',_binary '\0TN03 Finals Exam\0Finals\0SY2627\0Term 1\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-05\0\r07:00 - 10:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-05\0\r07:00 - 09:00\0\0\0\0\0\0\0\0\0\0\0\0\r\0\n2026-11-05\0\r09:00 - 10:30\0\0\0\r\0\0\0\0\0\0\0\0\0\0\n2026-11-05\0\r10:00 - 13:00\0\0\0\0\0\0\0\0\0\0\0\0	\0\n2026-11-05\0\r13:00 - 15:30\0\0\0\0\0\0\0\0\0\0\0\0	\0\n2026-11-05\0\r15:30 - 18:00\0\0\0\0\0\0\0\0\0\0\0\0\n\0\n2026-11-05\0\r18:00 - 20:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-06\0\r07:00 - 08:30'),(12,8,'2026-11-08 04:18:18','Before edit',_binary '\0TN24 Finals Exam\0Finals\0SY2627\0Term 1\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-10\0\r07:00 - 09:30\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-10\0\r09:30 - 12:30\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-10\0\r12:30 - 14:30\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-10\0\r14:30 - 17:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-10\0\r17:00 - 20:00\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-11\0\r07:00 - 09:30\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-11\0\r09:30 - 12:30\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-11\0\r12:30 - 14:00\0\0\0	\0\0\0\0\0\0\0\0\0\0\n2026-11-11\0\r14:00 - 16:00\0\0\0\n\0\0\0\0\0\0\0\0\0\0\n2026-11-11\0\r16:00 - 18:30\0\0\0\0\0\0\0\0\0\0\0\0\0\n2026-11-12\0\r07:00 - 10:00');
/*!40000 ALTER TABLE `schedule_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scheduler_write_lock`
--

DROP TABLE IF EXISTS `scheduler_write_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scheduler_write_lock` (
  `id` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scheduler_write_lock`
--

LOCK TABLES `scheduler_write_lock` WRITE;
/*!40000 ALTER TABLE `scheduler_write_lock` DISABLE KEYS */;
INSERT INTO `scheduler_write_lock` VALUES (1);
/*!40000 ALTER TABLE `scheduler_write_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schema_version`
--

DROP TABLE IF EXISTS `schema_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schema_version` (
  `version` int NOT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schema_version`
--

LOCK TABLES `schema_version` WRITE;
/*!40000 ALTER TABLE `schema_version` DISABLE KEYS */;
INSERT INTO `schema_version` VALUES (3);
/*!40000 ALTER TABLE `schema_version` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-08 12:28:55
