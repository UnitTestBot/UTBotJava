package main

import (
	"encoding/json"
	"flag"
	"go/ast"
	"go/importer"
	"go/parser"
	"go/token"
	"go/types"
	"log"
	"os"
)

func checkError(err error) {
	if err != nil {
		log.Fatal(err.Error())
	}
}

func analyzeTarget(target AnalysisTarget) AnalysisResult {
	// first of all, parse AST
	fset := token.NewFileSet()
	fileAst, astErr := parser.ParseFile(fset, target.AbsoluteFilePath, nil, 0)
	checkError(astErr)

	// collect info about types
	typesConfig := types.Config{Importer: importer.Default()}
	info := &types.Info{
		Defs:  make(map[*ast.Ident]types.Object),
		Uses:  make(map[*ast.Ident]types.Object),
		Types: make(map[ast.Expr]types.TypeAndValue),
	}
	_, typesCheckErr := typesConfig.Check(target.AbsoluteFilePath, fset, []*ast.File{fileAst}, info)
	checkError(typesCheckErr)

	// collect required info about selected functions
	analyzedFunctions, notSupportedFunctionsNames, notFoundFunctionsNames :=
		collectTargetAnalyzedFunctions(info, target.TargetFunctionsNames)

	return AnalysisResult{
		AbsoluteFilePath:           target.AbsoluteFilePath,
		PackageName:                fileAst.Name.String(),
		AnalyzedFunctions:          analyzedFunctions,
		NotSupportedFunctionsNames: notSupportedFunctionsNames,
		NotFoundFunctionsNames:     notFoundFunctionsNames,
	}
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
		result := analyzeTarget(target)
		analysisResults.Results = append(analysisResults.Results, result)
	}

	// serialize and write results
	jsonBytes, toJsonErr := json.MarshalIndent(analysisResults, "", "  ")
	checkError(toJsonErr)

	writeErr := os.WriteFile(resultsFilePath, jsonBytes, os.ModePerm)
	checkError(writeErr)
}
