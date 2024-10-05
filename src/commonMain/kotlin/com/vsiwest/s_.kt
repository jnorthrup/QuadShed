package com.vsiwest


/**Series macro object*/
object s_ {
    /**Series factorymethod */
    operator fun <T> get(vararg t: T):  Series<T> = t.size j t::get
}
