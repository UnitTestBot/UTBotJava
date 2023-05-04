package main

type InstrumentationResult struct {
	AbsolutePathToInstrumentedPackage string              `json:"absolutePathToInstrumentedPackage"`
	AbsolutePathToInstrumentedModule  string              `json:"absolutePathToInstrumentedModule"`
	TestedFunctionsToCounters         map[string][]string `json:"testedFunctionsToCounters"`
}
