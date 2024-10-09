package com.vsiwest.dsel

import com.vsiwest.Series
import com.vsiwest.Twin

// Narsese grammar constructs using backticks for special glyphs
sealed interface `⟨sentence⟩`
data class `⟨judgment⟩`(
    val tense: `⟨tense⟩`?,
    val statement: `⟨statement⟩`,
    val truthValue: `⟨truth-value⟩`,
) : `⟨sentence⟩`

data class `⟨goal⟩`(
    val statement: `⟨statement⟩`,
    val desireValue: `⟨desire-value⟩`,
) : `⟨sentence⟩`

data class `⟨question⟩`(
    val content: `⟨question-content⟩`,
) : `⟨sentence⟩`

sealed interface `⟨question-content⟩` : `⟨term⟩`

// Statement representation
data class `⟨statement⟩`(
    val subject: `⟨term⟩`,
    val copula: `⟨copula⟩`,
    val predicate: `⟨term⟩`,
) : `⟨term⟩`, `⟨question-content⟩`

// Term representation
sealed interface `⟨term⟩`
data class `⟨word⟩`(val value: String) : `⟨term⟩`
sealed interface `⟨variable⟩` : `⟨term⟩`
data class `⟨independent-variable⟩`(val name: String) : `⟨variable⟩`
data class `⟨dependent-variable⟩`(
    val name: String,
    val dependencies: List<`⟨independent-variable⟩`>,
) : `⟨variable⟩`

data class `⟨query-variable⟩`(val name: String) : `⟨variable⟩`

// Copula with special glyphs
enum class `⟨copula⟩` {
    `→`,
    `↔`,
    `∘→`,
    `→∘`,
    `∘→∘`,
    `⇒`,
    `⇔`,
    `2⇒`,
    `00⇒`  ,
    `1⇒`,
    `2⇔`,
    `1⇔`;
}

// Tense with special glyphs
enum class `⟨tense⟩` {
    `2⇒`,
    `00⇒`,
    `1⇒`;
}
// Truth and Desire values using Twin typealias
typealias `⟨truth-value⟩` = Twin<Double> // <frequency, confidence>
typealias `⟨desire-value⟩` = Twin<Double> // <desire, confidence>

// Compound terms
data class `⟨extension⟩`(val terms: Series<`⟨term⟩`>) : `⟨term⟩`
data class `⟨intension⟩`(val terms: Series<`⟨term⟩`>) : `⟨term⟩`
data class `⟨intersection⟩`(val terms: Series<`⟨term⟩`>) : `⟨term⟩`
data class `⟨union⟩`(val terms: Series<`⟨term⟩`>) : `⟨term⟩`
data class `⟨difference⟩`(
    val minuend: `⟨term⟩`,
    val subtrahend: `⟨term⟩`,
) : `⟨term⟩`

data class `⟨product⟩`(val terms: Series<`⟨term⟩`>) : `⟨term⟩`