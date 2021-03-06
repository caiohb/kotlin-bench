package com.manza.kotlinbench.reactor.netty

import com.google.gson.Gson
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServer
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


fun main(args: Array<String>) {

    val users = ConcurrentHashMap<String, User>()

    val server = HttpServer.create()
            .route { routes ->
                routes.get("/users/{id}") { request, response ->
                    response
                            .addHeader("Content-Type", "application/json")
                            .sendString(
                                    Mono.just(Gson().toJson(users.getOrDefault(
                                            request.param("id"),
                                            User("not found", "not found", "not found")
                                    ))
                                    )
                            )
                }.post("/users") { request, response ->
                    response
                            .addHeader("Content-Type", "application/json")
                            .sendString(
                                    request.receive().aggregate().map { byteBuf ->
                                        Gson().fromJson<User>(
                                                byteBuf.toString(Charset.defaultCharset()), User::class.java
                                        )
                                    }.map { user ->
                                        val newUser = user.copy(id = UUID.randomUUID().toString())
                                        users.put(newUser.id!!, newUser)
                                        Gson().toJson(newUser)
                                    }
                            )

                }
            }
            .port(61543)
            .bindNow()

    server.onDispose().block()
}


data class User(
        val id: String? = "not found",
        val username: String,
        val password: String
)
