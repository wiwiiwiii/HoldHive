CREATE DATABASE IF NOT EXISTS holdhive
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'holdhive'@'localhost' IDENTIFIED BY 'holdhive';
CREATE USER IF NOT EXISTS 'holdhive'@'127.0.0.1' IDENTIFIED BY 'holdhive';

GRANT ALL PRIVILEGES ON holdhive.* TO 'holdhive'@'localhost';
GRANT ALL PRIVILEGES ON holdhive.* TO 'holdhive'@'127.0.0.1';

FLUSH PRIVILEGES;
