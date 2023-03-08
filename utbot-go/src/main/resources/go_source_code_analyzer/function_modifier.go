package main

import (
	"go/ast"
	"go/token"
	"strconv"
)

type FunctionModifier struct {
	lineCounter int
	maxTraceLen int
}

func createNewFunctionName(funcDecl *ast.FuncDecl) *ast.Ident {
	funcName := funcDecl.Name.String()
	return ast.NewIdent("__" + funcName + "__")
}

func (v *FunctionModifier) ModifyFunctionDeclaration(funcDecl *ast.FuncDecl) {
	funcDecl.Name = createNewFunctionName(funcDecl)

	traceName := ast.NewIdent("__trace__")
	obj := ast.Object{
		Kind: ast.Var,
		Name: "__trace__",
		Decl: &traceName,
	}
	traceName.Obj = &obj

	traceParam := ast.Field{
		Names: []*ast.Ident{
			traceName,
		},
		Type: &ast.StarExpr{
			X: &ast.ArrayType{
				Elt: ast.NewIdent("uint16"),
			},
		},
	}

	params := &funcDecl.Type.Params.List
	*params = append(*params, &traceParam)
}

func (v *FunctionModifier) Visit(node ast.Node) ast.Visitor {
	switch n := node.(type) {
	case *ast.FuncDecl:
		n.Doc = nil
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

func (v *FunctionModifier) addLinesWithLoggingInTraceBeforeFirstReturnStatement(stmts []ast.Stmt) []ast.Stmt {
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

func (v *FunctionModifier) newLineWithLoggingInTrace() ast.Stmt {
	v.lineCounter++

	trace := ast.StarExpr{
		X: ast.NewIdent("__trace__"),
	}
	return &ast.IfStmt{
		Cond: &ast.BinaryExpr{
			X: &ast.CallExpr{
				Fun:  ast.NewIdent("len"),
				Args: []ast.Expr{&trace},
			},
			Op: token.LSS,
			Y: &ast.BasicLit{
				Kind:  token.INT,
				Value: strconv.Itoa(v.maxTraceLen),
			},
		},
		Body: &ast.BlockStmt{
			List: []ast.Stmt{
				&ast.AssignStmt{
					Lhs: []ast.Expr{&trace},
					Tok: token.ASSIGN,
					Rhs: []ast.Expr{
						&ast.CallExpr{
							Fun: ast.NewIdent("append"),
							Args: []ast.Expr{
								&trace,
								&ast.BasicLit{
									Kind:  token.INT,
									Value: strconv.Itoa(v.lineCounter),
								},
							},
						},
					},
				},
			},
		},
	}
}
