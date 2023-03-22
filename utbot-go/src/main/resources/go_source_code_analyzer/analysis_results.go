package main

import "go/token"

type GoPackage struct {
	PackageName string `json:"packageName"`
	PackagePath string `json:"packagePath"`
}

type AnalyzedType interface {
	GetName() string
}

type AnalyzedNamedType struct {
	Name            string       `json:"name"`
	SourcePackage   GoPackage    `json:"sourcePackage"`
	ImplementsError bool         `json:"implementsError"`
	UnderlyingType  AnalyzedType `json:"underlyingType"`
}

func (t AnalyzedNamedType) GetName() string {
	return t.Name
}

type AnalyzedInterfaceType struct {
	Name string `json:"name"`
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
	Name       string       `json:"name"`
	Type       AnalyzedType `json:"type"`
	IsExported bool         `json:"isExported"`
}

type AnalyzedStructType struct {
	Name   string          `json:"name"`
	Fields []AnalyzedField `json:"fields"`
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

type AnalyzedSliceType struct {
	Name        string       `json:"name"`
	ElementType AnalyzedType `json:"elementType"`
}

func (t AnalyzedSliceType) GetName() string {
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
	RequiredImports                     []Import                    `json:"requiredImports"`
	Constants                           map[string][]string         `json:"constants"`
	ModifiedFunctionForCollectingTraces string                      `json:"modifiedFunctionForCollectingTraces"`
	NumberOfAllStatements               int                         `json:"numberOfAllStatements"`
	position                            token.Pos
}

type AnalysisResult struct {
	AbsoluteFilePath           string             `json:"absoluteFilePath"`
	SourcePackage              Package            `json:"sourcePackage"`
	AnalyzedFunctions          []AnalyzedFunction `json:"analyzedFunctions"`
	NotSupportedFunctionsNames []string           `json:"notSupportedFunctionsNames"`
	NotFoundFunctionsNames     []string           `json:"notFoundFunctionsNames"`
}

type AnalysisResults struct {
	Results        []AnalysisResult `json:"results"`
	IntSize        int              `json:"intSize"`
	MaxTraceLength int              `json:"maxTraceLength"`
}
