package org.utbot.go.worker

import org.utbot.go.api.GoNamedTypeId
import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.simplecodegeneration.GoUtModelToCodeConverter

object GoCodeTemplates {

    private val errorMessages = """
        var (
        	ErrParsingValue                  = "failed to parse %s value: %s"
        	ErrInvalidTypeName               = "invalid type name: %s"
        	ErrStringToReflectTypeFailure    = "failed to convert '%s' to reflect.Type: %s"
        	ErrRawValueToReflectValueFailure = "failed to convert RawValue to reflect.Value: %s"
        	ErrReflectValueToRawValueFailure = "failed to convert reflect.Value to RawValue: %s"
        )
    """.trimIndent()

    private val testInputStruct = """
        type __TestInput__ struct {
        	FunctionName string                   `json:"functionName"`
        	Arguments    []map[string]interface{} `json:"arguments"`
        }
    """.trimIndent()

    private val rawValueInterface = """
        type __RawValue__ interface {
        	__toReflectValue__() (reflect.Value, error)
        }
    """.trimIndent()

    private val primitiveValueStruct = """
        type __PrimitiveValue__ struct {
        	Type  string `json:"type"`
        	Value string `json:"value"`
        }
    """.trimIndent()

    private val primitiveValueToReflectValueMethod = """
        func (v __PrimitiveValue__) __toReflectValue__() (reflect.Value, error) {
        	const complexPartsDelimiter = "@"

        	switch v.Type {
        	case "bool":
        		value, err := strconv.ParseBool(v.Value)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(value), nil
        	case "int":
        		value, err := strconv.Atoi(v.Value)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(value), nil
        	case "int8":
        		value, err := strconv.ParseInt(v.Value, 10, 8)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(int8(value)), nil
        	case "int16":
        		value, err := strconv.ParseInt(v.Value, 10, 16)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(int16(value)), nil
        	case "int32":
        		value, err := strconv.ParseInt(v.Value, 10, 32)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(int32(value)), nil
        	case "rune":
        		value, err := strconv.ParseInt(v.Value, 10, 32)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(rune(value)), nil
        	case "int64":
        		value, err := strconv.ParseInt(v.Value, 10, 64)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(value), nil
        	case "byte":
        		value, err := strconv.ParseUint(v.Value, 10, 8)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(byte(value)), nil
        	case "uint":
        		value, err := strconv.ParseUint(v.Value, 10, strconv.IntSize)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(uint(value)), nil
        	case "uint8":
        		value, err := strconv.ParseUint(v.Value, 10, 8)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(uint8(value)), nil
        	case "uint16":
        		value, err := strconv.ParseUint(v.Value, 10, 16)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(uint16(value)), nil
        	case "uint32":
        		value, err := strconv.ParseUint(v.Value, 10, 32)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(uint32(value)), nil
        	case "uint64":
        		value, err := strconv.ParseUint(v.Value, 10, 64)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(value), nil
        	case "float32":
        		value, err := strconv.ParseFloat(v.Value, 32)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(float32(value)), nil
        	case "float64":
        		value, err := strconv.ParseFloat(v.Value, 64)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(value), nil
        	case "complex64":
        		splitValue := strings.Split(v.Value, complexPartsDelimiter)
        		if len(splitValue) != 2 {
        			return reflect.Value{}, fmt.Errorf("not correct complex64 value: %s", v.Value)
        		}
        		realPart, err := strconv.ParseFloat(splitValue[0], 32)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		imaginaryPart, err := strconv.ParseFloat(splitValue[1], 32)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(complex(float32(realPart), float32(imaginaryPart))), nil
        	case "complex128":
        		splitValue := strings.Split(v.Value, complexPartsDelimiter)
        		if len(splitValue) != 2 {
        			return reflect.Value{}, fmt.Errorf("not correct complex128 value: %s", v.Value)
        		}

        		realPart, err := strconv.ParseFloat(splitValue[0], 64)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		imaginaryPart, err := strconv.ParseFloat(splitValue[1], 64)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(complex(realPart, imaginaryPart)), nil
        	case "string":
        		return reflect.ValueOf(v.Value), nil
        	case "uintptr":
        		value, err := strconv.ParseUint(v.Value, 10, strconv.IntSize)
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrParsingValue, v.Type, err)
        		}

        		return reflect.ValueOf(uintptr(value)), nil
        	}
        	return reflect.Value{}, fmt.Errorf("unsupported primitive type: '%s'", v.Type)
        }
    """.trimIndent()

    private val fieldValueStruct = """
        type __FieldValue__ struct {
        	Name       string       `json:"name"`
        	Value      __RawValue__ `json:"value"`
        	IsExported bool         `json:"isExported"`
        }
    """.trimIndent()

    private val structValueStruct = """
        type __StructValue__ struct {
        	Type  string           `json:"type"`
        	Value []__FieldValue__ `json:"value"`
        }
    """.trimIndent()

