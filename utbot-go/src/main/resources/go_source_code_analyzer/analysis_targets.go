package main

type AnalysisTarget struct {
	AbsoluteFilePath     string   `json:"absoluteFilePath"`
	TargetFunctionsNames []string `json:"targetFunctionsNames"`
}

type AnalysisTargets struct {
	Targets []AnalysisTarget `json:"targets"`
}
