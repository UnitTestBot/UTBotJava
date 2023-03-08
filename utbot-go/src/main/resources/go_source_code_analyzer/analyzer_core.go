package main

import (
	"bytes"
	"errors"
	"fmt"
	"go/ast"
	"go/printer"
	"go/token"
	"go/types"
	"sort"
	"sync"
)

var errorInterface = func() *types.Interface {
	variable := types.NewVar(token.NoPos, nil, "", types.Typ[types.String])
	results := types.NewTuple(variable)
	signature := types.NewSignatureType(nil, nil, nil, nil, results, false)
	method := types.NewFunc(token.NoPos, nil, "Error", signature)
	return types.NewInterfaceType([]*types.Func{method}, nil)
}()

func implementsError(typ types.Type) bool {
	return types.Implements(typ, errorInterface)
}

//goland:noinspection GoPreferNilSlice
func toAnalyzedType(typ types.Type) (AnalyzedType, error) {
	var result AnalyzedType
	switch underlyingType := typ.Underlying().(type) {
	case *types.Basic:
		name := underlyingType.Name()
		result = AnalyzedPrimitiveType{Name: name}
	case *types.Struct:
		namedType := typ.(*types.Named)
		name := namedType.Obj().Name()
		pkg := namedType.Obj().Pkg()
		isError := implementsError(namedType)

		fields := []AnalyzedField{}
		for i := 0; i < underlyingType.NumFields(); i++ {
			field := underlyingType.Field(i)

			fieldType, err := toAnalyzedType(field.Type())
			checkError(err)

			fields = append(fields, AnalyzedField{field.Name(), fieldType, field.Exported()})
		}

		result = AnalyzedStructType{
			Name:            name,
			PackageName:     pkg.Name(),
			PackagePath:     pkg.Path(),
			ImplementsError: isError,
			Fields:          fields,
		}
	case *types.Array:
		arrayType := typ.(*types.Array)

		arrayElemType, err := toAnalyzedType(arrayType.Elem())
		checkError(err)

		elemTypeName := arrayElemType.GetName()

		length := arrayType.Len()
		name := fmt.Sprintf("[%d]%s", length, elemTypeName)

		result = AnalyzedArrayType{
			Name:        name,
			ElementType: arrayElemType,
			Length:      length,
		}
	case *types.Slice:
		sliceType := typ.(*types.Slice)

		sliceElemType, err := toAnalyzedType(sliceType.Elem())
		checkError(err)

		elemTypeName := sliceElemType.GetName()
		name := fmt.Sprintf("[]%s", elemTypeName)

		result = AnalyzedSliceType{
			Name:        name,
			ElementType: sliceElemType,
		}
	case *types.Interface:
		namedType := typ.(*types.Named)
		name := namedType.Obj().Name()
		pkg := namedType.Obj().Pkg()
		packageName, packagePath := "", ""
		if pkg != nil {
			packageName = pkg.Name()
			packagePath = pkg.Path()
		}

		isError := implementsError(namedType)
		if !isError {
			return nil, errors.New("currently only error interface is supported")
		}
		result = AnalyzedInterfaceType{
			Name:            fmt.Sprintf("interface %s", name),
			ImplementsError: isError,
			PackageName:     packageName,
			PackagePath:     packagePath,
		}
	}
	return result, nil
}

