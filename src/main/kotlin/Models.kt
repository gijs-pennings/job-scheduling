import java.io.File
import java.math.MathContext
import kotlin.random.Random

class Input(val t: List<Long>, val m: Int) {

    val n: Int get() = t.size

    init {
        // only consider non-trivial inputs
        assert(t.all { it > 0 })
        assert(m in 2 until n)
    }

    private fun calculateGap(makespan: Long): Double {
        val lowerbound = t.sum().toDouble() / m
        return (makespan - lowerbound) / lowerbound * 100  // percentage
    }

    private fun calculateMakespan(assignment: Assignment) =
        assignment.withIndex().groupBy { it.value }.maxOf { (_, jobs) -> jobs.sumOf { t[it.index] } }

    fun printSummary(s: Schedule): String {
        val b = StringBuilder()

        // i. makespan
        b.append("makespan = ").append(s.second)
        val delta = calculateMakespan(s.first) - s.second
        if (delta == 0L)
            b.append(" (valid)\n")
        else
            b.append(" (invalid! Δ = ").append(delta).append(")\n")

        // ii. gap
        b.append("gap = ").append(calculateGap(s.second).toBigDecimal(MathContext(3)).toPlainString()).append("%\n")

        // iii. assignment
        val machines = MutableList(m) { mutableListOf<Int>() }
        s.first.forEachIndexed { i, j -> machines[j] += i+1 }
        machines.forEach { it.sort() }
        machines.sortBy { it[0] }
        machines.forEachIndexed { j, jobs -> b.append(j+1).append(": [").append(jobs.joinToString()).append("]\n") }
        b.setLength(b.length - 1)  // remove newline

        val summary = b.toString()
        println(summary)
        return summary
    }

    fun randomAssignment(r: Random = Random.Default): Assignment = IntArray(n) { r.nextInt(m) }

}

fun Input(path: String): Input {
    val lines = File(path).readLines()
    return Input(lines.drop(1).map { it.toLong() }, lines[0].toInt())
}

typealias Assignment2 = Int  // bit set
typealias Schedule2 = Pair<Assignment2, Long>

typealias Assignment = IntArray
typealias Schedule = Pair<Assignment, Long>

operator fun Schedule.compareTo(other: Schedule?) = if (other == null) -1 else second.compareTo(other.second)
