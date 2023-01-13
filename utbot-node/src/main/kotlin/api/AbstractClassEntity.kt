package api

abstract class AbstractClassEntity(
    open val name: String,
    open val methods: List<AbstractFunctionEntity>,
) {
}