// for now supports only basic and error result types
func checkTypeIsSupported(typ types.Type, isResultType bool) bool {
	underlyingType := typ.Underlying() // analyze real type, not alias or defined type
	if _, ok := underlyingType.(*types.Basic); ok {
		return true
	}
	if structType, ok := underlyingType.(*types.Struct); ok {
		for i := 0; i < structType.NumFields(); i++ {
			if !checkTypeIsSupported(structType.Field(i).Type(), isResultType) {
				return false
			}
		}
		return true
	}
	if arrayType, ok := underlyingType.(*types.Array); ok {
		return checkTypeIsSupported(arrayType.Elem(), isResultType)
	}
	if sliceType, ok := underlyingType.(*types.Slice); ok {
		return checkTypeIsSupported(sliceType.Elem(), isResultType)
	}
	if interfaceType, ok := underlyingType.(*types.Interface); ok && isResultType {
		return implementsError(interfaceType)
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

func collectTargetAnalyzedFunctions(
	fset *token.FileSet,
	info *types.Info,
	targetFunctionsNames []string,
	allImportsInFile map[Import]bool,
	sourcePackage Package,
) (
	analyzedFunctions []AnalyzedFunction,
	notSupportedFunctionsNames []string,
	notFoundFunctionsNames []string,
) {
	analyzedFunctions = []AnalyzedFunction{}
	notSupportedFunctionsNames = []string{}
	notFoundFunctionsNames = []string{}

	foundTargetFunctionsNamesMap := map[string]bool{}
	for _, functionName := range targetFunctionsNames {
		foundTargetFunctionsNamesMap[functionName] = false
	}

	var blankImports []Import
	for i := range allImportsInFile {
		if i.Alias == "_" {
			blankImports = append(blankImports, i)
		}
	}

	var wg sync.WaitGroup
	var mutex sync.Mutex

	for ident, obj := range info.Defs {
		switch typedObj := obj.(type) {
		case *types.Func:
			wg.Add(1)
			go func(ident *ast.Ident, typeObj *types.Func) {
				defer wg.Done()

				analyzedFunction := AnalyzedFunction{
					Name:            typedObj.Name(),
					Parameters:      []AnalyzedFunctionParameter{},
					ResultTypes:     []AnalyzedType{},
					RequiredImports: []Import{},
					position:        typedObj.Pos(),
				}

				mutex.Lock()
				if isFound, ok := foundTargetFunctionsNamesMap[analyzedFunction.Name]; !ok || isFound {
					mutex.Unlock()
					return
				} else {
					foundTargetFunctionsNamesMap[analyzedFunction.Name] = true
					mutex.Unlock()
				}

				signature := typedObj.Type().(*types.Signature)
				if !checkIsSupported(signature) {
					mutex.Lock()
					notSupportedFunctionsNames = append(notSupportedFunctionsNames, analyzedFunction.Name)
					mutex.Unlock()
					return
				}
				if parameters := signature.Params(); parameters != nil {
					for i := 0; i < parameters.Len(); i++ {
						parameter := parameters.At(i)

						parameterType, err := toAnalyzedType(parameter.Type())
						checkError(err)

						analyzedFunction.Parameters = append(analyzedFunction.Parameters,
							AnalyzedFunctionParameter{
								Name: parameter.Name(),
								Type: parameterType,
							},
						)
					}
				}
				if results := signature.Results(); results != nil {
					for i := 0; i < results.Len(); i++ {
						result := results.At(i)

						resultType, err := toAnalyzedType(result.Type())
						checkError(err)

						analyzedFunction.ResultTypes = append(analyzedFunction.ResultTypes, resultType)
					}
				}

				funcDecl := ident.Obj.Decl.(*ast.FuncDecl)

				constantExtractor := ConstantExtractor{info: info, constants: map[string][]string{}}
				ast.Walk(&constantExtractor, funcDecl)
				analyzedFunction.Constants = constantExtractor.constants

				functionModifier := FunctionModifier{maxTraceLen: MaxTraceLength}
				functionModifier.ModifyFunctionDeclaration(funcDecl)
				ast.Walk(&functionModifier, funcDecl)

				importsCollector := ImportsCollector{
					info:             info,
					requiredImports:  map[Import]bool{},
					allImportsInFile: allImportsInFile,
					sourcePackage:    sourcePackage,
				}
				ast.Walk(&importsCollector, funcDecl)
				for _, i := range blankImports {
					importsCollector.requiredImports[i] = true
				}

				var modifiedFunction bytes.Buffer
				cfg := printer.Config{
					Mode:     printer.TabIndent,
					Tabwidth: 4,
					Indent:   0,
				}
				err := cfg.Fprint(&modifiedFunction, fset, funcDecl)
				checkError(err)

				analyzedFunction.ModifiedName = funcDecl.Name.String()
				for i := range importsCollector.requiredImports {
					analyzedFunction.RequiredImports = append(analyzedFunction.RequiredImports, i)
				}
				analyzedFunction.ModifiedFunctionForCollectingTraces = modifiedFunction.String()
				analyzedFunction.NumberOfAllStatements = functionModifier.lineCounter

				mutex.Lock()
				analyzedFunctions = append(analyzedFunctions, analyzedFunction)
				mutex.Unlock()
			}(ident, typedObj)
		}
	}

	wg.Wait()

	for functionName, isFound := range foundTargetFunctionsNamesMap {
		if !isFound {
			notFoundFunctionsNames = append(notFoundFunctionsNames, functionName)
		}
	}
	sort.Slice(analyzedFunctions, func(i, j int) bool {
		return analyzedFunctions[i].position < analyzedFunctions[j].position
	})
	sort.Strings(notSupportedFunctionsNames)
	sort.Strings(notFoundFunctionsNames)

	return analyzedFunctions, notSupportedFunctionsNames, notFoundFunctionsNames
}
