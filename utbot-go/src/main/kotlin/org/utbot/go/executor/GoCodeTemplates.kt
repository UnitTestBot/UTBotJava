package org.utbot.go.executor

object GoCodeTemplates {

    private val traces = """
        var __traces__ []int
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
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(value), nil
        	case "int":
        		value, err := strconv.Atoi(v.Value)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(value), nil
        	case "int8":
        		value, err := strconv.ParseInt(v.Value, 10, 8)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(int8(value)), nil
        	case "int16":
        		value, err := strconv.ParseInt(v.Value, 10, 16)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(int16(value)), nil
        	case "int32":
        		value, err := strconv.ParseInt(v.Value, 10, 32)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(int32(value)), nil
        	case "rune":
        		value, err := strconv.ParseInt(v.Value, 10, 32)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(rune(value)), nil
        	case "int64":
        		value, err := strconv.ParseInt(v.Value, 10, 64)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(value), nil
        	case "byte":
        		value, err := strconv.ParseUint(v.Value, 10, 8)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(byte(value)), nil
        	case "uint":
        		value, err := strconv.ParseUint(v.Value, 10, strconv.IntSize)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(uint(value)), nil
        	case "uint8":
        		value, err := strconv.ParseUint(v.Value, 10, 8)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(uint8(value)), nil
        	case "uint16":
        		value, err := strconv.ParseUint(v.Value, 10, 16)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(uint16(value)), nil
        	case "uint32":
        		value, err := strconv.ParseUint(v.Value, 10, 32)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(uint32(value)), nil
        	case "uint64":
        		value, err := strconv.ParseUint(v.Value, 10, 64)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(value), nil
        	case "float32":
        		value, err := strconv.ParseFloat(v.Value, 32)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(float32(value)), nil
        	case "float64":
        		value, err := strconv.ParseFloat(v.Value, 64)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(value), nil
        	case "complex64":
        		splittedValue := strings.Split(v.Value, complexPartsDelimiter)
        		if len(splittedValue) != 2 {
        			return reflect.Value{}, fmt.Errorf("not correct complex64 value")
        		}
        		realPart, err := strconv.ParseFloat(splittedValue[0], 32)
        		__checkErrorAndExit__(err)

        		imaginaryPart, err := strconv.ParseFloat(splittedValue[1], 32)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(complex(float32(realPart), float32(imaginaryPart))), nil
        	case "complex128":
        		splittedValue := strings.Split(v.Value, complexPartsDelimiter)
        		if len(splittedValue) != 2 {
        			return reflect.Value{}, fmt.Errorf("not correct complex128 value")
        		}

        		realPart, err := strconv.ParseFloat(splittedValue[0], 64)
        		__checkErrorAndExit__(err)

        		imaginaryPart, err := strconv.ParseFloat(splittedValue[1], 64)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(complex(realPart, imaginaryPart)), nil
        	case "string":
        		return reflect.ValueOf(v.Value), nil
        	case "uintptr":
        		value, err := strconv.ParseUint(v.Value, 10, strconv.IntSize)
        		__checkErrorAndExit__(err)

        		return reflect.ValueOf(uint(value)), nil
        	}
        	return reflect.Value{}, fmt.Errorf("not supported type %s", v.Type)
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
        func (_ __StructValue__) __toReflectValue__() (reflect.Value, error) {
        	panic("not implemented")
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
        	__checkErrorAndExit__(err)

        	arrayType := reflect.ArrayOf(v.Length, elementType)
        	arrayPtr := reflect.New(arrayType)

        	for i := 0; i < v.Length; i++ {
        		element := arrayPtr.Elem().Index(i)

        		reflectValue, err := v.Value[i].__toReflectValue__()
        		__checkErrorAndExit__(err)

        		element.Set(reflectValue)
        	}

        	return arrayPtr.Elem(), nil
        }
    """.trimIndent()

