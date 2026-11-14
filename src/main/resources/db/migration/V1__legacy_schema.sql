
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `administration_audits` (
  `id` binary(16) NOT NULL,
  `action` varchar(40) NOT NULL,
  `actor_id` bigint NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `target_user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `completion_date` datetime(6) DEFAULT NULL,
  `priority` enum('HIGH','LOW','MEDIUM') NOT NULL,
  `fk_incident_id` bigint NOT NULL,
  `assigned_to_user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK250e5ly5rog8gkfys61te354k` (`fk_incident_id`),
  KEY `FK6cg6ajs2cq2d2q2fkr40ka2vd` (`assigned_to_user_id`),
  CONSTRAINT `FK6cg6ajs2cq2d2q2fkr40ka2vd` FOREIGN KEY (`assigned_to_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKbqtw56bfuxp5tqnn96fx13a9y` FOREIGN KEY (`fk_incident_id`) REFERENCES `incidents` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `companies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `active` bit(1) NOT NULL,
  `name` varchar(120) NOT NULL,
  `registration_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `company_invitations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumed_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `recipient_email` varchar(254) NOT NULL,
  `token_digest` varchar(64) NOT NULL,
  `version` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK28ibv5yxhny9qxabhnyxs1661` (`token_digest`),
  KEY `FKpvcecknxr1udxf7avngt1g26h` (`company_id`),
  CONSTRAINT `FKpvcecknxr1udxf7avngt1g26h` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incidents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(4000) NOT NULL,
  `document_url` varchar(2048) DEFAULT NULL,
  `location` varchar(500) NOT NULL,
  `status` enum('ASSIGNED','CLOSED','IN_PROGRESS','OPEN') NOT NULL,
  `title` varchar(120) NOT NULL,
  `version` bigint NOT NULL,
  `fk_company_id` bigint NOT NULL,
  `created_by_user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKies63s84s70pc1toyili2a75c` (`fk_company_id`),
  KEY `FKe7gfq3wson3k6evmvt3jrh6nt` (`created_by_user_id`),
  CONSTRAINT `FKe7gfq3wson3k6evmvt3jrh6nt` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKies63s84s70pc1toyili2a75c` FOREIGN KEY (`fk_company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` binary(16) NOT NULL,
  `body` varchar(5000) DEFAULT NULL,
  `company_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `recipient_id` varchar(255) DEFAULT NULL,
  `status` enum('PENDING','SENT') DEFAULT NULL,
  `subject` varchar(180) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` enum('ADMIN','EMPLOYER','WORKER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_sessions` (
  `id` binary(16) NOT NULL,
  `company_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `roles_snapshot` varchar(64) NOT NULL,
  `security_version` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_session_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `email` varchar(254) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `phone_number` varchar(32) DEFAULT NULL,
  `profile_picture_url` varchar(255) DEFAULT NULL,
  `security_version` bigint NOT NULL,
  `fk_company_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FK7xao1b4rc3x9dxswmnrfmkg36` (`fk_company_id`),
  CONSTRAINT `FK7xao1b4rc3x9dxswmnrfmkg36` FOREIGN KEY (`fk_company_id`) REFERENCES `companies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

