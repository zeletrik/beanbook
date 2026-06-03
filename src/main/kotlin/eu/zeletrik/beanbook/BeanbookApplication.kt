package eu.zeletrik.beanbook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BeanbookApplication

fun main(args: Array<String>) {
	runApplication<BeanbookApplication>(*args)
}