    private val structValueToReflectValueMethod = """
        func (v __StructValue__) __toReflectValue__() (reflect.Value, error) {
        	structType, err := __convertStringToReflectType__(v.Type)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.Type, err)
        	}

        	structPtr := reflect.New(structType)

        	for _, f := range v.Value {
        		field := structPtr.Elem().FieldByName(f.Name)

        		reflectValue, err := f.Value.__toReflectValue__()
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        		}

        		if field.Type().Kind() == reflect.Uintptr {
        			reflect.NewAt(field.Type(), unsafe.Pointer(field.UnsafeAddr())).Elem().SetUint(reflectValue.Uint())
        		} else {
        			reflect.NewAt(field.Type(), unsafe.Pointer(field.UnsafeAddr())).Elem().Set(reflectValue)
        		}
        	}

        	return structPtr.Elem(), nil
        }
    """.trimIndent()

    private val arrayValueStruct = """
        type __ArrayValue__ struct {
        	Type        string         `json:"type"`
        	ElementType string         `json:"elementType"`
        	Length      int            `json:"length"`
        	Value       []__RawValue__ `json:"value"`
        }
    """.trimIndent()

    private val arrayValueToReflectValueMethod = """
        func (v __ArrayValue__) __toReflectValue__() (reflect.Value, error) {
        	elementType, err := __convertStringToReflectType__(v.ElementType)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.ElementType, err)
        	}

        	arrayType := reflect.ArrayOf(v.Length, elementType)
        	arrayPtr := reflect.New(arrayType)

        	for i := 0; i < v.Length; i++ {
        		element := arrayPtr.Elem().Index(i)

        		reflectValue, err := v.Value[i].__toReflectValue__()
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        		}

        		element.Set(reflectValue)
        	}

        	return arrayPtr.Elem(), nil
        }
    """.trimIndent()

    private val sliceValueStruct = """
        type __SliceValue__ struct {
        	Type        string         `json:"type"`
        	ElementType string         `json:"elementType"`
        	Length      int            `json:"length"`
        	Value       []__RawValue__ `json:"value"`
        }
    """.trimIndent()

    private val sliceValueToReflectValueMethod = """
        func (v __SliceValue__) __toReflectValue__() (reflect.Value, error) {
        	elementType, err := __convertStringToReflectType__(v.ElementType)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.ElementType, err)
        	}

        	sliceType := reflect.SliceOf(elementType)
        	slice := reflect.MakeSlice(sliceType, v.Length, v.Length)
        	slicePtr := reflect.New(slice.Type())
        	slicePtr.Elem().Set(slice)

        	for i := 0; i < len(v.Value); i++ {
        		element := slicePtr.Elem().Index(i)

        		reflectValue, err := v.Value[i].__toReflectValue__()
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        		}

        		element.Set(reflectValue)
        	}

        	return slicePtr.Elem(), nil
        }
    """.trimIndent()

    private val keyValueStruct = """
        type __KeyValue__ struct {
        	Key   __RawValue__ `json:"key"`
        	Value __RawValue__ `json:"value"`
        }
    """.trimIndent()

    private val mapValueStruct = """
        type __MapValue__ struct {
        	Type        string         `json:"type"`
        	KeyType     string         `json:"keyType"`
        	ElementType string         `json:"elementType"`
        	Value       []__KeyValue__ `json:"value"`
        }
    """.trimIndent()

    private val mapValueToReflectValueMethod = """
        func (v __MapValue__) __toReflectValue__() (reflect.Value, error) {
        	keyType, err := __convertStringToReflectType__(v.KeyType)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.KeyType, err)
        	}

        	elementType, err := __convertStringToReflectType__(v.ElementType)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.ElementType, err)
        	}

        	mapType := reflect.MapOf(keyType, elementType)
        	m := reflect.MakeMap(mapType)
        	for _, keyValue := range v.Value {
        		keyReflectValue, err := keyValue.Key.__toReflectValue__()
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        		}

        		valueReflectValue, err := keyValue.Value.__toReflectValue__()
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        		}

        		m.SetMapIndex(keyReflectValue, valueReflectValue)
        	}
        	return m, nil
        }
    """.trimIndent()

    private val chanValueStruct = """
        type __ChanValue__ struct {
        	Type        string         `json:"type"`
        	ElementType string         `json:"elementType"`
        	Direction   string         `json:"direction"`
        	Length      int            `json:"length"`
        	Value       []__RawValue__ `json:"value"`
        }
    """.trimIndent()

    private val chanValueToReflectValueMethod = """
        func (v __ChanValue__) __toReflectValue__() (reflect.Value, error) {
        	elementType, err := __convertStringToReflectType__(v.ElementType)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.ElementType, err)
        	}

        	dir := reflect.BothDir

        	chanType := reflect.ChanOf(dir, elementType)
        	channel := reflect.MakeChan(chanType, v.Length)

        	for i := range v.Value {
        		reflectValue, err := v.Value[i].__toReflectValue__()
        		if err != nil {
        			return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        		}

        		channel.Send(reflectValue)
        	}
        	channel.Close()

        	return channel, nil
        }
    """.trimIndent()

    private val nilValueStruct = """
        type __NilValue__ struct {
        	Type string `json:"type"`
        }
    """.trimIndent()

    private val nilValueToReflectValueMethod = """
        func (v __NilValue__) __toReflectValue__() (reflect.Value, error) {
        	typ, err := __convertStringToReflectType__(v.Type)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.Type, err)
        	}

        	return reflect.Zero(typ), nil
        }
    """.trimIndent()

    private val namedValueStruct = """
        type __NamedValue__ struct {
        	Type  string       `json:"type"`
        	Value __RawValue__ `json:"value"`
        }
    """.trimIndent()

