package main

import (
	"crypto/sha1"
	"go/ast"
	"go/token"
	"strconv"
)

type Instrumentator struct {
	lineCounter        int
	currFunction       string
	testedFunctions    map[string]bool
	functionToCounters map[string][]string
}

func NewInstrumentator(testedFunctions map[string]bool) Instrumentator {
	return Instrumentator{
		testedFunctions:    testedFunctions,
		functionToCounters: make(map[string][]string),
	}
}

func (f *Instrumentator) Visit(node ast.Node) ast.Visitor {
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

func (f *Instrumentator) addCounter(stmts []ast.Stmt) []ast.Stmt {
	if len(stmts) == 0 {
		return []ast.Stmt{f.newCounter()}
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

func (f *Instrumentator) getNextCounter() int {
	f.lineCounter++
	id := f.lineCounter
	buf := []byte{byte(id), byte(id >> 8), byte(id >> 16), byte(id >> 24)}
	hash := sha1.Sum(buf)
	return int(uint16(hash[0]) | uint16(hash[1])<<8)
}

func (f *Instrumentator) newCounter() ast.Stmt {
	cnt := strconv.Itoa(f.getNextCounter())

	funcName := f.currFunction
	if ok := f.testedFunctions[funcName]; ok {
		f.functionToCounters[funcName] = append(f.functionToCounters[funcName], cnt)
	}

	idx := &ast.BasicLit{
		Kind:  token.INT,
		Value: cnt,
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