    private val convertStringToReflectType = """
        func __convertStringToReflectType__(typeName string) (reflect.Type, error) {
        	var result reflect.Type

        	switch {
        	case strings.HasPrefix(typeName, "map["):
        		return nil, fmt.Errorf("map type not supported")
        	case strings.HasPrefix(typeName, "[]"):
        		return nil, fmt.Errorf("slice type not supported")
        	case strings.HasPrefix(typeName, "["):
        		index := strings.IndexRune(typeName, ']')
        		if index == -1 {
        			return nil, fmt.Errorf("not correct type name '%s'", typeName)
        		}

        		lengthStr := typeName[1:index]
        		length, err := strconv.Atoi(lengthStr)
        		if err != nil {
        			return nil, err
        		}

        		res, err := __convertStringToReflectType__(typeName[index+1:])
        		if err != nil {
        			return nil, err
        		}

        		result = reflect.ArrayOf(length, res)
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
        		default:
        			return nil, fmt.Errorf("type '%s' not supported", typeName)
        		}
        	}
        	return result, nil
        }
    """.trimIndent()

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
        	Trace           []int                `json:"trace"`
        }
    """.trimIndent()

    private val checkErrorFunction = """
        func __checkErrorAndExit__(err error) {
        	if err != nil {
        		log.Fatal(err)
        	}
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

    private val convertReflectValueToRawValueFunction = """
        //goland:noinspection GoPreferNilSlice
        func __convertReflectValueToRawValue__(valueOfRes reflect.Value) (__RawValue__, error) {
        	const outputComplexPartsDelimiter = "@"

        	switch valueOfRes.Kind() {
        	case reflect.Bool:
        		return __PrimitiveValue__{
        			Type:  valueOfRes.Kind().String(),
        			Value: fmt.Sprintf("%#v", valueOfRes.Bool()),
        		}, nil
        	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
        		return __PrimitiveValue__{
        			Type:  valueOfRes.Kind().String(),
        			Value: fmt.Sprintf("%#v", valueOfRes.Int()),
        		}, nil
        	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Uintptr:
        		return __PrimitiveValue__{
        			Type:  valueOfRes.Kind().String(),
        			Value: fmt.Sprintf("%v", valueOfRes.Uint()),
        		}, nil
        	case reflect.Float32, reflect.Float64:
        		return __PrimitiveValue__{
        			Type:  valueOfRes.Kind().String(),
        			Value: __convertFloat64ValueToString__(valueOfRes.Float()),
        		}, nil
        	case reflect.Complex64, reflect.Complex128:
        		value := valueOfRes.Complex()
        		realPartString := __convertFloat64ValueToString__(real(value))
        		imagPartString := __convertFloat64ValueToString__(imag(value))
        		return __PrimitiveValue__{
        			Type:  valueOfRes.Kind().String(),
        			Value: fmt.Sprintf("%v%v%v", realPartString, outputComplexPartsDelimiter, imagPartString),
        		}, nil
        	case reflect.String:
        		return __PrimitiveValue__{
        			Type:  reflect.String.String(),
        			Value: fmt.Sprintf("%#v", valueOfRes.String()),
        		}, nil
        	case reflect.Struct:
        		fields := reflect.VisibleFields(valueOfRes.Type())
        		resultValues := make([]__FieldValue__, len(fields))
        		for i, field := range fields {
        			res, err := __convertReflectValueToRawValue__(valueOfRes.FieldByName(field.Name))
        			__checkErrorAndExit__(err)

        			resultValues[i] = __FieldValue__{
        				Name:       field.Name,
        				Value:      res,
        				IsExported: field.IsExported(),
        			}
        		}
        		return __StructValue__{
        			Type:  valueOfRes.Type().String(),
        			Value: resultValues,
        		}, nil
        	case reflect.Array:
        		elem := valueOfRes.Type().Elem()
        		elementType := elem.String()
        		arrayElementValues := []__RawValue__{}
        		for i := 0; i < valueOfRes.Len(); i++ {
        			arrayElementValue, err := __convertReflectValueToRawValue__(valueOfRes.Index(i))
        			__checkErrorAndExit__(err)

        			arrayElementValues = append(arrayElementValues, arrayElementValue)
        		}
        		length := len(arrayElementValues)
        		return __ArrayValue__{
        			Type:        fmt.Sprintf("[%d]%s", length, elementType),
        			ElementType: elementType,
        			Length:      length,
        			Value:       arrayElementValues,
        		}, nil
        	case reflect.Interface:
        		if valueOfRes.Interface() == nil {
        			return nil, nil
        		}
        		if e, ok := valueOfRes.Interface().(error); ok {
        			return __convertReflectValueToRawValue__(reflect.ValueOf(e.Error()))
        		}
        		return nil, errors.New("unsupported result type: " + valueOfRes.Type().String())
        	default:
        		return nil, errors.New("unsupported result type: " + valueOfRes.Type().String())
        	}
        }
    """.trimIndent()

    private val executeFunctionFunction = """
        func __executeFunction__(
        	timeoutMillis time.Duration, wrappedFunction func() []__RawValue__,
        ) __RawExecutionResult__ {
        	ctxWithTimeout, cancel := context.WithTimeout(context.Background(), timeoutMillis)
        	defer cancel()

        	done := make(chan __RawExecutionResult__, 1)
        	go func() {
        		executionResult := __RawExecutionResult__{
        			TimeoutExceeded: false,
        			RawResultValues: []__RawValue__{},
        			PanicMessage:    nil,
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
        				__checkErrorAndExit__(err)

        				executionResult.PanicMessage = &__RawPanicMessage__{
        					RawResultValue:  resultValue,
        					ImplementsError: implementsError,
        				}
        			}
        			executionResult.Trace = __traces__
        			done <- executionResult
        		}()

        		resultValues := wrappedFunction()
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
        			Trace:           __traces__,
        		}
        	}
        }
    """.trimIndent()

    private val wrapResultValuesForExecutorFunction = """
            //goland:noinspection GoPreferNilSlice
            func __wrapResultValuesForUtBotGoExecutor__(values ...any) []__RawValue__ {
            	rawValues := []__RawValue__{}
            	for _, value := range values {
            		if value == nil {
            			rawValues = append(rawValues, nil)
            			continue
            		}

            		if err, ok := value.(error); ok {
            			value = err.Error()
            		}

            		resultValue, err := __convertReflectValueToRawValue__(reflect.ValueOf(value))
            		__checkErrorAndExit__(err)

            		rawValues = append(rawValues, resultValue)
            	}
            	return rawValues
            }
        """.trimIndent()

    private val wrapResultValuesForWorkerFunction = """
        //goland:noinspection GoPreferNilSlice
        func __wrapResultValuesForUtBotGoWorker__(values []reflect.Value) []__RawValue__ {
        	rawValues := []__RawValue__{}
        	for _, value := range values {
        		resultValue, err := __convertReflectValueToRawValue__(value)
        		__checkErrorAndExit__(err)

        		rawValues = append(rawValues, resultValue)
        	}
        	return rawValues
        }
    """.trimIndent()

    private val convertRawValuesToReflectValuesFunction = """
        //goland:noinspection GoPreferNilSlice
        func __convertRawValuesToReflectValues__(values []__RawValue__) []reflect.Value {
        	parameters := []reflect.Value{}

        	for _, value := range values {
        		reflectValue, err := value.__toReflectValue__()
        		__checkErrorAndExit__(err)

        		parameters = append(parameters, reflectValue)
        	}

        	return parameters
        }
    """.trimIndent()

    private val parseJsonToRawValuesFunction = """
        //goland:noinspection GoPreferNilSlice
        func __parseJsonToRawValues__(decoder *json.Decoder) ([]__RawValue__, error) {
        	result := []__RawValue__{}

        	// read '['
        	_, err := decoder.Token()
        	if err == io.EOF {
        		return nil, err
        	}
        	__checkErrorAndExit__(err)

        	for decoder.More() {
        		var p map[string]interface{}
        		err = decoder.Decode(&p)
        		__checkErrorAndExit__(err)

        		rawValue, err := __convertParsedJsonToRawValue__(p)
        		__checkErrorAndExit__(err)

        		result = append(result, rawValue)
        	}

        	// read ']'
        	_, err = decoder.Token()
        	__checkErrorAndExit__(err)

        	return result, nil
        }
    """.trimIndent()

    private val convertParsedJsonToRawValueFunction = """
        //goland:noinspection GoPreferNilSlice
        func __convertParsedJsonToRawValue__(p map[string]interface{}) (__RawValue__, error) {
        	rawValue := p

        	typeName, ok := rawValue["type"]
        	if !ok {
        		return nil, fmt.Errorf("every rawValue must contain field 'type'")
        	}
        	typeNameStr, ok := typeName.(string)
        	if !ok {
        		return nil, fmt.Errorf("field 'type' must be string")
        	}

        	v, ok := rawValue["value"]
        	if !ok {
        		return nil, fmt.Errorf("every rawValue must contain field 'value'")
        	}

        	switch {
        	case strings.HasPrefix(typeNameStr, "map["):
        		return nil, fmt.Errorf("map type not supported")
        	case strings.HasPrefix(typeNameStr, "[]"):
        		return nil, fmt.Errorf("slice type not supported")
        	case strings.HasPrefix(typeNameStr, "["):
        		elementType, ok := rawValue["elementType"]
        		if !ok {
        			return nil, fmt.Errorf("arrayValue must contain field 'elementType")
        		}
        		elementTypeStr, ok := elementType.(string)
        		if !ok {
        			return nil, fmt.Errorf("arrayValue field 'elementType' must be string")
        		}

        		if _, ok := rawValue["length"]; !ok {
        			return nil, fmt.Errorf("arrayValue must contain field 'length'")
        		}
        		length, ok := rawValue["length"].(float64)
        		if !ok {
        			return nil, fmt.Errorf("arrayValue field 'length' must be float64")
        		}

        		value, ok := v.([]interface{})
        		if !ok || len(value) != int(length) {
        			return nil, fmt.Errorf("arrayValue field 'value' must be array of length %d", int(length))
        		}

        		values := []__RawValue__{}
        		for _, v := range value {
        			nextValue, err := __convertParsedJsonToRawValue__(v.(map[string]interface{}))
        			__checkErrorAndExit__(err)

        			values = append(values, nextValue)
        		}

        		return __ArrayValue__{
        			Type:        typeNameStr,
        			ElementType: elementTypeStr,
        			Length:      int(length),
        			Value:       values,
        		}, nil
        	default:
        		switch typeNameStr {
        		case "bool", "rune", "int", "int8", "int16", "int32", "int64", "byte", "uint", "uint8", "uint16", "uint32", "uint64", "float32", "float64", "complex64", "complex128", "string", "uintptr":
        			value, ok := v.(string)
        			if !ok {
        				return nil, fmt.Errorf("primitiveValue field 'value' must be string")
        			}

        			return __PrimitiveValue__{
        				Type:  typeNameStr,
        				Value: value,
        			}, nil
        		default:
        			return nil, fmt.Errorf("struct type not supported")
        		}
        	}
        }
    """.trimIndent()

    val topLevelHelperStructsAndFunctionsForExecutor = listOf(
        traces,
        rawValueInterface,
        primitiveValueStruct,
        primitiveValueToReflectValueMethod,
        fieldValueStruct,
        structValueStruct,
        structValueToReflectValueMethod,
        arrayValueStruct,
        arrayValueToReflectValueMethod,
        convertStringToReflectType,
        panicMessageStruct,
        rawExecutionResultStruct,
        checkErrorFunction,
        convertFloat64ValueToStringFunction,
        convertReflectValueToRawValueFunction,
        executeFunctionFunction,
        wrapResultValuesForExecutorFunction
    )

    val topLevelHelperStructsAndFunctionsForWorker = listOf(
        traces,
        rawValueInterface,
        primitiveValueStruct,
        primitiveValueToReflectValueMethod,
        fieldValueStruct,
        structValueStruct,
        structValueToReflectValueMethod,
        arrayValueStruct,
        arrayValueToReflectValueMethod,
        convertStringToReflectType,
        panicMessageStruct,
        rawExecutionResultStruct,
        checkErrorFunction,
        convertFloat64ValueToStringFunction,
        convertReflectValueToRawValueFunction,
        executeFunctionFunction,
        wrapResultValuesForWorkerFunction,
        convertRawValuesToReflectValuesFunction,
        parseJsonToRawValuesFunction,
        convertParsedJsonToRawValueFunction
    )
}