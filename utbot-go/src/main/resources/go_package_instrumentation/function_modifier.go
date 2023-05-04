package main

import (
	"fmt"
	"go/ast"
	"go/token"
	"strconv"
)

type FunctionModifier struct {
	lineCounter        int
	currFunction       string
	testedFunctions    map[string]struct{}
	functionToCounters map[string][]string
}

func (f *FunctionModifier) Visit(node ast.Node) ast.Visitor {
	switch n := node.(type) {
	case *ast.FuncDecl:
		n.Doc = nil
		f.currFunction = n.Name.Name
	case *ast.BlockStmt:
		if n == nil {
			n = &ast.BlockStmt{}
		}
		n.List = f.addCounter(n.List)
		return nil
	case *ast.IfStmt:
		if n.Body == nil {
			return nil
		}
		ast.Walk(f, n.Body)
		if n.Else == nil {
			n.Else = &ast.BlockStmt{}
		}
		switch stmt := n.Else.(type) {
		case *ast.IfStmt:
			n.Else = &ast.BlockStmt{List: []ast.Stmt{stmt}}
		}
		ast.Walk(f, n.Else)
		return nil
	case *ast.ForStmt:
		if n.Body == nil {
			return nil
		}
		ast.Walk(f, n.Body)
		return nil
	case *ast.RangeStmt:
		if n.Body == nil {
			return nil
		}
		ast.Walk(f, n.Body)
		return nil
	case *ast.SwitchStmt:
		hasDefault := false
		if n.Body == nil {
			n.Body = &ast.BlockStmt{}
		}
		for _, stmt := range n.Body.List {
			if cas, ok := stmt.(*ast.CaseClause); ok && cas.List == nil {
				hasDefault = true
				break
			}
		}
		if !hasDefault {
			n.Body.List = append(n.Body.List, &ast.CaseClause{})
		}
		for _, stmt := range n.Body.List {
			ast.Walk(f, stmt)
		}
		return nil
	case *ast.TypeSwitchStmt:
		hasDefault := false
		if n.Body == nil {
			n.Body = &ast.BlockStmt{}
		}
		for _, stmt := range n.Body.List {
			if cas, ok := stmt.(*ast.CaseClause); ok && cas.List == nil {
				hasDefault = true
				break
			}
		}
		if !hasDefault {
			n.Body.List = append(n.Body.List, &ast.CaseClause{})
		}
		for _, stmt := range n.Body.List {
			ast.Walk(f, stmt)
		}
		return nil
	case *ast.SelectStmt:
		if n.Body == nil {
			return nil
		}
		for _, stmt := range n.Body.List {
			ast.Walk(f, stmt)
		}
		return nil
	case *ast.CaseClause:
		for _, expr := range n.List {
			ast.Walk(f, expr)
		}
		n.Body = f.addCounter(n.Body)
		return nil
	case *ast.CommClause:
		ast.Walk(f, n.Comm)
		n.Body = f.addCounter(n.Body)
		return nil
	}
	return f
}

func (f *FunctionModifier) addCounter(stmts []ast.Stmt) []ast.Stmt {
	if len(stmts) == 0 {
		return []ast.Stmt{f.newCounter()}
	}
	if f.currFunction == "Test" {
		println(1)
	}

	var newList []ast.Stmt
	for _, stmt := range stmts {
		newList = append(newList, f.newCounter())
		ast.Walk(f, stmt)
		newList = append(newList, stmt)
		if _, ok := stmt.(*ast.ReturnStmt); ok {
			break
		}
	}
	return newList
}

func (f *FunctionModifier) newCounter() ast.Stmt {
	funcName := f.currFunction
	f.lineCounter++
	cnt := strconv.Itoa(f.lineCounter)
	if _, ok := f.testedFunctions[funcName]; ok {
		f.functionToCounters[funcName] = append(f.functionToCounters[funcName], cnt)
	}

	idx := &ast.BasicLit{
		Kind:  token.STRING,
		Value: fmt.Sprintf("\"%s\"", cnt),
	}
	counter := &ast.IndexExpr{
		X:     ast.NewIdent("__CoverTab__"),
		Index: idx,
	}
	return &ast.IncDecStmt{
		X:   counter,
		Tok: token.INC,
	}
}
