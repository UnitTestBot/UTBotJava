#!/bin/bash

declare -a projects=("antlr" "guava" "fescar" "seata" "spoon")


for i in "${projects[@]}"
do
   echo "$i"
done