package main

import (
	"go/ast"
	"go/token"
	"strconv"
)

type Visitor struct {
	counter         int
	newFunctionName string
}

func (v *Visitor) Visit(node ast.Node) ast.Visitor {
	switch n := node.(type) {
	case *ast.BlockStmt:
		if n == nil {
			n = &ast.BlockStmt{}
		}
		n.List = v.addLinesWithLoggingInTraceBeforeFirstReturnStatement(n.List)
		return nil
	case *ast.IfStmt:
		if n.Body == nil {
			return nil
		}
		ast.Walk(v, n.Body)
		if n.Else == nil {
			n.Else = &ast.BlockStmt{}
		}
		switch stmt := n.Else.(type) {
		case *ast.IfStmt:
			n.Else = &ast.BlockStmt{List: []ast.Stmt{stmt}}
		}
		ast.Walk(v, n.Else)
		return nil
	case *ast.ForStmt:
		if n.Body == nil {
			return nil
		}
		ast.Walk(v, n.Body)
		return nil
	case *ast.RangeStmt:
		if n.Body == nil {
			return nil
		}
		ast.Walk(v, n.Body)
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
			ast.Walk(v, stmt)
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
			ast.Walk(v, stmt)
		}
		return nil
	case *ast.SelectStmt:
		if n.Body == nil {
			return nil
		}
		for _, stmt := range n.Body.List {
			ast.Walk(v, stmt)
		}
		return nil
	case *ast.CaseClause:
		for _, expr := range n.List {
			ast.Walk(v, expr)
		}
		n.Body = v.addLinesWithLoggingInTraceBeforeFirstReturnStatement(n.Body)
		return nil
	case *ast.CommClause:
		ast.Walk(v, n.Comm)
		n.Body = v.addLinesWithLoggingInTraceBeforeFirstReturnStatement(n.Body)
		return nil
	}
	return v
}

func (v *Visitor) addLinesWithLoggingInTraceBeforeFirstReturnStatement(stmts []ast.Stmt) []ast.Stmt {
	if len(stmts) == 0 {
		return []ast.Stmt{v.newLineWithLoggingInTrace()}
	}

	var newList []ast.Stmt
	for _, stmt := range stmts {
		newList = append(newList, v.newLineWithLoggingInTrace())
		ast.Walk(v, stmt)
		newList = append(newList, stmt)
		if _, ok := stmt.(*ast.ReturnStmt); ok {
			break
		}
	}
	return newList
}

func (v *Visitor) newLineWithLoggingInTrace() ast.Stmt {
	v.counter++

	traces := ast.NewIdent("__traces__")
	return &ast.AssignStmt{
		Lhs: []ast.Expr{traces},
		Tok: token.ASSIGN,
		Rhs: []ast.Expr{
			&ast.CallExpr{
				Fun:  ast.NewIdent("append"),
				Args: []ast.Expr{traces, ast.NewIdent(strconv.Itoa(v.counter))},
			},
		},
	}
}

func createNewFunctionName(funcName string) string {
	return "__" + funcName + "__"
}
