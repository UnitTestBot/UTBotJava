package main

type AnalysisTarget struct {
	AbsoluteFilePath    string   `json:"absoluteFilePath"`
	TargetFunctionNames []string `json:"targetFunctionNames"`
	TargetMethodNames   []string `json:"targetMethodNames"`
}

type AnalysisTargets struct {
	Targets []AnalysisTarget `json:"targets"`
}
