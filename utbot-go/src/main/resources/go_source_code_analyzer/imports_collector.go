package main

import (
	"fmt"
	"go/ast"
	"go/types"
	"log"
)

type Package struct {
	PackageName string `json:"packageName"`
	PackagePath string `json:"packagePath"`
}

type Import struct {
	Package Package `json:"goPackage"`
	Alias   string  `json:"alias"`
}

type ImportsCollector struct {
	info             *types.Info
	prevPkg          *Package // for handling alias "."
	requiredImports  map[Import]bool
	allImportsInFile map[Import]bool
	sourcePackage    Package
}

func (i *ImportsCollector) Visit(node ast.Node) ast.Visitor {
	switch n := node.(type) {
	case *ast.Ident:
		if obj := i.info.ObjectOf(n); obj != nil && obj.Pkg() != nil {
			var typesPkg *types.Package

			switch o := obj.(type) {
			case *types.PkgName:
				typesPkg = o.Imported()
			case *types.Func, *types.TypeName:
				if i.prevPkg == nil {
					var nextImport = Import{
						Package: Package{
							PackageName: o.Pkg().Name(),
							PackagePath: o.Pkg().Path(),
						},
						Alias: ".",
					}
					fmt.Println(obj.Name())
					if _, ok := i.allImportsInFile[nextImport]; ok {
						i.requiredImports[nextImport] = true
					} else if i.sourcePackage != nextImport.Package {
						log.Fatal(fmt.Sprintf("not found import for %s", obj.Name()))
					}
				}
				i.prevPkg = nil
				return i
			default:
				return i
			}

			var pkg = Package{
				PackageName: typesPkg.Name(),
				PackagePath: typesPkg.Path(),
			}
			i.prevPkg = &pkg

			var alias = ""
			if n.Name != pkg.PackageName {
				alias = n.Name
			}
			var nextImport = Import{Package: pkg, Alias: alias}
			i.requiredImports[nextImport] = true
		}
	}
	return i
}