    private val namedValueToReflectValueMethod = """
        func (v __NamedValue__) __toReflectValue__() (reflect.Value, error) {
        	value, err := v.Value.__toReflectValue__()
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        	}

        	typ, err := __convertStringToReflectType__(v.Type)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.Type, err)
        	}

        	return value.Convert(typ), nil
        }
    """.trimIndent()

    private val pointerValueStruct = """
        type __PointerValue__ struct {
        	Type        string       `json:"type"`
        	ElementType string       `json:"elementType"`
        	Value       __RawValue__ `json:"value"`
        }
    """.trimIndent()

    private val pointerValueToReflectValueMethod = """
        func (v __PointerValue__) __toReflectValue__() (reflect.Value, error) {
        	elementType, err := __convertStringToReflectType__(v.ElementType)
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrStringToReflectTypeFailure, v.Type, err)
        	}

        	value, err := v.Value.__toReflectValue__()
        	if err != nil {
        		return reflect.Value{}, fmt.Errorf(ErrRawValueToReflectValueFailure, err)
        	}

        	pointer := reflect.New(elementType)
        	pointer.Elem().Set(value)

        	return pointer, nil
        }
    """.trimIndent()

    private fun convertStringToReflectType(
        namedTypes: Set<GoNamedTypeId>,
        destinationPackage: GoPackage,
        aliases: Map<GoPackage, String?>
    ): String {
        val converter = GoUtModelToCodeConverter(destinationPackage, aliases)
        return """
            func __convertStringToReflectType__(typeName string) (reflect.Type, error) {
            	var result reflect.Type

            	switch {
            	case strings.HasPrefix(typeName, "map["):
            		index := strings.IndexRune(typeName, ']')
            		if index == -1 {
            			return nil, fmt.Errorf(ErrInvalidTypeName, typeName)
            		}

            		keyTypeStr := typeName[4:index]
            		keyType, err := __convertStringToReflectType__(keyTypeStr)
            		if err != nil {
            			return nil, fmt.Errorf(ErrStringToReflectTypeFailure, keyTypeStr, err)
            		}

            		elementTypeStr := typeName[index+1:]
            		elementType, err := __convertStringToReflectType__(elementTypeStr)
            		if err != nil {
            			return nil, fmt.Errorf(ErrStringToReflectTypeFailure, elementTypeStr, err)
            		}

            		result = reflect.MapOf(keyType, elementType)
            	case strings.HasPrefix(typeName, "[]"):
            		index := strings.IndexRune(typeName, ']')
            		if index == -1 {
            			return nil, fmt.Errorf(ErrInvalidTypeName, typeName)
            		}

            		res, err := __convertStringToReflectType__(typeName[index+1:])
            		if err != nil {
            			return nil, fmt.Errorf(ErrInvalidTypeName, typeName)
            		}

            		result = reflect.SliceOf(res)
            	case strings.HasPrefix(typeName, "["):
            		index := strings.IndexRune(typeName, ']')
            		if index == -1 {
            			return nil, fmt.Errorf(ErrInvalidTypeName, typeName)
            		}

            		lengthStr := typeName[1:index]
            		length, err := strconv.Atoi(lengthStr)
            		if err != nil {
            			return nil, err
            		}

            		res, err := __convertStringToReflectType__(typeName[index+1:])
            		if err != nil {
            			return nil, fmt.Errorf(ErrStringToReflectTypeFailure, typeName[index+1:], err)
            		}

            		result = reflect.ArrayOf(length, res)
            	case strings.HasPrefix(typeName, "<-chan") || strings.HasPrefix(typeName, "chan"):
            		dir := reflect.BothDir
            		index := 5
            		if strings.HasPrefix(typeName, "<-chan") {
            			dir = reflect.RecvDir
            			index = 7
            		} else if strings.HasPrefix(typeName, "chan<-") {
            			dir = reflect.SendDir
            			index = 7
            		}

            		elemType, err := __convertStringToReflectType__(typeName[index:])
            		if err != nil {
            			return nil, fmt.Errorf(ErrStringToReflectTypeFailure, typeName[index:], err)
            		}

            		result = reflect.ChanOf(dir, elemType)
            	case strings.HasPrefix(typeName, "*"):
            		elemType, err := __convertStringToReflectType__(typeName[1:])
            		if err != nil {
            			return nil, fmt.Errorf(ErrStringToReflectTypeFailure, typeName[1:], err)
            		}

            		result = reflect.PointerTo(elemType)
            	default:
            		switch typeName {
            		case "bool":
            			result = reflect.TypeOf(true)
            		case "int":
            			result = reflect.TypeOf(0)
            		case "int8":
            			result = reflect.TypeOf(int8(0))
            		case "int16":
            			result = reflect.TypeOf(int16(0))
            		case "int32":
            			result = reflect.TypeOf(int32(0))
            		case "rune":
            			result = reflect.TypeOf(rune(0))
            		case "int64":
            			result = reflect.TypeOf(int64(0))
            		case "byte":
            			result = reflect.TypeOf(byte(0))
            		case "uint":
            			result = reflect.TypeOf(uint(0))
            		case "uint8":
            			result = reflect.TypeOf(uint8(0))
            		case "uint16":
            			result = reflect.TypeOf(uint16(0))
            		case "uint32":
            			result = reflect.TypeOf(uint32(0))
            		case "uint64":
            			result = reflect.TypeOf(uint64(0))
            		case "float32":
            			result = reflect.TypeOf(float32(0))
            		case "float64":
            			result = reflect.TypeOf(float64(0))
            		case "complex64":
            			result = reflect.TypeOf(complex(float32(0), float32(0)))
            		case "complex128":
            			result = reflect.TypeOf(complex(float64(0), float64(0)))
            		case "string":
            			result = reflect.TypeOf("")
            		case "uintptr":
            			result = reflect.TypeOf(uintptr(0))
                ${
            namedTypes.joinToString(separator = "\n") {
                val relativeName = it.getRelativeName(destinationPackage, aliases)
                "case \"${relativeName}\": result = reflect.TypeOf(${converter.toGoCode(it.goDefaultValueModel())})"
            }
        }
            		default:
            			return nil, fmt.Errorf("unsupported type: %s", typeName)
            		}
            	}
            	return result, nil
            }
        """.trimIndent()
    }

