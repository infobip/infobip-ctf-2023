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
