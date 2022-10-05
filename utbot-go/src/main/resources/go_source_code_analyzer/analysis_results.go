package main

import "go/token"

type AnalyzedType struct {
	Name            string `json:"name"`
	ImplementsError bool   `json:"implementsError"`
}

type AnalyzedFunctionParameter struct {
	Name string       `json:"name"`
	Type AnalyzedType `json:"type"`
}

type AnalyzedFunction struct {
	Name        string                      `json:"name"`
	Parameters  []AnalyzedFunctionParameter `json:"parameters"`
	ResultTypes []AnalyzedType              `json:"resultTypes"`
	position    token.Pos
}

type AnalysisResult struct {
	AbsoluteFilePath           string             `json:"absoluteFilePath"`
	PackageName                string             `json:"packageName"`
	AnalyzedFunctions          []AnalyzedFunction `json:"analyzedFunctions"`
	NotSupportedFunctionsNames []string           `json:"notSupportedFunctionsNames"`
	NotFoundFunctionsNames     []string           `json:"notFoundFunctionsNames"`
}

type AnalysisResults struct {
	Results []AnalysisResult `json:"results"`
}