    private val panicMessageStruct = """
        type __RawPanicMessage__ struct {
        	RawResultValue  __RawValue__ `json:"rawResultValue"`
        	ImplementsError bool         `json:"implementsError"`
        }
    """.trimIndent()

    private val rawExecutionResultStruct = """
        type __RawExecutionResult__ struct {
        	TimeoutExceeded bool                 `json:"timeoutExceeded"`
        	RawResultValues []__RawValue__       `json:"rawResultValues"`
        	PanicMessage    *__RawPanicMessage__ `json:"panicMessage"`
        	Trace           []uint16             `json:"trace"`
        }
    """.trimIndent()

    private val convertReflectValueOfDefinedTypeToRawValueFunction = """
        func __convertReflectValueOfDefinedTypeToRawValue__(v reflect.Value) (__RawValue__, error) {
        	value, err := __convertReflectValueOfPredeclaredOrNotDefinedTypeToRawValue__(v)
        	if err != nil {
        		return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        	}

        	return __NamedValue__{
        		Type:  v.Type().Name(),
        		Value: value,
        	}, nil
        }
    """.trimIndent()

    private val convertFloat64ValueToStringFunction = """
        func __convertFloat64ValueToString__(value float64) string {
        	const outputNaN = "NaN"
        	const outputPosInf = "+Inf"
        	const outputNegInf = "-Inf"
        	switch {
        	case math.IsNaN(value):
        		return fmt.Sprint(outputNaN)
        	case math.IsInf(value, 1):
        		return fmt.Sprint(outputPosInf)
        	case math.IsInf(value, -1):
        		return fmt.Sprint(outputNegInf)
        	default:
        		return fmt.Sprintf("%v", value)
        	}
        }
    """.trimIndent()

