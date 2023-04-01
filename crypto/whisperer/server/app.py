from flask import abort
from flask import flash
from flask import Flask
from flask import g
from flask import make_response
from flask import render_template
from flask import redirect
from flask import request
from flask import url_for

from apscheduler.schedulers.background import BackgroundScheduler
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher
from cryptography.hazmat.primitives.ciphers import algorithms
from cryptography.hazmat.primitives.ciphers import modes
from datetime import datetime
from datetime import timedelta
from urllib.parse import parse_qs
from urllib.parse import urlencode

import atexit
import binascii
import ipaddress
import os
import random
import requests
import socket
import string
import sqlite3

_DB_ATTR = "_db"
DATABASE_NAME = "whisper.db"
SVC_ACC_NAME = "svcaccount"

COOKIE_TOKEN_NAME = "token"
TOKEN_USER_KEY = "user"
TOKEN_SESS_KEY = "sess"
TOKEN_INSTANCE_KEY = "inst"
SESS_LT_SECS = 300

ENV_SVC_PASSWD = "FLASK_SVC_PASSWD"
ENV_SECRET_KEY = "FLASK_SECRET_KEY"
ENV_PASSWD_SALT = "FLASK_PASSWD_SALT"

KEY_LENGTH = 16
DATETIME_FORMAT = "%Y-%m-%d.%H:%M"
TOKEN_SPLIT_CH = ";"
APP_URL = "http://127.0.0.1:8080/"

app = Flask(__name__)
app.secret_key = binascii.unhexlify(os.getenv(ENV_SECRET_KEY))
svc_passwd = os.getenv(ENV_SVC_PASSWD)
salt = os.getenv(ENV_PASSWD_SALT)

cron = BackgroundScheduler(daemon=True)
cron.start()
atexit.register(lambda: cron.shutdown(wait=False))

key = os.urandom(KEY_LENGTH)
cipher = Cipher(algorithms.AES(key), modes.CBC(key))
padd = padding.PKCS7(KEY_LENGTH*8)

def db():
    dbconn = getattr(g, _DB_ATTR, None)
    if dbconn is None:
        dbconn = g._db = sqlite3.connect(DATABASE_NAME)
    return dbconn

@app.teardown_appcontext
def db_teardown(exception):
    dbconn = getattr(g, _DB_ATTR, None)
    if dbconn is not None:
        dbconn.close()

@cron.scheduled_job("cron", id="cleanup_job", hour="*", minute="*/5")
def simulate_manual_cleanup():
    s = requests.Session()
    s.post(f"{APP_URL}login", data={"username": SVC_ACC_NAME, "password": svc_passwd})
    s.get(f"{APP_URL}cleanup")

def sha256_derive(source: bytes) -> bytes:
    digest = hashes.Hash(hashes.SHA256())
    digest.update(source)
    return digest.finalize()
    
def pad(pt: bytes) -> bytes:
    padder = padd.padder()
    return padder.update(pt) + padder.finalize()
    
def unpad(pt: bytes) -> bytes:
    unpadder = padd.unpadder()
    return unpadder.update(pt) + unpadder.finalize()

def aes_encrypt(pt: bytes) -> bytes:
    enc = cipher.encryptor()
    return enc.update(pt) + enc.finalize()

def aes_decrypt(ct: bytes) -> bytes:
    dec = cipher.decryptor()
    return dec.update(ct) + dec.finalize()

def session_token(username: str) -> str:
    now = datetime.utcnow()
    nowf = now.strftime(DATETIME_FORMAT)

    padded = pad(f"{username}{TOKEN_SPLIT_CH}{nowf}".encode())
    shah = aes_encrypt(padded)
    token = binascii.hexlify(shah)
    instance = socket.gethostname()
    
    sess_tok = {
        TOKEN_USER_KEY: username,
        TOKEN_SESS_KEY: token,
        TOKEN_INSTANCE_KEY: instance
    }
    return urlencode(sess_tok)

def verify_session(cks: dict) -> str:
    if COOKIE_TOKEN_NAME not in cks:
        return None

    try:
        session = parse_qs(cks[COOKIE_TOKEN_NAME])
        username = session[TOKEN_USER_KEY]
        token = session[TOKEN_SESS_KEY]
        instance = session[TOKEN_INSTANCE_KEY]
    except KeyError:
        abort(401, f"Broken session: {session}")

    for i in [username, token, instance]:
        if len(i) != 1:
            abort(401, f"More than one {i}")

    usernameo = username[0]
    tokeno = token[0]
    instanceo = instance[0]

    try:
        xtok = binascii.unhexlify(tokeno)
        dec = aes_decrypt(xtok)
        shah = unpad(dec).decode()
        u, t = shah.split(TOKEN_SPLIT_CH)
        host = socket.gethostname()
    except Exception as e:
        msg = f"Bad decrypt for {tokeno}?"
        if dec is not None:
            msg += f" {binascii.hexlify(dec)}"
        abort(401, msg)

    dbcur = db().cursor()
    params = {"username": usernameo}
    dbcur.execute("SELECT 1 FROM user WHERE username = :username", params)
    success = dbcur.fetchone()


    if success is None or success[0] != 1:
        abort(401, f"User not found: {username}, bad decrypt? {shah}")

    if usernameo != u:
        abort(401, f"User mismatch: got {u}, want {usernameo}, bad decrypt? {shah}")

    if instanceo != host:
        abort(401, f"Instance mismatch: got {instanceo}, want {host}")

    tm = datetime.strptime(t, DATETIME_FORMAT)
    now = datetime.utcnow()
    diff = tm - now
    
    if diff > timedelta(seconds=SESS_LT_SECS):
        abort(401, f"Session expired")

    return usernameo

