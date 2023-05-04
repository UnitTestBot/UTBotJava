package main

type InstrumentationTarget struct {
	AbsolutePackagePath string   `json:"absolutePackagePath"`
	TestedFunctions     []string `json:"testedFunctions"`
}
