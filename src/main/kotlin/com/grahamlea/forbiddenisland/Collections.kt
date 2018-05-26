package com.grahamlea.forbiddenisland

import java.util.*

data class ImmutableList<out E>(private val inner:List<E>) : List<E> by inner {
    override fun toString() = "@" + inner.toString()
}
fun <T> immListOf(element: T): ImmutableList<T> = java.util.Collections.singletonList(element).imm()
fun <T> immListOf(vararg elements: T): ImmutableList<T> = listOf(*elements).imm()
infix operator fun <E> ImmutableList<E>.plus(e: E): ImmutableList<E> = ImmutableList(this as List<E> + e)
infix operator fun <E> ImmutableList<E>.plus(es: Collection<E>): ImmutableList<E> = ImmutableList(this as List<E> + es)
fun <E> List<E>.imm() = this as? ImmutableList<E> ?: ImmutableList(this)
fun <E> ImmutableList<E>.subtract(es: Iterable<E>): ImmutableList<E> {
    val undesired = es.toMutableList()
    return this.filter { !undesired.remove(it) }.imm()
}

data class ImmutableMap<K, out V>(private val inner: Map<K, V>) : Map<K, V> by inner {
    override fun toString() = "@" + inner.toString()
}
fun <K, V> immMapOf(pair: Pair<K, V>): ImmutableMap<K, V> = java.util.Collections.singletonMap(pair.first, pair.second).imm()
fun <K, V> immMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = mapOf(*pairs).imm()
fun <K, V> Map<K, V>.imm() = this as? ImmutableMap<K, V> ?: ImmutableMap(this)
infix operator fun <K, V> ImmutableMap<K, V>.plus(p: Pair<K, V>): ImmutableMap<K, V> = ImmutableMap(this as Map<K, V> + p)

data class ImmutableSet<E: Comparable<E>>(private val inner:Set<E>) : SortedSet<E> by inner.toSortedSet() {
    override fun toString() = "@" + inner.toString()
}
fun <T: Comparable<T>> immSetOf(element: T): ImmutableSet<T> = java.util.Collections.singleton(element).imm()
fun <T: Comparable<T>> immSetOf(vararg elements: T): ImmutableSet<T> = sortedSetOf(*elements).imm()
fun <E: Comparable<E>> Set<E>.imm() = this as? ImmutableSet<E> ?: ImmutableSet(this)

inline fun <reified T : Enum<T>> shuffled(random: Random = Random(), count: Int = enumValues<T>().size): ImmutableList<T>
        = enumValues<T>().toList().shuffled(random).take(count).imm()
