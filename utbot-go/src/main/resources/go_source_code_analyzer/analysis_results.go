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
	Name            string    `json:"name"`
	SourcePackage   GoPackage `json:"sourcePackage"`
	ImplementsError bool      `json:"implementsError"`
	UnderlyingType  string    `json:"underlyingType"`
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
	Name       string `json:"name"`
	Type       string `json:"type"`
	IsExported bool   `json:"isExported"`
}

type AnalyzedStructType struct {
	Name   string          `json:"name"`
	Fields []AnalyzedField `json:"fields"`
}

func (t AnalyzedStructType) GetName() string {
	return t.Name
}

type AnalyzedArrayType struct {
	Name        string `json:"name"`
	ElementType string `json:"elementType"`
	Length      int64  `json:"length"`
}

func (t AnalyzedArrayType) GetName() string {
	return t.Name
}

type AnalyzedSliceType struct {
	Name        string `json:"name"`
	ElementType string `json:"elementType"`
}

func (t AnalyzedSliceType) GetName() string {
	return t.Name
}

type AnalyzedMapType struct {
	Name        string `json:"name"`
	KeyType     string `json:"keyType"`
	ElementType string `json:"elementType"`
}

func (t AnalyzedMapType) GetName() string {
	return t.Name
}

type AnalyzedChanType struct {
	Name        string `json:"name"`
	ElementType string `json:"elementType"`
	Direction   string `json:"direction"`
}

func (t AnalyzedChanType) GetName() string {
	return t.Name
}

type AnalyzedPointerType struct {
	Name        string `json:"name"`
	ElementType string `json:"elementType"`
}

func (t AnalyzedPointerType) GetName() string {
	return t.Name
}

type AnalyzedFunctionParameter struct {
	Name string `json:"name"`
	Type string `json:"type"`
}

type AnalyzedFunction struct {
	Name        string                      `json:"name"`
	Types       map[string]AnalyzedType     `json:"types"`
	Parameters  []AnalyzedFunctionParameter `json:"parameters"`
	ResultTypes []string                    `json:"resultTypes"`
	Constants   map[string][]string         `json:"constants"`
	position    token.Pos
}

type AnalysisResult struct {
	AbsoluteFilePath           string             `json:"absoluteFilePath"`
	SourcePackage              Package            `json:"sourcePackage"`
	AnalyzedFunctions          []AnalyzedFunction `json:"analyzedFunctions"`
	NotSupportedFunctionsNames []string           `json:"notSupportedFunctionsNames"`
	NotFoundFunctionsNames     []string           `json:"notFoundFunctionsNames"`
}

type AnalysisResults struct {
	Results []AnalysisResult `json:"results"`
	IntSize int              `json:"intSize"`
}
