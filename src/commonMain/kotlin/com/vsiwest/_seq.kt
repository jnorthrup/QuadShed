package com.vsiwest

object _seq {
      operator fun <T> get(vararg t: T): kotlin.sequences.Sequence<T> = kotlin.sequences.sequence {
          for (t: T in t) {
              yield(t)
          }
      }
}