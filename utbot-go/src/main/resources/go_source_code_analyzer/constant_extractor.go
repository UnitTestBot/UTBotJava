package main

import (
	"fmt"
	"go/ast"
	"go/constant"
	"go/types"
)

type ConstantExtractor struct {
	info      *types.Info
	constants map[string][]string
}

func (e *ConstantExtractor) Visit(node ast.Node) ast.Visitor {
	if _, ok := node.(*ast.BasicLit); !ok {
		return e
	}

	expr, ok := node.(ast.Expr)
	if !ok {
		return e
	}

	typeAndValue, ok := e.info.Types[expr]
	if !ok {
		return e
	}

	var t = typeAndValue.Type
	basicType, ok := t.(*types.Basic)
	if !ok {
		return e
	}

	switch basicType.Kind() {
	case types.Int, types.Int8, types.Int16, types.Int32, types.Int64:
		e.constants[basicType.Name()] = append(e.constants[basicType.Name()], typeAndValue.Value.String())
	case types.Uint, types.Uint8, types.Uint16, types.Uint32, types.Uint64, types.Uintptr:
		e.constants[basicType.Name()] = append(e.constants[basicType.Name()], typeAndValue.Value.String())
	case types.Float32:
		if f32, ok := constant.Float32Val(typeAndValue.Value); ok {
			e.constants[basicType.Name()] = append(e.constants[basicType.Name()], fmt.Sprintf("%v", f32))
		}
	case types.Float64:
		if f64, ok := constant.Float64Val(typeAndValue.Value); ok {
			e.constants[basicType.Name()] = append(e.constants[basicType.Name()], fmt.Sprintf("%v", f64))
		}
	case types.String:
		e.constants[basicType.Name()] = append(e.constants[basicType.Name()], constant.StringVal(typeAndValue.Value))
	}

	return e
}
