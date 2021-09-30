CREATE DATABASE `template_account`;
CREATE USER `template_account`@`%` IDENTIFIED BY 'changeit';
GRANT ALL PRIVILEGES ON `template_account`.* TO `template_account`@`%`;
