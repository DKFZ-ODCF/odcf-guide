package de.dkfz.odcf.guide

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class StartWebApplication

fun main(args: Array<String>) {
    runApplication<StartWebApplication>(*args)
}
