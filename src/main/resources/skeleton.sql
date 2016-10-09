CREATE DATABASE twitter;
USE twitter;
show databases;
create table tweets(
	id INT AUTO_INCREMENT NOT NULL PRIMARY KEY,
  tweet_id INT NOT NULL,
  user_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
	tweet_text TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
	created_at varchar(255) NOT NULL
)CHARACTER SET utf8mb4 COLLATE utf8mb4_bin ENGINE=InnoDB;

# To support Emoji CHARACTER SET utf8mb4 COLLATE utf8mb4_bin
# Reference : https://mathiasbynens.be/notes/mysql-utf8mb4

CREATE USER 'twitter'@'localhost' IDENTIFIED BY 'strong_passw0rd';
GRANT ALL PRIVILEGES ON *.* TO 'twitter'@'localhost' WITH GRANT OPTION;

# execute "ALTER TABLE tweets CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";