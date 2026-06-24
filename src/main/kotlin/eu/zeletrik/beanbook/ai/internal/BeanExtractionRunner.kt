package eu.zeletrik.beanbook.ai.internal

import ai.koog.prompt.Prompt
import eu.zeletrik.beanbook.ai.BeanExtraction

/**
 * Narrow seam over the single LLM call: runs a fully built [Prompt] and returns the parsed
 * [BeanExtraction], or `null` if the model produced nothing usable.
 *
 * Production wiring (`AiConfiguration`) backs this with Koog's `executeStructured`; tests supply a
 * canned implementation so [eu.zeletrik.beanbook.ai.AiExtractionService] is exercised without a real
 * API call. Keeping the executor/model types behind this interface keeps the public service
 * provider-agnostic.
 */
fun interface BeanExtractionRunner {
    suspend fun run(prompt: Prompt): BeanExtraction?
}
