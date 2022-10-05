package main

import (
	"go/types"
	"sort"
)

func implementsError(typ types.Type) bool {
	// TODO: get types.Interface of "error", for now straightforward strings equals
	//if(types.Implements(typ.Underlying(), ErrorInterface)) {
	//	return true
	//}
	return typ.Underlying().String() == "interface{Error() string}"
}

func toAnalyzedType(typ types.Type) AnalyzedType {
	implementsError := implementsError(typ)
	var name string
	if implementsError {
		name = "error"
	} else {
		name = typ.Underlying().String()
	}
	return AnalyzedType{
		Name:            name,
		ImplementsError: implementsError,
	}
}

// for now supports only basic and error result types
func checkTypeIsSupported(typ types.Type, isResultType bool) bool {
	underlyingType := typ.Underlying() // analyze real type, not alias or defined type
	if _, ok := underlyingType.(*types.Basic); ok {
		return true
	}
	if isResultType && implementsError(underlyingType) {
		return true
	}
	return false
}

func checkIsSupported(signature *types.Signature) bool {
	if signature.Recv() != nil { // is method
		return false
	}
	if signature.TypeParams() != nil { // has type params
		return false
	}
	if signature.Variadic() { // is variadic
		return false
	}
	if results := signature.Results(); results != nil {
		for i := 0; i < results.Len(); i++ {
			result := results.At(i)
			if !checkTypeIsSupported(result.Type(), true) {
				return false
			}
		}
	}
	if parameters := signature.Params(); parameters != nil {
		for i := 0; i < parameters.Len(); i++ {
			parameter := parameters.At(i)
			if !checkTypeIsSupported(parameter.Type(), false) {
				return false
			}
		}
	}
	return true
}

//goland:noinspection GoPreferNilSlice
func collectTargetAnalyzedFunctions(info *types.Info, targetFunctionsNames []string) (
	analyzedFunctions []AnalyzedFunction,
	notSupportedFunctionsNames []string,
	notFoundFunctionsNames []string,
) {
	analyzedFunctions = []AnalyzedFunction{}
	notSupportedFunctionsNames = []string{}
	notFoundFunctionsNames = []string{}

	selectAll := len(targetFunctionsNames) == 0
	foundTargetFunctionsNamesMap := map[string]bool{}
	for _, functionName := range targetFunctionsNames {
		foundTargetFunctionsNamesMap[functionName] = false
	}

	for _, obj := range info.Defs {
		switch typedObj := obj.(type) {
		case *types.Func:
			analyzedFunction := AnalyzedFunction{
				Name:        typedObj.Name(),
				Parameters:  []AnalyzedFunctionParameter{},
				ResultTypes: []AnalyzedType{},
				position:    typedObj.Pos(),
			}

			if !selectAll {
				if isFound, ok := foundTargetFunctionsNamesMap[analyzedFunction.Name]; !ok || isFound {
					continue
				} else {
					foundTargetFunctionsNamesMap[analyzedFunction.Name] = true
				}
			}

			signature := typedObj.Type().(*types.Signature)
			if !checkIsSupported(signature) {
				notSupportedFunctionsNames = append(notSupportedFunctionsNames, analyzedFunction.Name)
				continue
			}
			if parameters := signature.Params(); parameters != nil {
				for i := 0; i < parameters.Len(); i++ {
					parameter := parameters.At(i)
					analyzedFunction.Parameters = append(analyzedFunction.Parameters,
						AnalyzedFunctionParameter{
							Name: parameter.Name(),
							Type: toAnalyzedType(parameter.Type()),
						})
				}
			}
			if results := signature.Results(); results != nil {
				for i := 0; i < results.Len(); i++ {
					result := results.At(i)
					analyzedFunction.ResultTypes = append(analyzedFunction.ResultTypes, toAnalyzedType(result.Type()))
				}
			}

			analyzedFunctions = append(analyzedFunctions, analyzedFunction)
		}
	}

	for functionName, isFound := range foundTargetFunctionsNamesMap {
		if !isFound {
			notFoundFunctionsNames = append(notFoundFunctionsNames, functionName)
		}
	}
	sort.Slice(analyzedFunctions, func(i, j int) bool {
		return analyzedFunctions[i].position < analyzedFunctions[j].position
	})
	sort.Sort(sort.StringSlice(notSupportedFunctionsNames))
	sort.Sort(sort.StringSlice(notFoundFunctionsNames))

	return analyzedFunctions, notSupportedFunctionsNames, notFoundFunctionsNames
}
