package main

import "go/token"

type AnalyzedType interface {
	GetName() string
}

type AnalyzedInterfaceType struct {
	Name            string `json:"name"`
	ImplementsError bool   `json:"implementsError"`
}

func (t AnalyzedInterfaceType) GetName() string {
	return t.Name
}

type AnalyzedPrimitiveType struct {
	Name string `json:"name"`
}

func (t AnalyzedPrimitiveType) GetName() string {
	return t.Name
}

type AnalyzedField struct {
	Name string       `json:"name"`
	Type AnalyzedType `json:"type"`
}

type AnalyzedStructType struct {
	Name            string          `json:"name"`
	PackageName     string          `json:"packageName"`
	PackagePath     string          `json:"packagePath"`
	ImplementsError bool            `json:"implementsError"`
	Fields          []AnalyzedField `json:"fields"`
}

func (t AnalyzedStructType) GetName() string {
	return t.Name
}

type AnalyzedArrayType struct {
	Name        string       `json:"name"`
	ElementType AnalyzedType `json:"elementType"`
	Length      int64        `json:"length"`
}

func (t AnalyzedArrayType) GetName() string {
	return t.Name
}

type AnalyzedFunctionParameter struct {
	Name string       `json:"name"`
	Type AnalyzedType `json:"type"`
}

type AnalyzedFunction struct {
	Name                                string                      `json:"name"`
	ModifiedName                        string                      `json:"modifiedName"`
	Parameters                          []AnalyzedFunctionParameter `json:"parameters"`
	ResultTypes                         []AnalyzedType              `json:"resultTypes"`
	ModifiedFunctionForCollectingTraces string                      `json:"modifiedFunctionForCollectingTraces"`
	NumberOfAllStatements               int                         `json:"numberOfAllStatements"`
	position                            token.Pos
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
