package org.utbot.fuzzing.demo

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.utbot.fuzzing.*
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    withTimeout(TimeUnit.SECONDS.toMillis(10)) {
        object : Fuzzing<String, String, Description<String, String>, Feedback<String, String>> {
            override fun generate(description: Description<String, String>, type: String) = sequence<Seed<String, String>> {
                when (type) {
                    "url" -> yield(Seed.Recursive(
                        construct = Routine.Create(
                            listOf("protocol", "host", "port", "path")
                        ) {
                            val (protocol, host, port, path) = it
                            "$protocol://$host${if (port.isNotBlank()) ":$port" else ""}/$path"
                        },
                        empty = Routine.Empty { error("error") }
                    ))
                    "protocol" -> {
                        yield(Seed.Simple("http"))
                        yield(Seed.Simple("https"))
                        yield(Seed.Simple("ftp"))
                    }
                    "host" -> {
                        yield(Seed.Simple("localhost"))
                        yield(Seed.Simple("127.0.0.1"))
                    }
                    "port" -> {
                        yield(Seed.Simple("8080"))
                        yield(Seed.Simple(""))
                    }
                    "path" -> yield(Seed.Recursive(
                        construct = Routine.Create(listOf("page", "id")) {
                            it.joinToString("/")
                        },
                        empty = Routine.Empty { error("error") }
                    ))
                    "page" -> {
                        yield(Seed.Simple("owners"))
                        yield(Seed.Simple("users"))
                        yield(Seed.Simple("admins"))
                    }
                    "id" -> (0..1000).forEach { yield(Seed.Simple(it.toString())) }
                    else -> error("unknown type '$type'")
                }
            }

            override suspend fun handle(
                description: Description<String, String>,
                values: List<String>
            ): Feedback<String, String> {
                println(values[0])
                return BaseFeedback(values[0], Control.PASS)
            }
        }.fuzz(Description(listOf("url")))
    }
}