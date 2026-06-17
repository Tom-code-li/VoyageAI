CREATE DATABASE IF NOT EXISTS travel_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE travel_assistant;

DROP TABLE IF EXISTS itinerary_detail;
DROP TABLE IF EXISTS itinerary_day_plan;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS itinerary;
DROP TABLE IF EXISTS attraction;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attraction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  city VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  image_url VARCHAR(500),
  reference_count INT NOT NULL DEFAULT 0,
  play_time DECIMAL(4,1) DEFAULT 2.0,
  longitude DECIMAL(10, 6) COMMENT '经度',
  latitude DECIMAL(10, 6) COMMENT '纬度',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_city_name (city, name)
);

CREATE TABLE itinerary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  city VARCHAR(50) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_itinerary_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE itinerary_day_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  itinerary_id BIGINT NOT NULL,
  day_number INT NOT NULL,
  route_summary TEXT,
  route_distance VARCHAR(50),
  route_duration VARCHAR(50),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_itinerary_day (itinerary_id, day_number),
  CONSTRAINT fk_day_plan_itinerary FOREIGN KEY (itinerary_id) REFERENCES itinerary (id) ON DELETE CASCADE
);

CREATE TABLE itinerary_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  itinerary_id BIGINT NOT NULL,
  attraction_id BIGINT NOT NULL,
  day_number INT NOT NULL,
  sort_order INT NOT NULL,
  CONSTRAINT fk_detail_itinerary FOREIGN KEY (itinerary_id) REFERENCES itinerary (id) ON DELETE CASCADE,
  CONSTRAINT fk_detail_attraction FOREIGN KEY (attraction_id) REFERENCES attraction (id)
);

CREATE TABLE chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  itinerary_id BIGINT NOT NULL,
  sender VARCHAR(20) NOT NULL COMMENT 'USER or BOT',
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_chat_itinerary FOREIGN KEY (itinerary_id) REFERENCES itinerary (id) ON DELETE CASCADE
);
