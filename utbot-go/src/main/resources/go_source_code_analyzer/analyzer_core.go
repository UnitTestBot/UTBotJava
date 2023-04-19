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

func ChanDirToString(dir types.ChanDir) (string, error) {
	switch dir {
	case types.SendOnly:
		return "SENDONLY", nil
	case types.RecvOnly:
		return "RECVONLY", nil
	case types.SendRecv:
		return "SENDRECV", nil
	}
	return "", fmt.Errorf("unsupported channel direction: %d", dir)
}

func toAnalyzedType(typ types.Type) (AnalyzedType, error) {
	switch t := typ.(type) {
	case *types.Pointer:
		elemType, err := toAnalyzedType(t.Elem())
		checkError(err)

		elemTypeName := elemType.GetName()

		name := fmt.Sprintf("*%s", elemTypeName)
		return AnalyzedPointerType{
			Name:        name,
			ElementType: elemType,
		}, nil
	case *types.Named:
		name := t.Obj().Name()

		pkg := t.Obj().Pkg()
		packageName, packagePath := "", ""
		if pkg != nil {
			packageName = pkg.Name()
			packagePath = pkg.Path()
		}

		isError := implementsError(t)

		underlyingType, err := toAnalyzedType(t.Underlying())
		checkError(err)

		return AnalyzedNamedType{
			Name: name,
			SourcePackage: GoPackage{
				PackageName: packageName,
				PackagePath: packagePath,
			},
			ImplementsError: isError,
			UnderlyingType:  underlyingType,
		}, nil
	case *types.Basic:
		name := t.Name()
		return AnalyzedPrimitiveType{Name: name}, nil
	case *types.Struct:
		name := "struct{}"

		fields := make([]AnalyzedField, 0, t.NumFields())
		for i := 0; i < t.NumFields(); i++ {
			field := t.Field(i)

			fieldType, err := toAnalyzedType(field.Type())
			checkError(err)

			fields = append(fields, AnalyzedField{field.Name(), fieldType, field.Exported()})
		}

		return AnalyzedStructType{
			Name:   name,
			Fields: fields,
		}, nil
	case *types.Array:
		arrayElemType, err := toAnalyzedType(t.Elem())
		checkError(err)

		elemTypeName := arrayElemType.GetName()

		length := t.Len()
		name := fmt.Sprintf("[%d]%s", length, elemTypeName)

		return AnalyzedArrayType{
			Name:        name,
			ElementType: arrayElemType,
			Length:      length,
		}, nil
	case *types.Slice:
		sliceElemType, err := toAnalyzedType(t.Elem())
		checkError(err)

		elemTypeName := sliceElemType.GetName()
		name := fmt.Sprintf("[]%s", elemTypeName)

		return AnalyzedSliceType{
			Name:        name,
			ElementType: sliceElemType,
		}, nil
	case *types.Map:
		keyType, err := toAnalyzedType(t.Key())
		checkError(err)

		elemType, err := toAnalyzedType(t.Elem())
		checkError(err)

		name := fmt.Sprintf("map[%s]%s", keyType.GetName(), elemType.GetName())

		return AnalyzedMapType{
			Name:        name,
			KeyType:     keyType,
			ElementType: elemType,
		}, nil
	case *types.Chan:
		elemType, err := toAnalyzedType(t.Elem())
		checkError(err)

		chanName := "chan"
		switch t.Dir() {
		case types.SendOnly:
			chanName = chanName + "<-"
		case types.RecvOnly:
			chanName = "<-" + chanName
		}
		chanName += " " + elemType.GetName()

		chanDir, err := ChanDirToString(t.Dir())
		checkError(err)

		return AnalyzedChanType{
			Name:        chanName,
			ElementType: elemType,
			Direction:   chanDir,
		}, nil
	case *types.Interface:
		name := "interface{}"
		isError := implementsError(t)
		if !isError {
			return nil, errors.New("currently only error interface is supported")
		}
		return AnalyzedInterfaceType{
			Name: name,
		}, nil
	}
	return nil, fmt.Errorf("unsupported type: %s", typ)
}

type ResultOfChecking int

const (
	SupportedType = iota
	Unknown
	UnsupportedType
)

func checkTypeIsSupported(typ types.Type, visited map[string]ResultOfChecking, isResultType bool, depth int) ResultOfChecking {
	underlyingType := typ.Underlying() // analyze real type, not alias or defined type
	if res, ok := visited[underlyingType.String()]; ok {
		return res
	}
	visited[underlyingType.String()] = UnsupportedType
	var result ResultOfChecking = Unknown
	if pointerType, ok := underlyingType.(*types.Pointer); ok && !isResultType {
		// no support for pointer to pointer and pointer to channel,
		// support for pointer to primitive only if depth is 0
		switch pointerType.Elem().Underlying().(type) {
		case *types.Basic, *types.Chan:
			if depth == 0 {
				result = SupportedType
			} else {
				result = UnsupportedType
			}
		case *types.Pointer:
			result = UnsupportedType
		default:
			result = checkTypeIsSupported(pointerType.Elem(), visited, isResultType, depth+1)
		}
	}
	if _, ok := underlyingType.(*types.Basic); ok {
		result = SupportedType
	}
	if structType, ok := underlyingType.(*types.Struct); ok {
		for i := 0; i < structType.NumFields(); i++ {
			if checkTypeIsSupported(structType.Field(i).Type(), visited, isResultType, depth+1) == UnsupportedType {
				visited[structType.String()] = UnsupportedType
				return UnsupportedType
			}
		}
		result = SupportedType
	}
	if arrayType, ok := underlyingType.(*types.Array); ok {
		result = checkTypeIsSupported(arrayType.Elem(), visited, isResultType, depth+1)
	}
	if sliceType, ok := underlyingType.(*types.Slice); ok {
		result = checkTypeIsSupported(sliceType.Elem(), visited, isResultType, depth+1)
	}
	if mapType, ok := underlyingType.(*types.Map); ok {
		if checkTypeIsSupported(mapType.Key(), visited, isResultType, depth+1) == UnsupportedType ||
			checkTypeIsSupported(mapType.Elem(), visited, isResultType, depth+1) == UnsupportedType {
			result = UnsupportedType
		} else {
			result = SupportedType
		}
	}
	if chanType, ok := underlyingType.(*types.Chan); ok && !isResultType && depth == 0 {
		result = checkTypeIsSupported(chanType.Elem(), visited, isResultType, depth+1)
	}
	if interfaceType, ok := underlyingType.(*types.Interface); ok && isResultType {
		if implementsError(interfaceType) {
			result = SupportedType
		} else {
			result = UnsupportedType
		}
	}

	if result == Unknown {
		result = UnsupportedType
	}
	visited[underlyingType.String()] = result
	return visited[underlyingType.String()]
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
	visited := make(map[string]ResultOfChecking, signature.Results().Len()+signature.Params().Len())
	if results := signature.Results(); results != nil {
		for i := 0; i < results.Len(); i++ {
			resultType := results.At(i).Type().Underlying()
			if _, ok := visited[resultType.String()]; ok {
				continue
			}
			checkTypeIsSupported(resultType, visited, true, 0)
			if visited[resultType.String()] == UnsupportedType {
				return false
			}
		}
	}
	if parameters := signature.Params(); parameters != nil {
		for i := 0; i < parameters.Len(); i++ {
			paramType := parameters.At(i).Type().Underlying()
			if _, ok := visited[paramType.String()]; ok {
				continue
			}
			checkTypeIsSupported(paramType, visited, false, 0)
			if visited[paramType.String()] == UnsupportedType {
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
