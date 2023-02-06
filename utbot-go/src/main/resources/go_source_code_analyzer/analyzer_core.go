package main

import (
	"bytes"
	"errors"
	"fmt"
	"go/ast"
	"go/importer"
	"go/parser"
	"go/printer"
	"go/token"
	"go/types"
	"sort"
)

var errorInterface = func() *types.Interface {
	// TODO not sure if it's best way
	src := `package src

import "errors"

var x = errors.New("")
`
	fset := token.NewFileSet()
	fileAst, astErr := parser.ParseFile(fset, "", src, 0)
	checkError(astErr)
	typesConfig := types.Config{Importer: importer.Default()}
	info := &types.Info{
		Defs:  make(map[*ast.Ident]types.Object),
		Uses:  make(map[*ast.Ident]types.Object),
		Types: make(map[ast.Expr]types.TypeAndValue),
	}
	_, typesCheckErr := typesConfig.Check("", fset, []*ast.File{fileAst}, info)
	checkError(typesCheckErr)
	for _, obj := range info.Defs {
		if obj != nil {
			return obj.Type().Underlying().(*types.Interface)
		}
	}
	return nil
}()

func implementsError(typ *types.Named) bool {
	return types.Implements(typ, errorInterface)
}

//goland:noinspection GoPreferNilSlice
func toAnalyzedType(typ types.Type) (AnalyzedType, error) {
	var result AnalyzedType
	underlyingType := typ.Underlying()
	switch underlyingType.(type) {
	case *types.Basic:
		basicType := underlyingType.(*types.Basic)
		name := basicType.Name()
		result = AnalyzedPrimitiveType{Name: name}
	case *types.Struct:
		namedType := typ.(*types.Named)
		name := namedType.Obj().Name()
		pkg := namedType.Obj().Pkg()
		isError := implementsError(namedType)

		structType := underlyingType.(*types.Struct)
		fields := []AnalyzedField{}
		for i := 0; i < structType.NumFields(); i++ {
			field := structType.Field(i)

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
	case *types.Interface:
		namedType := typ.(*types.Named)
		name := namedType.Obj().Name()

		isError := implementsError(namedType)
		if !isError {
			return nil, errors.New("currently only error interface is supported")
		}
		result = AnalyzedInterfaceType{
			Name:            fmt.Sprintf("interface %s", name),
			ImplementsError: isError,
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
	if interfaceType, ok := underlyingType.(*types.Interface); ok && isResultType {
		return interfaceType == errorInterface
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

func collectTargetAnalyzedFunctions(fset *token.FileSet, info *types.Info, targetFunctionsNames []string) (
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

	for ident, obj := range info.Defs {
		switch typedObj := obj.(type) {
		case *types.Func:
			analyzedFunction := AnalyzedFunction{
				Name:         typedObj.Name(),
				ModifiedName: createNewFunctionName(typedObj.Name()),
				Parameters:   []AnalyzedFunctionParameter{},
				ResultTypes:  []AnalyzedType{},
				position:     typedObj.Pos(),
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

					parameterType, err := toAnalyzedType(parameter.Type())
					checkError(err)

					analyzedFunction.Parameters = append(analyzedFunction.Parameters,
						AnalyzedFunctionParameter{
							Name: parameter.Name(),
							Type: parameterType,
						})
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
			funcDecl.Name = ast.NewIdent(analyzedFunction.ModifiedName)

			visitor := Visitor{
				counter:         0,
				newFunctionName: analyzedFunction.ModifiedName,
			}
			ast.Walk(&visitor, funcDecl)

			var modifiedFunction bytes.Buffer
			cfg := printer.Config{
				Mode:     printer.TabIndent,
				Tabwidth: 4,
				Indent:   0,
			}
			err := cfg.Fprint(&modifiedFunction, fset, funcDecl)
			checkError(err)

			analyzedFunction.ModifiedFunctionForCollectingTraces = modifiedFunction.String()
			analyzedFunction.NumberOfAllStatements = visitor.counter
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