    private val convertReflectValueOfPredeclaredOrNotDefinedTypeToRawValueFunction = """
        func __convertReflectValueOfPredeclaredOrNotDefinedTypeToRawValue__(v reflect.Value) (__RawValue__, error) {
        	const outputComplexPartsDelimiter = "@"

        	switch v.Kind() {
        	case reflect.Bool:
        		return __PrimitiveValue__{
        			Type:  v.Kind().String(),
        			Value: fmt.Sprintf("%#v", v.Bool()),
        		}, nil
        	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
        		return __PrimitiveValue__{
        			Type:  v.Kind().String(),
        			Value: fmt.Sprintf("%#v", v.Int()),
        		}, nil
        	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Uintptr:
        		return __PrimitiveValue__{
        			Type:  v.Kind().String(),
        			Value: fmt.Sprintf("%v", v.Uint()),
        		}, nil
        	case reflect.Float32, reflect.Float64:
        		return __PrimitiveValue__{
        			Type:  v.Kind().String(),
        			Value: __convertFloat64ValueToString__(v.Float()),
        		}, nil
        	case reflect.Complex64, reflect.Complex128:
        		value := v.Complex()
        		realPartString := __convertFloat64ValueToString__(real(value))
        		imagPartString := __convertFloat64ValueToString__(imag(value))
        		return __PrimitiveValue__{
        			Type:  v.Kind().String(),
        			Value: fmt.Sprintf("%v%v%v", realPartString, outputComplexPartsDelimiter, imagPartString),
        		}, nil
        	case reflect.String:
        		return __PrimitiveValue__{
        			Type:  reflect.String.String(),
        			Value: fmt.Sprintf("%v", v.String()),
        		}, nil
        	case reflect.Struct:
        		fields := reflect.VisibleFields(v.Type())
        		resultValues := make([]__FieldValue__, 0, v.NumField())
        		for _, field := range fields {
        			if len(field.Index) != 1 {
        				continue
        			}

        			res, err := __convertReflectValueToRawValue__(v.FieldByName(field.Name))
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}

        			resultValues = append(resultValues, __FieldValue__{
        				Name:       field.Name,
        				Value:      res,
        				IsExported: field.IsExported(),
        			})
        		}
        		return __StructValue__{
        			Type:  "struct{}",
        			Value: resultValues,
        		}, nil
        	case reflect.Array:
        		elem := v.Type().Elem()
        		elementType := elem.String()
        		arrayElementValues := make([]__RawValue__, 0, v.Len())
        		for i := 0; i < v.Len(); i++ {
        			arrayElementValue, err := __convertReflectValueToRawValue__(v.Index(i))
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}

        			arrayElementValues = append(arrayElementValues, arrayElementValue)
        		}
        		length := len(arrayElementValues)
        		return __ArrayValue__{
        			Type:        fmt.Sprintf("[%d]%s", length, elementType),
        			ElementType: elementType,
        			Length:      length,
        			Value:       arrayElementValues,
        		}, nil
        	case reflect.Slice:
        		if v.IsNil() {
        			return __NilValue__{Type: "nil"}, nil
        		}
        		elem := v.Type().Elem()
        		elementType := elem.String()
        		typeName := fmt.Sprintf("[]%s", elementType)
        		sliceElementValues := make([]__RawValue__, 0, v.Len())
        		for i := 0; i < v.Len(); i++ {
        			sliceElementValue, err := __convertReflectValueToRawValue__(v.Index(i))
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}

        			sliceElementValues = append(sliceElementValues, sliceElementValue)
        		}
        		length := len(sliceElementValues)
        		return __SliceValue__{
        			Type:        typeName,
        			ElementType: elementType,
        			Length:      length,
        			Value:       sliceElementValues,
        		}, nil
        	case reflect.Map:
        		if v.IsNil() {
        			return __NilValue__{Type: "nil"}, nil
        		}
        		key := v.Type().Key()
        		keyType := key.String()
        		elem := v.Type().Elem()
        		elementType := elem.String()
        		typeName := fmt.Sprintf("map[%s]%s", keyType, elementType)
        		mapValues := make([]__KeyValue__, 0, v.Len())
        		for iter := v.MapRange(); iter.Next(); {
        			key, err := __convertReflectValueToRawValue__(iter.Key())
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}

        			value, err := __convertReflectValueToRawValue__(iter.Value())
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}

        			mapValues = append(mapValues, __KeyValue__{
        				Key:   key,
        				Value: value,
        			})
        		}
        		return __MapValue__{
        			Type:        typeName,
        			KeyType:     keyType,
        			ElementType: elementType,
        			Value:       mapValues,
        		}, nil
        	case reflect.Chan:
        		if v.IsNil() {
        			return __NilValue__{Type: "nil"}, nil
        		}
        		typeName := v.Type().String()
        		elementType := v.Type().Elem().String()
        		dir := "SENDRECV"
        		if v.Type().ChanDir() == reflect.SendDir {
        			dir = "SENDONLY"
        		} else {
        			dir = "RECVONLY"
        		}
        		length := v.Len()

        		chanElementValues := make([]__RawValue__, 0, v.Len())
        		for {
        			v, ok := v.Recv()
        			if !ok {
        				break
        			}
        			rawValue, err := __convertReflectValueToRawValue__(v)
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}

        			chanElementValues = append(chanElementValues, rawValue)
        		}

        		return __ChanValue__{
        			Type:        typeName,
        			ElementType: elementType,
        			Direction:   dir,
        			Length:      length,
        			Value:       chanElementValues,
        		}, nil
        	case reflect.Interface:
        		if v.Interface() == nil {
        			return __NilValue__{Type: "nil"}, nil
        		}
        		if e, ok := v.Interface().(error); ok {
        			value, err := __convertReflectValueOfPredeclaredOrNotDefinedTypeToRawValue__(reflect.ValueOf(e.Error()))
        			if err != nil {
        				return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        			}
        			return __NamedValue__{
        				Type:  "error",
        				Value: value,
        			}, nil
        		}
        		return nil, fmt.Errorf("unsupported result type: %s", v.Type().String())
        	case reflect.Pointer:
        		if v.IsNil() {
        			return __NilValue__{Type: "nil"}, nil
        		}
        		typeName := v.Type().String()
        		elementType := v.Type().Elem().String()

        		value, err := __convertReflectValueToRawValue__(v.Elem())
        		if err != nil {
        			return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        		}

        		return __PointerValue__{
        			Type:        typeName,
        			ElementType: elementType,
        			Value:       value,
        		}, nil
        	default:
        		return nil, fmt.Errorf("unsupported result type: %s", v.Type().String())
        	}
        }
    """.trimIndent()

    private val convertReflectValueToRawValueFunction = """
        func __convertReflectValueToRawValue__(v reflect.Value) (__RawValue__, error) {
        	if v.Type().PkgPath() != "" {
        		return __convertReflectValueOfDefinedTypeToRawValue__(v)
        	}
        	return __convertReflectValueOfPredeclaredOrNotDefinedTypeToRawValue__(v)
        }
    """.trimIndent()