@app.errorhandler(401)
def unauthorized(e):
    flash(f"{e}")
    return logout()

@app.errorhandler(Exception)
def unexpected(e):
    flash(f"Unexpected error occurred: {e}")
    return logout()

@app.route("/logout")
def logout():
    resp = make_response(render_template("login.html"))
    resp.set_cookie(COOKIE_TOKEN_NAME, "", expires=0)
    return resp

@app.route("/login", methods=["GET"])
def login():
    if verify_session(request.cookies) is not None:
        return redirect(url_for("secrets"))

    return render_template("login.html")

@app.route("/login", methods=["POST"])
def login_submit():
    if verify_session(request.cookies) is not None:
        return redirect(url_for("secrets"))

    username = request.form.get("username")
    password = request.form.get("password")
    passh = binascii.hexlify(sha256_derive(f"{password}{salt}".encode()))

    dbcur = db().cursor()
    params = {"username": username, "password": passh.decode()}
    dbcur.execute("SELECT 1 FROM user WHERE username = :username AND password = :password", params)
    success = dbcur.fetchone()

    if success is None or success[0] != 1:
        flash("Login failed")
        return redirect(url_for("login"))

    resp = make_response(redirect(url_for("secrets")))
    resp.set_cookie(COOKIE_TOKEN_NAME, session_token(username))
    return resp

@app.route("/register", methods=["GET"])
def register():
    if verify_session(request.cookies) is not None:
        return redirect(url_for("secrets"))

    return render_template("register.html")

@app.route("/register", methods=["POST"])
def register_submit():
    if verify_session(request.cookies) is not None:
        return redirect(url_for("secrets"))

    username = request.form.get("username")
    if len(username) > 50:
        flash("Username is too long")
        return register()

    password = request.form.get("password")
    passh = binascii.hexlify(sha256_derive(f"{password}{salt}".encode()))

    try:
        dbconn = db()
        dbcur = dbconn.cursor()
        params = {"username": username, "password": passh.decode()}
        dbcur.execute("INSERT INTO user (username, password) VALUES (:username, :password)", params)
        dbconn.commit()
        flash("Registration successful")
    except sqlite3.IntegrityError:
        flash("Username already exists")
        return register()

    return redirect(url_for("login"))

@app.route("/", methods=["GET"])
def secrets():
    username = verify_session(request.cookies)
    if username is None:
        abort(401, "Unauthorized")

    dbcur = db().cursor()
    params = {"username": username}
    dbcur.execute("SELECT 1 FROM user WHERE username = :username", params)
    success = dbcur.fetchone()

    if success is None or success[0] != 1:
        abort(401, f"User not found: {username}, bad decrypt? {shah}")

    dbcur.execute("SELECT data FROM secret WHERE user_id = (SELECT id FROM user WHERE username = :username)", params)
    secrets = dbcur.fetchall()

    return render_template("secrets.html", secrets=secrets)

@app.route("/secret", methods=["GET"])
def secret_get():
    return secrets()

@app.route("/secret", methods=["POST"])
def secret_submit():
    username = verify_session(request.cookies)
    if username is None:
        abort(401, "Unauthorized")

    whisper = request.form.get("whisper")

    dbconn = db()
    dbcur = dbconn.cursor()
    params = {"secret": whisper, "username": username}
    dbcur.execute("INSERT INTO secret (data, user_id) VALUES (:secret, (SELECT id FROM user WHERE username = :username))", params)
    dbconn.commit()

    return secrets()

@app.route("/cleanup")
def cleanup():
    ip = ipaddress.ip_address(request.remote_addr)
    if not ip.is_loopback:
        abort(401)

    username = verify_session(request.cookies)
    if username is None or username != SVC_ACC_NAME:
        abort(401)

    dbconn = db()
    dbcur = dbconn.cursor()
    params = {"svc": SVC_ACC_NAME}
    dbcur.execute("DELETE FROM user WHERE username != :svc", params)
    dbcur.execute("DELETE FROM secret WHERE user_id != (SELECT id FROM user WHERE username = :svc)", params)
    dbconn.commit()

    return secrets()
