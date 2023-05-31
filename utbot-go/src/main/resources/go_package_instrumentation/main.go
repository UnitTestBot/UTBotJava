package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"go/ast"
	"go/printer"
	"golang.org/x/tools/go/packages"
	"io"
	"io/fs"
	"os"
	"path/filepath"
	"strings"
)

func failf(str string, args ...any) {
	_, _ = fmt.Fprintf(os.Stderr, str+"\n", args...)
	os.Exit(1)
}

func instrument(astFile *ast.File, modifier *Instrumentator) {
	ast.Walk(modifier, astFile)
}

func copyFile(src, dst string) {
	r, err := os.Open(src)
	if err != nil {
		failf("copyFile: could not read %v", src, err)
	}
	w, err := os.OpenFile(dst, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0666)
	if err != nil {
		failf("copyFile: could not write %v: %v", dst, err)
	}
	if _, err := io.Copy(w, r); err != nil {
		failf("copyFile: copying failed: %v", err)
	}
	if err := r.Close(); err != nil {
		failf("copyFile: closing %v failed: %v", src, err)
	}
	if err := w.Close(); err != nil {
		failf("copyFile: closing %v failed: %v", dst, err)
	}
}

func main() {
	var targetFilePath, resultFilePath string
	flag.StringVar(&targetFilePath, "target", "", "path to JSON file to read instrumentation target from")
	flag.StringVar(&resultFilePath, "result", "", "path to JSON file to write instrumentation result to")
	flag.Parse()

	// read and deserialize targets
	targetBytes, readErr := os.ReadFile(targetFilePath)
	if readErr != nil {
		failf("failed to read file %s: %s", targetFilePath, readErr)
	}
	var instrumentationTarget InstrumentationTarget
	fromJsonErr := json.Unmarshal(targetBytes, &instrumentationTarget)
	if fromJsonErr != nil {
		failf("failed to parse instrumentation target: %s", fromJsonErr)
	}

	// parse package
	pkgPath := instrumentationTarget.AbsolutePackagePath
	cfg := packages.Config{
		Mode: packages.NeedName | packages.NeedFiles | packages.NeedCompiledGoFiles | packages.NeedImports |
			packages.NeedTypes | packages.NeedTypesSizes | packages.NeedSyntax | packages.NeedTypesInfo |
			packages.NeedDeps | packages.NeedModule | packages.NeedEmbedFiles | packages.NeedEmbedPatterns | packages.NeedExportFile,
		Dir: pkgPath,
	}
	cfg.Env = os.Environ()
	pkgs, err := packages.Load(&cfg, pkgPath)
	if err != nil {
		failf("failed to parse package: %s", err)
	}
	if len(pkgs) != 1 {
		failf("cannot build multiple packages: %s", err)
	}
	if packages.PrintErrors(pkgs) > 0 {
		failf("typechecking of %s failed", pkgPath)
	}

	targetPackage := pkgs[0]
	module := targetPackage.Module

	workdir, err := os.MkdirTemp("", "utbot-go*")
	if err != nil {
		failf("failed to create temporary directory: %s", err)
	}

	testedFunctions := make(map[string]bool, len(instrumentationTarget.TestedFunctions))
	for _, f := range instrumentationTarget.TestedFunctions {
		testedFunctions[f] = true
	}
	modifier := NewInstrumentator(testedFunctions)
	absolutePathToInstrumentedPackage := ""
	visit := func(pkg *packages.Package) {
		if pkg.Module == nil || pkg.Module.Path != module.Path {
			return
		}
		for i, fullName := range pkg.CompiledGoFiles {
			fname := strings.Replace(fullName, module.Dir, "", 1)
			outpath := filepath.Join(workdir, fname)
			if !strings.HasSuffix(fullName, ".go") {
				continue
			}
			astFile := pkg.Syntax[i]
			if pkg.PkgPath == targetPackage.PkgPath {
				instrument(astFile, &modifier)
				absolutePathToInstrumentedPackage = filepath.Dir(outpath)
			}
			buf := new(bytes.Buffer)
			c := printer.Config{
				Mode:     printer.TabIndent,
				Tabwidth: 4,
				Indent:   0,
			}
			err = c.Fprint(buf, pkg.Fset, astFile)
			if err != nil {
				failf("failed to fprint: %s", err)
			}
			err = os.MkdirAll(filepath.Dir(outpath), 0666)
			if err != nil {
				failf("failed to create directories: %s", err)
			}
			_, err = os.Create(outpath)
			if err != nil {
				failf("failed to create file: %s", err)
			}
			err = os.WriteFile(outpath, buf.Bytes(), 0666)
			if err != nil {
				failf("failed to write to file: %s", err)
			}
		}
	}
	packages.Visit(pkgs, nil, visit)

	err = filepath.Walk(module.Dir, func(path string, info fs.FileInfo, err error) error {
		if info.IsDir() && info.Name() == ".git" {
			return filepath.SkipDir
		}
		if info.IsDir() {
			dname := strings.Replace(path, module.Dir, "", 1)
			dirPath := filepath.Join(workdir, dname)
			err = os.MkdirAll(dirPath, 0666)
			return err
		}
		if !strings.HasSuffix(path, ".go") {
			fname := strings.Replace(path, module.Dir, "", 1)
			outpath := filepath.Join(workdir, fname)
			err = os.Symlink(path, outpath)
			if err != nil {
				copyFile(path, outpath)
			}
			return nil
		}
		return nil
	})
	if err != nil {
		failf("failed to walk the module directory: %s", err)
	}

	// serialize and write results
	instrumentationResult := InstrumentationResult{
		AbsolutePathToInstrumentedPackage: absolutePathToInstrumentedPackage,
		AbsolutePathToInstrumentedModule:  workdir,
		TestedFunctionsToCounters:         modifier.functionToCounters,
	}
	jsonBytes, toJsonErr := json.MarshalIndent(instrumentationResult, "", "  ")
	if toJsonErr != nil {
		failf("failed to serialize instrumentation result: %s", toJsonErr)
	}
	writeErr := os.WriteFile(resultFilePath, jsonBytes, os.ModePerm)
	if writeErr != nil {
		failf("failed to write instrumentation result: %s", writeErr)
	}
}