    private fun executeFunctionFunction(maxTraceLength: Int) = """
        func __executeFunction__(
        	function reflect.Value, arguments []reflect.Value, timeout time.Duration,
        ) __RawExecutionResult__ {
        	ctxWithTimeout, cancel := context.WithTimeout(context.Background(), timeout)
        	defer cancel()

        	trace := make([]uint16, 0, $maxTraceLength)

        	done := make(chan __RawExecutionResult__, 1)
        	go func() {
        		executionResult := __RawExecutionResult__{
        			TimeoutExceeded: false,
        			RawResultValues: []__RawValue__{},
        			PanicMessage:    nil,
        			Trace:           []uint16{},
        		}

        		panicked := true
        		defer func() {
        			panicMessage := recover()
        			if panicked {
        				panicAsError, implementsError := panicMessage.(error)
        				var (
        					resultValue __RawValue__
        					err         error
        				)
        				if implementsError {
        					resultValue, err = __convertReflectValueToRawValue__(reflect.ValueOf(panicAsError.Error()))
        				} else {
        					resultValue, err = __convertReflectValueToRawValue__(reflect.ValueOf(panicMessage))
        				}
        				if err != nil {
        					_, _ = fmt.Fprint(os.Stderr, ErrReflectValueToRawValueFailure, err)
        					os.Exit(1)
        				}

        				executionResult.PanicMessage = &__RawPanicMessage__{
        					RawResultValue:  resultValue,
        					ImplementsError: implementsError,
        				}
        			}
        			executionResult.Trace = trace
        			done <- executionResult
        		}()

        		argumentsWithTrace := append(arguments, reflect.ValueOf(&trace))
        		resultValues, err := __wrapResultValues__(function.Call(argumentsWithTrace))
        		if err != nil {
        			_, _ = fmt.Fprintf(os.Stderr, "Failed to wrap result values: %s", err)
        			os.Exit(1)
        		}
        		executionResult.RawResultValues = resultValues
        		panicked = false
        	}()

        	select {
        	case timelyExecutionResult := <-done:
        		return timelyExecutionResult
        	case <-ctxWithTimeout.Done():
        		return __RawExecutionResult__{
        			TimeoutExceeded: true,
        			RawResultValues: []__RawValue__{},
        			PanicMessage:    nil,
        			Trace:           trace,
        		}
        	}
        }
    """.trimIndent()

    private val wrapResultValuesForWorkerFunction = """
        func __wrapResultValues__(values []reflect.Value) ([]__RawValue__, error) {
        	rawValues := make([]__RawValue__, 0, len(values))
        	for _, value := range values {
        		resultValue, err := __convertReflectValueToRawValue__(value)
        		if err != nil {
        			return nil, fmt.Errorf(ErrReflectValueToRawValueFailure, err)
        		}

        		rawValues = append(rawValues, resultValue)
        	}
        	return rawValues, nil
        }
    """.trimIndent()

    private val convertRawValuesToReflectValuesFunction = """
        func __convertRawValuesToReflectValues__(values []__RawValue__) ([]reflect.Value, error) {
        	parameters := make([]reflect.Value, 0, len(values))

        	for _, value := range values {
        		reflectValue, err := value.__toReflectValue__()
        		if err != nil {
        			return nil, fmt.Errorf("failed to convert RawValue %s to reflect.Value: %s", value, err)
        		}

        		parameters = append(parameters, reflectValue)
        	}

        	return parameters, nil
        }
    """.trimIndent()

    private val parseTestInputFunction = """
        func __parseTestInput__(decoder *json.Decoder) (funcName string, rawValues []__RawValue__, err error) {
        	var testInput __TestInput__
        	err = decoder.Decode(&testInput)
        	if err != nil {
        		return
        	}

        	funcName = testInput.FunctionName
        	rawValues = make([]__RawValue__, 0, 10)
        	for _, arg := range testInput.Arguments {
        		var rawValue __RawValue__

        		rawValue, err = __parseRawValue__(arg, "")
        		if err != nil {
        			return "", nil, fmt.Errorf("failed to parse argument %s of function %s: %s", arg, funcName, err)
        		}

        		rawValues = append(rawValues, rawValue)
        	}

        	return
        }
    """.trimIndent()

