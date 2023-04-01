from datetime import datetime
from urllib.parse import parse_qs
from urllib.parse import urlencode

from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher
from cryptography.hazmat.primitives.ciphers import algorithms
from cryptography.hazmat.primitives.ciphers import modes

import binascii
import random
import re
import requests
import string

URL = "http://172.17.0.2:8080/"

def xor(a: bytes, b: bytes) -> bytes:
    l = len(a) if len(a) < len(b) else len(b)
    return bytes([a[i]^b[i] for i in range(l)])

def aes_encrypt(pt: bytes, key: bytes) -> bytes:
    padder = padding.PKCS7(128).padder()
    padded = padder.update(pt) + padder.finalize()
    enc = Cipher(algorithms.AES(key), modes.CBC(key)).encryptor()
    return enc.update(padded) + enc.finalize()

# Generate user, username must be length of 15 (+17 DATETIME +16 PADDING)
user = ''.join(random.sample(string.ascii_letters, 15))
params={"username": user, "password": user}

# Register and login
s = requests.Session()
s.post(f"{URL}register", data=params)
s.post(f"{URL}login", data=params)
qs = parse_qs(s.cookies["token"])

# Obtain ciphertext from token
ciphertext = binascii.unhexlify(qs["sess"][0])
# Craft a malicious ciphertext
crafted = ciphertext[:16] + b"\x00"*16 + ciphertext[:16]
craft_tok = {
    "user": qs["user"][0],
    "sess": binascii.hexlify(crafted),
    "inst": qs["inst"][0]
}
token = urlencode(craft_tok)

# Send the malicious ciphertext and obtain decrypt result
s.cookies.set("token", token)
r = s.get(f"{URL}secret")
m = re.search("Bad decrypt for [0-9a-f]+\? b&#39;(.+)&#39;", r.text)
plaintext = binascii.unhexlify(m.group(1))

# XOR first and third to obtain the key
key = xor(plaintext[:16], plaintext[32:48])
# Craft a token for service account
time = datetime.utcnow().strftime("%Y-%m-%d.%H:%M")
sess = aes_encrypt(f"svcaccount;{time}".encode(), key)
craft_tok["sess"] = binascii.hexlify(sess)
craft_tok["user"] = "svcaccount"
token = urlencode(craft_tok)

# Send request to get the flag
s.cookies.set("token", token)
r = s.get(f"{URL}secret")
flag = re.search("ibctf{.*}", r.text)
print(flag.group(0))
s.close()
