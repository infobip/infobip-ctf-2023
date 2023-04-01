PRAGMA foreign_keys=ON;

DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS secret;

CREATE TABLE IF NOT EXISTS user (
	id INTEGER PRIMARY KEY,
	username VARCHAR(50) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL
);
CREATE TABLE IF NOT EXISTS secret (
	id INTEGER PRIMARY KEY,
	data VARCHAR(255) NOT NULL,
	user_id INTEGER,
	FOREIGN KEY(user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- svcaccount:BGtRl46socAY3hzm8N2VwDOd0L1FJyZM
INSERT INTO user (username, password) VALUES
	("svcaccount", "e05998ff70e774963e1706c24de9c8c5f7d8cedfb3c260a2cd97d99dc8f04a5e");

INSERT INTO secret (data, user_id) VALUES
	("ibctf{t1m3_c4n_n3v3r-m3nd-C4r3l3ss_Wh1sp3rs_OF_a-g00d-fr13nd}", (SELECT id FROM user WHERE username = "svcaccount"))