    private val parseRawValueFunction = """
        func __parseRawValue__(rawValue map[string]interface{}, name string) (__RawValue__, error) {
        	typeName, ok := rawValue["type"]
        	if !ok {
        		return nil, fmt.Errorf("every RawValue must contain field 'type'")
        	}
        	typeNameStr, ok := typeName.(string)
        	if !ok {
        		return nil, fmt.Errorf("field 'type' must be string")
        	}

        	v, ok := rawValue["value"]
        	if !ok {
        		return __NilValue__{Type: typeNameStr}, nil
        	}

        	switch {
        	case typeNameStr == "struct{}":
        		if name == "" {
        			return nil, fmt.Errorf("anonymous structs is not supported")
        		}

        		value, ok := v.([]interface{})
        		if !ok {
        			return nil, fmt.Errorf("StructValue field 'value' must be array")
        		}

        		values := make([]__FieldValue__, 0, len(value))
        		for _, v := range value {
        			nextValue, err := __parseFieldValue__(v.(map[string]interface{}))
        			if err != nil {
        				return nil, fmt.Errorf("failed to parse field %s of struct: %s", v, err)
        			}

        			values = append(values, nextValue)
        		}

        		return __StructValue__{
        			Type:  name,
        			Value: values,
        		}, nil
        	case strings.HasPrefix(typeNameStr, "map["):
        		keyType, ok := rawValue["keyType"]
        		if !ok {
        			return nil, fmt.Errorf("MapValue must contain field 'keyType'")
        		}
        		keyTypeStr, ok := keyType.(string)
        		if !ok {
        			return nil, fmt.Errorf("MapValue field 'keyType' must be string")
        		}

        		elementType, ok := rawValue["elementType"]
        		if !ok {
        			return nil, fmt.Errorf("MapValue must contain field 'elementType'")
        		}
        		elementTypeStr, ok := elementType.(string)
        		if !ok {
        			return nil, fmt.Errorf("MapValue field 'elementType' must be string")
        		}

        		value, ok := v.([]interface{})
        		if !ok {
        			return nil, fmt.Errorf("MapValue field 'value' must be array")
        		}

        		values := make([]__KeyValue__, 0, len(value))
        		for _, v := range value {
        			nextValue, err := __parseKeyValue__(v.(map[string]interface{}))
        			if err != nil {
        				return nil, fmt.Errorf("failed to parse KeyValue %s of map: %s", v, err)
        			}

        			values = append(values, nextValue)
        		}

        		return __MapValue__{
        			Type:        typeNameStr,
        			KeyType:     keyTypeStr,
        			ElementType: elementTypeStr,
        			Value:       values,
        		}, nil
        	case strings.HasPrefix(typeNameStr, "[]"):
        		elementType, ok := rawValue["elementType"]
        		if !ok {
        			return nil, fmt.Errorf("SliceValue must contain field 'elementType'")
        		}
        		elementTypeStr, ok := elementType.(string)
        		if !ok {
        			return nil, fmt.Errorf("SliceValue field 'elementType' must be string")
        		}

        		if _, ok := rawValue["length"]; !ok {
        			return nil, fmt.Errorf("SliceValue must contain field 'length'")
        		}
        		length, ok := rawValue["length"].(float64)
        		if !ok {
        			return nil, fmt.Errorf("SliceValue field 'length' must be float64")
        		}

        		value, ok := v.([]interface{})
        		if !ok || len(value) != int(length) {
        			return nil, fmt.Errorf("SliceValue field 'value' must be array of length %d", int(length))
        		}

        		values := make([]__RawValue__, 0, len(value))
        		for i, v := range value {
        			nextValue, err := __parseRawValue__(v.(map[string]interface{}), "")
        			if err != nil {
        				return nil, fmt.Errorf("failed to parse %d slice element: %s", i, err)
        			}

        			values = append(values, nextValue)
        		}

        		return __SliceValue__{
        			Type:        typeNameStr,
        			ElementType: elementTypeStr,
        			Length:      int(length),
        			Value:       values,
        		}, nil
        	case strings.HasPrefix(typeNameStr, "["):
        		elementType, ok := rawValue["elementType"]
        		if !ok {
        			return nil, fmt.Errorf("ArrayValue must contain field 'elementType'")
        		}
        		elementTypeStr, ok := elementType.(string)
        		if !ok {
        			return nil, fmt.Errorf("ArrayValue field 'elementType' must be string")
        		}

        		if _, ok := rawValue["length"]; !ok {
        			return nil, fmt.Errorf("ArrayValue must contain field 'length'")
        		}
        		length, ok := rawValue["length"].(float64)
        		if !ok {
        			return nil, fmt.Errorf("ArrayValue field 'length' must be float64")
        		}

        		value, ok := v.([]interface{})
        		if !ok || len(value) != int(length) {
        			return nil, fmt.Errorf("ArrayValue field 'value' must be array of length %d", int(length))
        		}

        		values := make([]__RawValue__, 0, len(value))
        		for i, v := range value {
        			nextValue, err := __parseRawValue__(v.(map[string]interface{}), "")
        			if err != nil {
        				return nil, fmt.Errorf("failed to parse %d array element: %s", i, err)
        			}

        			values = append(values, nextValue)
        		}

        		return __ArrayValue__{
        			Type:        typeNameStr,
        			ElementType: elementTypeStr,
        			Length:      int(length),
        			Value:       values,
        		}, nil
        	case strings.HasPrefix(typeNameStr, "<-chan") || strings.HasPrefix(typeNameStr, "chan"):
        		elementType, ok := rawValue["elementType"]
        		if !ok {
        			return nil, fmt.Errorf("ChanValue must contain field 'elementType'")
        		}
        		elementTypeStr, ok := elementType.(string)
        		if !ok {
        			return nil, fmt.Errorf("ChanValue field 'elementType' must be string")
        		}

        		dir, ok := rawValue["direction"]
        		if !ok {
        			return nil, fmt.Errorf("ChanValue must contain field 'direction'")
        		}
        		direction, ok := dir.(string)
        		if !ok {
        			return nil, fmt.Errorf("ChanValue field 'direction' must be string")
        		}

        		if _, ok := rawValue["length"]; !ok {
        			return nil, fmt.Errorf("ChanValue must contain field 'length'")
        		}
        		length, ok := rawValue["length"].(float64)
        		if !ok {
        			return nil, fmt.Errorf("ChanValue field 'length' must be float64")
        		}

        		value, ok := v.([]interface{})
        		if !ok || len(value) != int(length) {
        			return nil, fmt.Errorf("ChanValue field 'value' must be array of length %d", int(length))
        		}

        		values := make([]__RawValue__, 0, len(value))
        		for i, v := range value {
        			nextValue, err := __parseRawValue__(v.(map[string]interface{}), "")
        			if err != nil {
        				return nil, fmt.Errorf("failed to parse %d chan element: %s", i, err)
        			}

        			values = append(values, nextValue)
        		}

        		return __ChanValue__{
        			Type:        typeNameStr,
        			ElementType: elementTypeStr,
        			Direction:   direction,
        			Length:      int(length),
        			Value:       values,
        		}, nil
        	case strings.HasPrefix(typeNameStr, "*"):
        		elementType, ok := rawValue["elementType"]
        		if !ok {
        			return nil, fmt.Errorf("PointerValue must contain field 'elementType'")
        		}
        		elementTypeStr, ok := elementType.(string)
        		if !ok {
        			return nil, fmt.Errorf("PointerValue field 'elementType' must be string")
        		}

        		value, err := __parseRawValue__(v.(map[string]interface{}), "")
        		if err != nil {
        			return nil, fmt.Errorf("failed to parse of PointerValue with type %s: %s", typeNameStr, err)
        		}

        		return __PointerValue__{
        			Type:        typeNameStr,
        			ElementType: elementTypeStr,
        			Value:       value,
        		}, nil
        	default:
        		switch typeNameStr {
        		case "bool", "rune", "int", "int8", "int16", "int32", "int64", "byte", "uint", "uint8", "uint16", "uint32", "uint64", "float32", "float64", "complex64", "complex128", "string", "uintptr":
        			value, ok := v.(string)
        			if !ok {
        				return nil, fmt.Errorf("PrimitiveValue field 'value' must be string")
        			}

        			return __PrimitiveValue__{
        				Type:  typeNameStr,
        				Value: value,
        			}, nil
        		default: // named type
        			value, err := __parseRawValue__(v.(map[string]interface{}), typeNameStr)
        			if err != nil {
        				return nil, fmt.Errorf("failed to parse of NamedValue with type %s: %s", typeNameStr, err)
        			}

        			return __NamedValue__{
        				Type:  typeNameStr,
        				Value: value,
        			}, nil
        		}
        	}
        }
    """.trimIndent()

