package main

import (
	"fmt"
	"go/ast"
	"go/token"
	"go/types"
	"sort"
	"strconv"
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

func toAnalyzedType(
	typ types.Type,
	analyzedTypes map[string]AnalyzedType,
	typeToIndex map[string]string,
	sourcePackage Package,
	currentPackage Package,
	info *types.Info,
) string {
	if index, ok := typeToIndex[typ.String()]; ok {
		return index
	}

	var result AnalyzedType
	indexOfResult := strconv.Itoa(len(typeToIndex))
	typeToIndex[typ.String()] = indexOfResult

	switch t := typ.(type) {
	case *types.Pointer:
		indexOfElemType := toAnalyzedType(t.Elem(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)

		result = AnalyzedPointerType{
			Name:        "*",
			ElementType: indexOfElemType,
		}
	case *types.Named:
		name := t.Obj().Name()

		var pkg Package
		if p := t.Obj().Pkg(); p != nil {
			pkg.PackageName = p.Name()
			pkg.PackagePath = p.Path()
		}

		isError := implementsError(t)

		indexOfUnderlyingType := toAnalyzedType(t.Underlying(), analyzedTypes, typeToIndex, sourcePackage, pkg, info)

		result = AnalyzedNamedType{
			Name:            name,
			SourcePackage:   pkg,
			ImplementsError: isError,
			UnderlyingType:  indexOfUnderlyingType,
		}
	case *types.Basic:
		name := t.Name()
		result = AnalyzedPrimitiveType{Name: name}
	case *types.Struct:
		fields := make([]AnalyzedField, 0, t.NumFields())
		for i := 0; i < t.NumFields(); i++ {
			field := t.Field(i)
			if currentPackage != sourcePackage && !field.Exported() {
				continue
			}

			fieldType := toAnalyzedType(field.Type(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)

			fields = append(fields, AnalyzedField{field.Name(), fieldType, field.Exported()})
		}

		result = AnalyzedStructType{
			Name:   "struct{}",
			Fields: fields,
		}
	case *types.Array:
		indexOfArrayElemType := toAnalyzedType(t.Elem(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)

		length := t.Len()

		result = AnalyzedArrayType{
			Name:        "[_]",
			ElementType: indexOfArrayElemType,
			Length:      length,
		}
	case *types.Slice:
		indexOfSliceElemType := toAnalyzedType(t.Elem(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)

		result = AnalyzedSliceType{
			Name:        "[]",
			ElementType: indexOfSliceElemType,
		}
	case *types.Map:
		indexOfKeyType := toAnalyzedType(t.Key(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)
		indexOfElemType := toAnalyzedType(t.Elem(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)

		result = AnalyzedMapType{
			Name:        "map",
			KeyType:     indexOfKeyType,
			ElementType: indexOfElemType,
		}
	case *types.Chan:
		indexOfElemType := toAnalyzedType(t.Elem(), analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)

		chanDir, err := ChanDirToString(t.Dir())
		checkError(err)

		result = AnalyzedChanType{
			Name:        "chan",
			ElementType: indexOfElemType,
			Direction:   chanDir,
		}
	case *types.Interface:
		implementations := make([]string, 0)
		used := make(map[string]bool)
		for _, typeAndValue := range info.Types {
			switch typeAndValue.Type.(type) {
			case *types.Struct, *types.Signature, *types.Tuple, *types.TypeParam, *types.Union:
				continue
			case *types.Basic:
				b := typeAndValue.Type.(*types.Basic)
				switch b.Kind() {
				case types.UntypedBool, types.UntypedInt, types.UntypedRune, types.UntypedFloat, types.UntypedComplex, types.UntypedString, types.UntypedNil:
					continue
				}
			}
			if !types.IsInterface(typeAndValue.Type) && types.Implements(typeAndValue.Type, t) {
				analyzedType := toAnalyzedType(typeAndValue.Type, analyzedTypes, typeToIndex, sourcePackage, currentPackage, info)
				if used[analyzedType] {
					continue
				}
				used[analyzedType] = true
				implementations = append(implementations, analyzedType)
			}
		}
		result = AnalyzedInterfaceType{
			Name:            "interface{}",
			Implementations: implementations,
		}
	default:
		err := fmt.Errorf("unsupported type: %s", typ.Underlying())
		checkError(err)
	}
	analyzedTypes[indexOfResult] = result
	return indexOfResult
}

type ResultOfChecking int

const (
	SupportedType = iota
	Unknown
	UnsupportedType
)

func checkTypeIsSupported(
	typ types.Type,
	visited map[string]ResultOfChecking,
	isResultType bool,
	sourcePackage Package,
	currentPackage Package,
	depth int,
) ResultOfChecking {
	if res, ok := visited[typ.String()]; ok {
		return res
	}
	var result ResultOfChecking = Unknown
	visited[typ.String()] = result
	switch t := typ.(type) {
	case *types.Pointer:
		// no support for pointer to pointer and pointer to channel,
		// support for pointer to primitive only if depth is 0
		switch t.Elem().Underlying().(type) {
		case *types.Basic:
			if depth == 0 {
				result = SupportedType
			} else {
				result = UnsupportedType
			}
		case *types.Chan:
			if depth == 0 && !isResultType {
				result = SupportedType
			} else {
				result = UnsupportedType
			}
		case *types.Pointer:
			result = UnsupportedType
		default:
			result = checkTypeIsSupported(t.Elem(), visited, isResultType, sourcePackage, currentPackage, depth+1)
		}
	case *types.Named:
		var pkg Package
		if p := t.Obj().Pkg(); p != nil {
			pkg.PackageName = p.Name()
			pkg.PackagePath = p.Path()
		}
		result = checkTypeIsSupported(t.Underlying(), visited, isResultType, sourcePackage, pkg, depth+1)
	case *types.Basic:
		result = SupportedType
	case *types.Struct:
		for i := 0; i < t.NumFields(); i++ {
			field := t.Field(i)
			if currentPackage != sourcePackage && !field.Exported() {
				continue
			}

			if checkTypeIsSupported(field.Type(), visited, isResultType, sourcePackage, currentPackage, depth+1) == UnsupportedType {
				visited[t.String()] = UnsupportedType
				return UnsupportedType
			}
		}
		result = SupportedType
	case *types.Array:
		result = checkTypeIsSupported(t.Elem(), visited, isResultType, sourcePackage, currentPackage, depth+1)
	case *types.Slice:
		result = checkTypeIsSupported(t.Elem(), visited, isResultType, sourcePackage, currentPackage, depth+1)
	case *types.Map:
		if checkTypeIsSupported(t.Key(), visited, isResultType, sourcePackage, currentPackage, depth+1) == UnsupportedType ||
			checkTypeIsSupported(t.Elem(), visited, isResultType, sourcePackage, currentPackage, depth+1) == UnsupportedType {
			result = UnsupportedType
		} else {
			result = SupportedType
		}
	case *types.Chan:
		if !isResultType && depth == 0 {
			result = checkTypeIsSupported(t.Elem(), visited, isResultType, sourcePackage, currentPackage, depth+1)
		}
	case *types.Interface:
		if isResultType {
			if implementsError(t) && depth == 0 {
				result = SupportedType
			} else {
				result = UnsupportedType
			}
		} else {
			result = SupportedType
		}
	}

	if result == Unknown {
		result = UnsupportedType
	}
	visited[typ.String()] = result
	return visited[typ.String()]
}

func checkIsSupported(signature *types.Signature, sourcePackage Package) bool {
	if signature.TypeParams() != nil { // has type params
		return false
	}
	if signature.Variadic() { // is variadic
		return false
	}
	visited := make(map[string]ResultOfChecking, signature.Results().Len()+signature.Params().Len())
	if signature.Recv() != nil {
		receiverType := signature.Recv().Type()
		checkTypeIsSupported(receiverType, visited, false, sourcePackage, sourcePackage, 0)
		if visited[receiverType.String()] == UnsupportedType {
			return false
		}
	}
	if results := signature.Results(); results != nil {
		for i := 0; i < results.Len(); i++ {
			resultType := results.At(i).Type().Underlying()
			if _, ok := visited[resultType.String()]; ok {
				continue
			}
			checkTypeIsSupported(resultType, visited, true, sourcePackage, sourcePackage, 0)
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
			checkTypeIsSupported(paramType, visited, false, sourcePackage, sourcePackage, 0)
			if visited[paramType.String()] == UnsupportedType {
				return false
			}
		}
	}
	return true
}

func extractConstants(info *types.Info, funcDecl *ast.FuncDecl) map[string][]string {
	constantExtractor := ConstantExtractor{info: info, constants: map[string][]string{}}
	ast.Walk(&constantExtractor, funcDecl)
	return constantExtractor.constants
}

func collectTargetAnalyzedFunctions(
	info *types.Info,
	targetFunctionNames []string,
	targetMethodNames []string,
	sourcePackage Package,
) (
	analyzedFunctions []AnalyzedFunction,
	notSupportedFunctionNames []string,
	notFoundFunctionNames []string,
) {
	analyzedFunctions = []AnalyzedFunction{}
	notSupportedFunctionNames = []string{}
	notFoundFunctionNames = []string{}

	foundTargetFunctionNamesMap := map[string]bool{}
	for _, functionName := range targetFunctionNames {
		foundTargetFunctionNamesMap[functionName] = false
	}

	foundTargetMethodNamesMap := map[string]bool{}
	for _, functionName := range targetMethodNames {
		foundTargetMethodNamesMap[functionName] = false
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
					Name:        typedObj.Name(),
					Parameters:  []AnalyzedVariable{},
					ResultTypes: []AnalyzedVariable{},
					Constants:   map[string][]string{},
					position:    typedObj.Pos(),
				}

				signature := typedObj.Type().(*types.Signature)
				if signature.Recv() != nil {
					mutex.Lock()
					if isFound, ok := foundTargetMethodNamesMap[analyzedFunction.Name]; !ok || isFound {
						mutex.Unlock()
						return
					} else {
						foundTargetMethodNamesMap[analyzedFunction.Name] = true
						mutex.Unlock()
					}
				} else {
					mutex.Lock()
					if isFound, ok := foundTargetFunctionNamesMap[analyzedFunction.Name]; !ok || isFound {
						mutex.Unlock()
						return
					} else {
						foundTargetFunctionNamesMap[analyzedFunction.Name] = true
						mutex.Unlock()
					}
				}

				if !checkIsSupported(signature, sourcePackage) {
					mutex.Lock()
					notSupportedFunctionNames = append(notSupportedFunctionNames, analyzedFunction.Name)
					mutex.Unlock()
					return
				}
				analyzedTypes := make(map[string]AnalyzedType, signature.Params().Len()+signature.Results().Len())
				typeToIndex := make(map[string]string, signature.Params().Len()+signature.Results().Len())
				if receiver := signature.Recv(); receiver != nil {
					receiverType := toAnalyzedType(receiver.Type(), analyzedTypes, typeToIndex, sourcePackage, sourcePackage, info)
					analyzedFunction.Receiver = &AnalyzedVariable{
						Name: receiver.Name(),
						Type: receiverType,
					}
				}
				if parameters := signature.Params(); parameters != nil {
					for i := 0; i < parameters.Len(); i++ {
						parameter := parameters.At(i)
						parameterType := toAnalyzedType(parameter.Type(), analyzedTypes, typeToIndex, sourcePackage, sourcePackage, info)
						analyzedFunction.Parameters = append(
							analyzedFunction.Parameters,
							AnalyzedVariable{
								Name: parameter.Name(),
								Type: parameterType,
							},
						)
					}
				}
				if results := signature.Results(); results != nil {
					for i := 0; i < results.Len(); i++ {
						result := results.At(i)
						resultType := toAnalyzedType(result.Type(), analyzedTypes, typeToIndex, sourcePackage, sourcePackage, info)
						analyzedFunction.ResultTypes = append(
							analyzedFunction.ResultTypes,
							AnalyzedVariable{
								Name: result.Name(),
								Type: resultType,
							},
						)
					}
				}

				if ident.Obj != nil {
					funcDecl := ident.Obj.Decl.(*ast.FuncDecl)
					analyzedFunction.Constants = extractConstants(info, funcDecl)
				}
				analyzedFunction.Types = analyzedTypes

				mutex.Lock()
				analyzedFunctions = append(analyzedFunctions, analyzedFunction)
				mutex.Unlock()
			}(ident, typedObj)
		}
	}

	wg.Wait()

	for functionName, isFound := range foundTargetFunctionNamesMap {
		if !isFound {
			notFoundFunctionNames = append(notFoundFunctionNames, functionName)
		}
	}
	for methodName, isFound := range foundTargetMethodNamesMap {
		if !isFound {
			notFoundFunctionNames = append(notFoundFunctionNames, methodName)
		}
	}
	sort.Slice(analyzedFunctions, func(i, j int) bool {
		return analyzedFunctions[i].position < analyzedFunctions[j].position
	})
	sort.Strings(notSupportedFunctionNames)
	sort.Strings(notFoundFunctionNames)
	return
}
