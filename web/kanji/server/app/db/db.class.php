<?php
require_once(dirname(__FILE__) . '/upload.class.php');

class DB {

	private $server;
	private $username;
	private $password;
	private $database;
	private $conn;
	
	function __construct($server, $username, $password, $database) {
		$this->server = $server;
		$this->username = $username;
		$this->password = $password;
		$this->database = $database;
	}
	
	function getConnection() {
		return $this->conn;
	}
	
	function connect() {
		$this->conn = new PDO("mysql:host=$this->server;dbname=$this->database", $this->username, $this->password);
		$this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
		$this->conn->query('SET NAMES gbk');
	}
	
	function close() {
		$this->conn = null;
	}
	
	function findAll() {
		$this->checkConnection();
		$stmt = $this->conn->prepare('SELECT id, name, path FROM uploads');
		$stmt->setFetchMode(PDO::FETCH_CLASS, 'Upload');
		$stmt->execute();
		return $stmt->fetchAll();
	}
	
	function findByName($name) {
		$this->checkConnection();
		$stmt = $this->conn->prepare('SELECT id, name, path FROM uploads WHERE name = ?');
		$stmt->setFetchMode(PDO::FETCH_CLASS, 'Upload');
		$stmt->bindValue(1, $name);
		$stmt->execute();
		return $stmt->fetchAll();
	}
	
	function __destruct() {
		$this->close();
	}
	
	private function checkConnection() {
		if ($this->conn == null) {
			throw new PDOException('not connected');
		}
	}
}
?>
