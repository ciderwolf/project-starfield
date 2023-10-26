package starfield
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.*

@Serializable
sealed class Project

@Serializable
@SerialName("owned")
data class OwnedProject(val name: String, val owner: String) : Project()

inline fun doSend(obj: Project) {
    println(Json.encodeToString(obj))
}

sealed class TypescriptType {
    data class EnumType(val name: String, val names: List<String>) : TypescriptType() {
        override fun write(): String {
            val names = this.names.joinToString(" | ") { "'$it'" }
            return "type $name = $names;"
        }

        override val repr: String
            get() = name
    }

    data class CompositeType(val name: String, val props: Map<String, TypescriptType>) : TypescriptType() {
        override fun write(): String {
            val props = this.props.map {
                "${it.key}: ${it.value.repr};"
            }
            return "type $name = {\n  " +
                    props.joinToString("\n  ") +
                "\n}"
        }

        override val repr: String
            get() = name
    }

    data class BaseType(val name: String) : TypescriptType() {
        override fun write(): String? {
            return null
        }

        override val repr: String
            get() = name
    }
    
    data class ListType(val arg: TypescriptType) : TypescriptType() {
        override fun write(): String? {
            return null
        }

        override val repr: String
            get() = arg.repr + "[]"
    }

    data class MapType(val key: TypescriptType, val value: TypescriptType) : TypescriptType() {
        override fun write(): String? {
            return null
        }

        override val repr: String
            get() = "{ [index: ${key.repr}]: ${value.repr} }"
    }

    abstract fun write(): String?
    abstract val repr: String
}
val baseTypes = mapOf(
    Int::class to "number",
    Double::class to "number",
    String::class to "string",
    Boolean::class to "boolean",
    UUID::class to "string",
)

val types: MutableList<TypescriptType> = baseTypes.values.map { TypescriptType.BaseType(it) }.toMutableList()
fun main() {


//    val types: MutableList<TypescriptType> = baseTypes.values.map { TypescriptType.BaseType(it) }.toMutableList()
//    val queue = mutableListOf<KClass<*>>(ServerMessage::class)
//    while(queue.size > 0) {
//        val current = queue.removeAt(0)
//
//    }

    determineType(ServerMessage::class)

    for(t in types) {
        val source = t.write()
        if (source != null) {
            println(source + "\n")
        }

    }
}

fun determineType(current: KClass<*>, isFromSealed: Boolean = false): TypescriptType {
    if (current.isSealed) {
        // add all base classes
//        queue.addAll(current.sealedSubclasses)
        types.addAll(current.sealedSubclasses.map { determineType(it, true) })
        return TypescriptType.BaseType(current.qualifiedName!!)
    }
    else if (current.java.isEnum) {
        // enum -- write all names
        val names = current.java.enumConstants.map { it.toString() }
        types.add(TypescriptType.EnumType(current.simpleName!!, names))
        return TypescriptType.EnumType(current.simpleName!!, names)
    }
    else if (current in baseTypes) {
        return TypescriptType.BaseType(baseTypes[current]!!)
    }
    else {
        // some class -- add all properties
        val props = mutableMapOf<String, TypescriptType>()
        for(it in current.memberProperties) {
            val cls = it.returnType.classifier as? KClass<*> ?: continue
            when (cls.simpleName) {
                "List" -> {
                    val generic = it.returnType.arguments[0].type!!.classifier as KClass<*>
                    val genericDef = determineType(generic)
                    props[it.name] = TypescriptType.ListType(genericDef)
                }
                "Map" -> {
                    val key = it.returnType.arguments[0].type!!.classifier as KClass<*>
                    val value = it.returnType.arguments[1].type!!.classifier as KClass<*>
//                    types.add(TypescriptType.BaseType("{ [index: ${key.simpleName}]: ${value.simpleName} }"))
//                    queue.add(value)

                    props[it.name] = TypescriptType.MapType(determineType(key), determineType(value))
                }
                else -> {
//                    queue.add(cls)
                    if (isFromSealed && it.name == "type") {
//                        val default = getTypeName(current)
                        props[it.name] = TypescriptType.BaseType("\"${current.simpleName!!}\"")
                    } else {
                        props[it.name] = determineType(cls)
                    }
                }
            }
        }
        types.add(TypescriptType.CompositeType(current.simpleName!!, props))
        return TypescriptType.CompositeType(current.simpleName!!, props)
    }
}

fun getTypeName(cls: KClass<*>): String {
    cls.primaryConstructor!!.parameters.forEach {
        val paramClass = (it.type.classifier!! as KClass<*>)
        if (paramClass in baseTypes) {

        }
    }
    return "foo"
}