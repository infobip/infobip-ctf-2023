CREATE DATABASE IF NOT EXISTS myCDN;
USE myCDN;

CREATE TABLE IF NOT EXISTS uploads (
	id INT AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR(30) NOT NULL,
	path VARCHAR(255) NOT NULL
);
DELETE FROM uploads;
INSERT INTO uploads (name, path) VALUES 
	('white.png', 'uploads/white.png'),
	('gray.png', 'uploads/gray.png');

CREATE TABLE IF NOT EXISTS cdnsecrets (
	content VARCHAR(255) NOT NULL
);
DELETE FROM cdnsecrets;
INSERT INTO cdnsecrets VALUES 
	('ibctf{l4y3rs_up0n-L4Y3R-0f-w1nd_4nd_r41n-c4nn0t_s3p4r4t3-UZz}');

CREATE USER 'user'@'localhost' IDENTIFIED BY 'Strong3stP4ssw0rd-H4ckTh3Pl4n3t';
GRANT ALL PRIVILEGES ON myCDN.uploads TO 'user'@'localhost';
GRANT SELECT ON myCDN.cdnsecrets TO 'user'@'localhost';
FLUSH PRIVILEGES;
DROP USER 'root'@'localhost';
DROP USER 'mysql'@'localhost';
