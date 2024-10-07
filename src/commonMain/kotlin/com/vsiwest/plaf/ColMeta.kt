package com.vsiwest.plaf

import com.vsiwest.Join
import com.vsiwest.meta.TypeMemento


typealias ColumnMeta = Join<String, TypeMemento>

//mix-in for name
val ColumnMeta.name: String get() = this.a

//mix-in for type
val ColumnMeta.type: TypeMemento get() = this.b
