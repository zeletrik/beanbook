package eu.zeletrik.beanbook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/** Spring Boot entry point for the Bean Book application. */
@SpringBootApplication
class BeanbookApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
	runApplication<BeanbookApplication>(*args)
}
