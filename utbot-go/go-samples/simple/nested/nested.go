package nested

type unexportedStruct struct {
	int
}

type ExportedStruct struct {
	unexportedStruct
}
