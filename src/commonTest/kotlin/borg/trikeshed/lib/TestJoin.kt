package borg.trikeshed.lib

import com.vsiwest.Join
import com.vsiwest.get
import com.vsiwest.j
import com.vsiwest.s_
import com.vsiwest.toArray
import com.vsiwest.toList
import com.vsiwest.α
import com.vsiwest.`▶`
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinTests {

    @Test
    fun joinFactoryMethodCreatesJoin() {
        val join = Join(1, "one")
        assertEquals(1, join.a)
        assertEquals("one", join.b)
    }

    @Test
    fun joinPairFactoryMethodCreatesJoin() {
        val pair = Pair(1, "one")
        val join = Join(pair)
        assertEquals(1, join.a)
        assertEquals("one", join.b)
    }

    @Test
    fun joinMapFactoryMethodCreatesSeries() {
        val map = mapOf(1 to "one", 2 to "two")
        val series = Join(map)
        assertEquals(2, series.a)
        assertEquals("one", series.b(0).b)
        assertEquals("two", series.b(1).b)
    }


    @Test
    fun joinListReturnsListForSeries() {
        val series = s_ [1 j "one", 2 j "two"]
        val expected = listOf(Join(1, "one"), Join(2, "two"))

        assertEquals(expected.map(Join<Int, String>::pair),(series α Join<Int, String>::pair).toList())



    }


    @Test
    fun seriesToListReturnsCorrectList() {
        val series = Join(mapOf(1 to "one", 2 to "two"))
        val list = series.toList()
        assertEquals(2, list.size)
        assertEquals("one", list[0].b)
        assertEquals("two", list[1].b)
    }

    @Test
    fun seriesToArrayReturnsCorrectArray() {
        val series = Join(mapOf(1 to 1, 2 to 2))
        val array = series.toArray()
        assertEquals(2, array.size)
        assertEquals(1, array[0].b)
        assertEquals(2, array[1].b)
    }

    enum class TestEnum { FIRST, SECOND }

    @Test
    fun seriesGetByEnumReturnsCorrectValue() {
        val series = Join(mapOf(0 to "zero", 1 to "one"))

        run{
            var (c, d) = series.get(TestEnum.FIRST)
            assertEquals("zero", d)
        }
        run{
            var (c, d) = series.get(TestEnum.FIRST)

            assertEquals("one", series.get(TestEnum.SECOND).b)
        }
    }

    @Test
    fun seriesIterableReturnsCorrectValues() {
        val series = Join(mapOf(0 to "zero", 1 to "one"))
        val iterable = series.`▶`
        val list = iterable.toList()
        assertEquals(2, list.size)
        assertEquals("zero", list[0].b)
        assertEquals("one", list[1].b)
    }
}
