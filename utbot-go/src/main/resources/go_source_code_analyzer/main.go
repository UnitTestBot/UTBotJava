package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"go/parser"
	"go/token"
	"log"
	"os"
	"path/filepath"

	"golang.org/x/tools/go/packages"
)

func checkError(err error) {
	if err != nil {
		log.Fatal(err.Error())
	}
}

func getPackageName(path string) string {
	fset := token.NewFileSet()

	astFile, astErr := parser.ParseFile(fset, path, nil, parser.PackageClauseOnly)
	checkError(astErr)

	return astFile.Name.Name
}

func analyzeTarget(target AnalysisTarget) (*AnalysisResult, error) {
	packageName := getPackageName(target.AbsoluteFilePath)

	dir, _ := filepath.Split(target.AbsoluteFilePath)
	cfg := packages.Config{
		Mode: packages.NeedName | packages.NeedTypes | packages.NeedTypesInfo | packages.NeedDeps | packages.NeedImports,
		Dir:  dir,
	}
	pkgs, err := packages.Load(&cfg, "")
	if err != nil {
		return nil, err
	}

	for _, pkg := range pkgs {
		if pkg.Name == packageName {
			// collect required info about selected functions
			analyzedFunctions, notSupportedFunctionsNames, notFoundFunctionsNames :=
				collectTargetAnalyzedFunctions(pkg.Fset, pkg.TypesInfo, target.TargetFunctionsNames)

			return &AnalysisResult{
				AbsoluteFilePath:           target.AbsoluteFilePath,
				PackageName:                packageName,
				AnalyzedFunctions:          analyzedFunctions,
				NotSupportedFunctionsNames: notSupportedFunctionsNames,
				NotFoundFunctionsNames:     notFoundFunctionsNames,
			}, nil
		}
	}
	return nil, fmt.Errorf("package %s not found", packageName)
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
	analysisResults := AnalysisResults{Results: []AnalysisResult{}}
	for _, target := range analysisTargets.Targets {
		result, err := analyzeTarget(target)
		checkError(err)
		analysisResults.Results = append(analysisResults.Results, *result)
	}

	// serialize and write results
	jsonBytes, toJsonErr := json.MarshalIndent(analysisResults, "", "  ")
	checkError(toJsonErr)

	writeErr := os.WriteFile(resultsFilePath, jsonBytes, os.ModePerm)
	checkError(writeErr)
}
