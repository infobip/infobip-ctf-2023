# Solution

The challenge description gives a hint:
> We are so sure in our service that we have put our biggest secret under the service account performing the cleanup.

The target of this challenge is to access the service account that performs the cleanup - `svcaccount`.

Analyzing the source code points out an interesting way of initializing `Cipher`:
```python
key = os.urandom(KEY_LENGTH)
cipher = Cipher(algorithms.AES(key), modes.CBC(key))
```

What we see here is AES in CBC mode being initialized with key being set as both key and IV. This is a big security issue because recovering of the key is very easy in this case:
- encrypt plaintext that is 3 blocks (3\*16 bytes) large
- obtain ciphertext
- convert second block of ciphertext into 16 bytes of `\x00`
- repeat the first block of ciphertext as the third block
- feed modified ciphertext to the decryption algorithm
- obtain decryption result
- perform xor between the first and third 16-byte block of the decryption result to get the key

Luckily for us, AES in CBC mode is used when generating user's session token:
```python
def session_token(username: str) -> str:
    now = datetime.utcnow()
    nowf = now.strftime(DATETIME_FORMAT)

    shah = aes_encrypt(f"{username}{TOKEN_SPLIT_CH}{nowf}".encode())
    token = binascii.hexlify(shah)
    instance = socket.gethostname()
    
    sess_tok = {
        TOKEN_USER_KEY: username,
        TOKEN_SESS_KEY: token,
        TOKEN_INSTANCE_KEY: instance
    }
    return urlencode(sess_tok)
```

In order for us to enter the service account, we have to perform the aformentioned attack on AES in CBC mode to obtain encryption key and craft our own token.

## Obtaining ciphertext and performing conversions

We have to be very careful on the username length since we want to obtain a ciphertext that is exactly 3 blocks long. The building blocks of our token looks like this:
```
{USERNAME};YYYY-MM-DD.HH:mm
___________________________
??????????<---- len 17 --->
```

If we mind the padding, we should have a username of 15 byte length in order to produce a 3-block ciphertext (`[15 BYTE USERNAME][17 BYTE DATETIME][16 BYTE PADDING]`).

By simply registering a user with a large enough username and logging in, we can easily obtain the ciphertext in the cookie:
```sh
$ curl "http://172.17.0.2:8080/register" -d "username=123456789012345&password=123" -i
HTTP/1.1 302 FOUND
Server: gunicorn
Date: Thu, 24 Nov 2022 18:51:46 GMT
Connection: close
Content-Type: text/html; charset=utf-8
Content-Length: 199
Location: /login
Vary: Cookie
Set-Cookie: session=eyJfZmxhc2hlcyI6W3siIHQiOlsibWVzc2FnZSIsIlJlZ2lzdHJhdGlvbiBzdWNjZXNzZnVsIl19XX0.Y3-7GQ.H4WyWgzMThwzxKuD4npkD4VHhPo; HttpOnly; Path=/

<!doctype html>
<html lang=en>
<title>Redirecting...</title>
<h1>Redirecting...</h1>
<p>You should be redirected automatically to the target URL: <a href="/login">/login</a>. If not, click the link.
$ curl "http://172.17.0.2:8080/login" -d "username=123456789012345&password=123" -i
HTTP/1.1 302 FOUND
Server: gunicorn
Date: Thu, 24 Nov 2022 18:52:07 GMT
Connection: close
Content-Type: text/html; charset=utf-8
Content-Length: 189
Location: /
Set-Cookie: token=user=123456789012345&sess=3d87e206a17df723cd7146943e8393eaad73c9b652479642d7ceb201ebd862af8d7fb5211159abf2827a3d2d4626ab40&inst=4876fde4e15e; Path=/

<!doctype html>
<html lang=en>
<title>Redirecting...</title>
<h1>Redirecting...</h1>
<p>You should be redirected automatically to the target URL: <a href="/">/</a>. If not, click the link.
```

We can easily tamper with the token by hand:
```
Before: 3d87e206a17df723cd7146943e8393eaad73c9b652479642d7ceb201ebd862af8d7fb5211159abf2827a3d2d4626ab40
After:  3d87e206a17df723cd7146943e8393ea000000000000000000000000000000003d87e206a17df723cd7146943e8393ea
```

## Obtaining cipher key

Supplying the tampered cookie we obtain the ciphertext:
```sh
$ curl "http://172.17.0.2:8080/secret" -H "Cookie: token=user=123456789012345&sess=3d87e206a17df723cd7146943e8393ea000000000000000000000000000000003d87e206a17df723cd7146943e8393ea&inst=4876fde4e15e" -i
HTTP/1.1 200 OK
Server: gunicorn
Date: Thu, 24 Nov 2022 18:53:30 GMT
Connection: close
Content-Type: text/html; charset=utf-8
Content-Length: 977
Set-Cookie: token=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/
Set-Cookie: session=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=0; HttpOnly; Path=/

<!doctype html>
<title>Secret whisperer</title>
<link rel="stylesheet" href="/static/style.css">
<nav>
<h1><blink><a href="/">Secret whisperer</a></blink></h1>
</nav>
<section class="content">
  <header>
    
      <div class="flash">401 Unauthorized: Bad decrypt for 3d87e206a17df723cd7146943e8393ea000000000000000000000000000000003d87e206a17df723cd7146943e8393ea? b&#39;3132333435363738393031323334353b15775f90716901750508f3a51bbfd7b3ad8d58b0da9e3222eea3f4d22bb87564&#39;</div>
...
```

The obtained ciphertext blocks one and three must be xor'd in order to obtain the key:
```python
>>> def xor(a: bytes, b: bytes) -> bytes:
...     l = len(a) if len(a) < len(b) else len(b)
...     return bytes([a[i]^b[i] for i in range(l)])
... 
>>> from binascii import unhexlify
>>> a = unhexlify('3132333435363738393031323334353b15775f90716901750508f3a51bbfd7b3ad8d58b0da9e3222eea3f4d22bb87564')
>>> xor(a[:16], a[32:48])
b'\x9c\xbfk\x84\xef\xa8\x05\x1a\xd7\x93\xc5\xe0\x18\x8c@_'
>>> key = _
>>> from binascii import hexlify
>>> hexlify(key)
b'9cbf6b84efa8051ad793c5e0188c405f'
```

## Crafting service account token and getting flag

With the given key, we create the service account token. Keep in mind that the time used is UTC, so the local time cannot be used as reference:
```sh
$ echo -ne "svcaccount;2022-11-24.19:09\x05\x05\x05\x05\x05" | openssl enc -aes-128-cbc -iv 9cbf6b84efa8051ad793c5e0188c405f -K 9cbf6b84efa8051ad793c5e0188c405f -nosalt -nopad | xxd -p -c 32
eb66d9eea909b1e8df27cf8a078c898a54a3394b0f79d2d474c6050fc536be42
$ curl -sS "http://172.17.0.2:8080/secret" -H "Cookie: token=user=svcaccount&sess=eb66d9eea909b1e8df27cf8a078c898a54a3394b0f79d2d474c6050fc536be42&inst=4876fde4e15e" | grep ibctf
			<tr><td colspan="2">ibctf{t1m3_c4n_n3v3r-m3nd-C4r3l3ss_Wh1sp3rs_OF_a-g00d-fr13nd}</td></tr>
```

All described steps made by hand can be automated by a script. The automated version is in `solve.py`:
```sh
$ python3 solve.py
ibctf{t1m3_c4n_n3v3r-m3nd-C4r3l3ss_Wh1sp3rs_OF_a-g00d-fr13nd}
```