    private val parseFieldValueFunction = """
        func __parseFieldValue__(p map[string]interface{}) (__FieldValue__, error) {
        	name, ok := p["name"]
        	if !ok {
        		return __FieldValue__{}, fmt.Errorf("FieldValue must contain field 'name'")
        	}
        	nameStr, ok := name.(string)
        	if !ok {
        		return __FieldValue__{}, fmt.Errorf("FieldValue 'name' must be string")
        	}

        	if _, ok := p["value"]; !ok {
        		return __FieldValue__{}, fmt.Errorf("FieldValue must contain field 'value'")
        	}
        	value, err := __parseRawValue__(p["value"].(map[string]interface{}), "")
        	if err != nil {
        		return __FieldValue__{}, err
        	}

        	isExported, ok := p["isExported"]
        	if !ok {
        		return __FieldValue__{}, fmt.Errorf("FieldValue must contain field 'isExported'")
        	}
        	isExportedBool, ok := isExported.(bool)
        	if !ok {
        		return __FieldValue__{}, fmt.Errorf("FieldValue 'isExported' must be bool")
        	}

        	return __FieldValue__{
        		Name:       nameStr,
        		Value:      value,
        		IsExported: isExportedBool,
        	}, nil
        }
    """.trimIndent()

    private val parseKeyValueFunction = """
        func __parseKeyValue__(p map[string]interface{}) (__KeyValue__, error) {
        	if _, ok := p["key"]; !ok {
        		return __KeyValue__{}, fmt.Errorf("KeyValue must contain field 'key'")
        	}
        	key, err := __parseRawValue__(p["key"].(map[string]interface{}), "")
        	if err != nil {
        		return __KeyValue__{}, err
        	}

        	if _, ok := p["value"]; !ok {
        		return __KeyValue__{}, fmt.Errorf("KeyValue must contain field 'value'")
        	}
        	value, err := __parseRawValue__(p["value"].(map[string]interface{}), "")
        	if err != nil {
        		return __KeyValue__{}, err
        	}

        	return __KeyValue__{
        		Key:   key,
        		Value: value,
        	}, nil
        }
    """.trimIndent()

    fun getTopLevelHelperStructsAndFunctionsForWorker(
        namedTypes: Set<GoNamedTypeId>,
        destinationPackage: GoPackage,
        aliases: Map<GoPackage, String?>,
        maxTraceLength: Int,
    ) = listOf(
        errorMessages,
        testInputStruct,
        rawValueInterface,
        primitiveValueStruct,
        primitiveValueToReflectValueMethod,
        fieldValueStruct,
        structValueStruct,
        structValueToReflectValueMethod,
        keyValueStruct,
        mapValueStruct,
        mapValueToReflectValueMethod,
        arrayValueStruct,
        arrayValueToReflectValueMethod,
        sliceValueStruct,
        sliceValueToReflectValueMethod,
        chanValueStruct,
        chanValueToReflectValueMethod,
        nilValueStruct,
        nilValueToReflectValueMethod,
        namedValueStruct,
        namedValueToReflectValueMethod,
        pointerValueStruct,
        pointerValueToReflectValueMethod,
        convertStringToReflectType(namedTypes, destinationPackage, aliases),
        panicMessageStruct,
        rawExecutionResultStruct,
        convertReflectValueOfDefinedTypeToRawValueFunction,
        convertFloat64ValueToStringFunction,
        convertReflectValueOfPredeclaredOrNotDefinedTypeToRawValueFunction,
        convertReflectValueToRawValueFunction,
        executeFunctionFunction(maxTraceLength),
        wrapResultValuesForWorkerFunction,
        convertRawValuesToReflectValuesFunction,
        parseTestInputFunction,
        parseRawValueFunction,
        parseFieldValueFunction,
        parseKeyValueFunction
    )
}