package com.vsiwest.plaf


import com.vsiwest.Join
import com.vsiwest.plaf.*
import com.vsiwest.meta.IOMemento
import com.vsiwest.meta.RecordMeta
import com.vsiwest.*
import com.vsiwest.`⏵`
import kotlin.jvm.JvmOverloads

class SimpleCursor @JvmOverloads constructor(
    val scalars: Series<ColumnMeta>,
    val data: Series<Series<Any>>,
    val o: Series<RecordMeta> = scalars α {
        (it as? RecordMeta) ?: RecordMeta(it.name, (it.type as? IOMemento )?: IOMemento.IoString)
    },
    val c: Series<RowVec> = data α  {
        it.zip(o) as Join<Int, (Int) -> Join<Any?, () -> ColumnMeta>>
    } ,
) : Cursor by c