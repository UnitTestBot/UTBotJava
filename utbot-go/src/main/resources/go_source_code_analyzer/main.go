package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strconv"
	"sync"

	"golang.org/x/tools/go/packages"
)

func checkError(err error) {
	if err != nil {
		log.Fatal(err.Error())
	}
}

func analyzeTarget(target AnalysisTarget) (*AnalysisResult, error) {
	if len(target.TargetFunctionNames) == 0 && len(target.TargetMethodNames) == 0 {
		return nil, fmt.Errorf("target must contain target functions or methods")
	}

	pkgPath := filepath.Dir(target.AbsoluteFilePath)

	dir, _ := filepath.Split(target.AbsoluteFilePath)
	cfg := packages.Config{
		Mode: packages.NeedName | packages.NeedTypes | packages.NeedTypesInfo | packages.NeedDeps |
			packages.NeedImports | packages.NeedSyntax | packages.NeedFiles | packages.NeedCompiledGoFiles,
		Dir: dir,
	}
	cfg.Env = os.Environ()
	pkgs, err := packages.Load(&cfg, pkgPath)
	checkError(err)
	if len(pkgs) != 1 {
		return nil, fmt.Errorf("cannot build multiple packages: %s", err)
	}
	if packages.PrintErrors(pkgs) > 0 {
		return nil, fmt.Errorf("typechecking of %s failed", pkgPath)
	}

	targetPackage := pkgs[0]
	if len(targetPackage.CompiledGoFiles) != len(targetPackage.Syntax) {
		return nil, fmt.Errorf("parsing returned nil for some files")
	}
	index := 0
	for ; index < len(targetPackage.CompiledGoFiles); index++ {
		p1, err := filepath.Abs(targetPackage.CompiledGoFiles[index])
		checkError(err)
		p2, err := filepath.Abs(target.AbsoluteFilePath)
		checkError(err)

		if p1 == p2 {
			break
		}
	}
	if index == len(targetPackage.CompiledGoFiles) {
		return nil, fmt.Errorf("target file not found in compiled go files")
	}

	// collect required info about selected functions
	analyzedFunctions, notSupportedFunctionsNames, notFoundFunctionsNames :=
		collectTargetAnalyzedFunctions(
			targetPackage.TypesInfo,
			target.TargetFunctionNames,
			target.TargetMethodNames,
			Package{
				PackageName: targetPackage.Name,
				PackagePath: targetPackage.PkgPath,
			},
		)

	return &AnalysisResult{
		AbsoluteFilePath: target.AbsoluteFilePath,
		SourcePackage: Package{
			PackageName: targetPackage.Name,
			PackagePath: targetPackage.PkgPath,
		},
		AnalyzedFunctions:         analyzedFunctions,
		NotSupportedFunctionNames: notSupportedFunctionsNames,
		NotFoundFunctionNames:     notFoundFunctionsNames,
	}, nil
}

func main() {
	var targetsFilePath, resultsFilePath string
	flag.StringVar(&targetsFilePath, "targets", "", "path to JSON file to read analysis targets from")
	flag.StringVar(&resultsFilePath, "results", "", "path to JSON file to write analysis results to")
	flag.Parse()

	// read and deserialize targets
	targetsBytes, readErr := os.ReadFile(targetsFilePath)
	checkError(readErr)

	var analysisTargets AnalysisTargets
	fromJsonErr := json.Unmarshal(targetsBytes, &analysisTargets)
	checkError(fromJsonErr)

	// parse each requested Go source file
	var wg sync.WaitGroup

	results := make([]AnalysisResult, len(analysisTargets.Targets))
	for i, target := range analysisTargets.Targets {
		wg.Add(1)
		go func(i int, target AnalysisTarget) {
			defer wg.Done()

			result, err := analyzeTarget(target)
			checkError(err)

			results[i] = *result
		}(i, target)
	}

	wg.Wait()

	analysisResults := AnalysisResults{
		Results: results,
		IntSize: strconv.IntSize,
	}

	// serialize and write results
	jsonBytes, toJsonErr := json.MarshalIndent(analysisResults, "", "  ")
	checkError(toJsonErr)

	writeErr := os.WriteFile(resultsFilePath, jsonBytes, os.ModePerm)
	checkError(writeErr)
}
