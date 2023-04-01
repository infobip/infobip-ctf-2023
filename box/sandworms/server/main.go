package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"go/parser"
	"go/token"
	"html/template"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"golang.org/x/tools/go/packages"
)

const (
	SourceFile    = "src.go"
	BinaryFile    = "sandbox"
	ExecDirectory = "exec"
)

type Sandbox struct {
	Script   string `json:"script"`
	execDir  string
	pkgNames []string
}

func NewSandbox() (*Sandbox, error) {
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

	execDir, err := os.MkdirTemp("", ExecDirectory)
	if err != nil {
		return nil, fmt.Errorf("failed to create temp dir: %v", err)
	}

	return &Sandbox{execDir: execDir, pkgNames: pkgNames}, nil
}

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

func (s *Sandbox) saveScript() error {
	if len(s.execDir) == 0 {
		return errors.New("execution directory does not exist")
	}

	source := filepath.Join(s.execDir, SourceFile)
	if err := os.WriteFile(source, []byte(*&s.Script), 0666); err != nil {
		return fmt.Errorf("failed to write script: %v", err)
	}

	return nil
}

func (s *Sandbox) run() (string, error) {
	var build bytes.Buffer
	cmd := &exec.Cmd{
		Path:   "/usr/local/go/bin/go",
		Args:   []string{"go", "build", "-gcflags", "-N", "-o", BinaryFile, SourceFile},
		Dir:    s.execDir,
		Stdout: &build,
		Stderr: &build,
	}
	if err := cmd.Start(); err != nil {
		return "", fmt.Errorf("exec failed with: %v, %s", err, build.String())
	}
	if err := cmd.Wait(); err != nil {
		return "", fmt.Errorf("wait failed with: %v, %s", err, build.String())
	}

	cmd = &exec.Cmd{
		Path: BinaryFile,
		Args: []string{BinaryFile},
		Dir:  s.execDir,
	}
	out, err := cmd.CombinedOutput()
	if err != nil {
		return "", err
	}

	return string(out), nil
}

func (s *Sandbox) cleanup() error {
	return os.RemoveAll(s.execDir)
}

func runner(w http.ResponseWriter, r *http.Request) {
	sandbox, err := NewSandbox()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "failed to initialize sandbox: %v", err)
		return
	}
	defer sandbox.cleanup()

	if err := json.NewDecoder(r.Body).Decode(&sandbox); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "invalid request: %v", err)
		return
	}

	if err := sandbox.saveScript(); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "failed to save script: %v", err)
		return
	}

	if err := sandbox.validate(); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "error validating script: %v", err)
		return
	}

	result, err := sandbox.run()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "failed to run script: %v", err)
		return
	}

	fmt.Fprintf(w, result)
}

func index(w http.ResponseWriter, r *http.Request) {
	t, err := template.ParseFiles("index.html")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "failed to parse template: %v", err)
		return
	}
	t.Execute(w, nil)
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/", index)
	mux.HandleFunc("/run", runner)
	fmt.Println("server started")
	if err := http.ListenAndServe(":8000", mux); err != nil {
		fmt.Printf("error occured: %v\n", err)
	}
}
