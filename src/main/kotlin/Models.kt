import java.io.File
import java.math.MathContext
import kotlin.math.max
import kotlin.random.Random

class Machine : Comparable<Machine> {
    var jobs = mutableListOf<Int>()
    var time = 0L
    override fun compareTo(other: Machine) = time.compareTo(other.time)
}

fun Array<Machine>.toSchedule(input: Input): Schedule {
    val assignment = IntArray(input.n)
    for (j in indices)
        for (i in this[j].jobs)
            assignment[i] = j
    return Pair(assignment, last().time)
}

class Input(val t: List<Long>, val m: Int) {

    val n = t.size
    val lowerbound = (t.sum() + m-1) / m

    init {
        // only consider non-trivial inputs
        assert(t.all { it > 0 })
        assert(m in 2 until n)
    }

    private fun calculateMakespan(assignment: Assignment) =
        assignment.withIndex().groupBy { it.value }.maxOf { (_, jobs) -> jobs.sumOf { t[it.index] } }

    fun getRandomAssignment(r: Random): Assignment = IntArray(n) { r.nextInt(m) }
    fun getSimpleAssignment(): Assignment = IntArray(n) { it % m }

    fun printSummary(s: Schedule): String {
        val b = StringBuilder()

        // i. makespan
        b.append("makespan = ", s.second)
        val delta = calculateMakespan(s.first) - s.second
        if (delta == 0L)
            b.append(" (valid)\n")
        else
            b.append(" (invalid! Δ = ", delta, ")\n")

        // ii. overtime
        val overtime = s.second - lowerbound
        val gapPercentage = (100.0 * overtime / lowerbound).toBigDecimal(MathContext(3))
        b.append("overtime = ", overtime, " (", gapPercentage.toPlainString(), "%)\n\n")

        // iii. assignment
        val machines = s.first.toMachines(this)
        machines.forEach { it.jobs.sort() }
        machines.sortBy { it.jobs[0] }
        machines.sortBy { it.time }  // stable

        val machinesOvertime = machines.map { it.time - lowerbound }
        val width = max(8, machinesOvertime.maxOf { it.toString().length })

        b.append("machine  ").appendPadded("overtime", width).append("  jobs\n")
        machines.forEachIndexed { i, m ->
            b.appendPadded(i+1, 7)
            b.append("  ").appendPadded(machinesOvertime[i], width)
            b.append("  [", m.jobs.map { it + 1 }.joinToString(), "]\n")
        }

        val summary = b.toString()
        println(summary)
        return summary
    }

    @JvmName("printSummary4")
    fun printSummary(s: Schedule4) = printSummary(Pair(IntArray(n) { (s.first shr it).toInt() and 1 }, s.second))

    @JvmName("printSummary2")
    fun printSummary(s: Schedule2) = printSummary(Pair(s.first.toLong(), s.second))

}

fun Input(path: String): Input {
    val lines = File(path).readLines()
    return Input(lines.drop(1).map { it.toLong() }, lines[0].toInt())
}

typealias Assignment2 = Int  // bit set, for at most 32 jobs (sufficient for 2 machines)
typealias Schedule2 = Pair<Assignment2, Long>

typealias Assignment4 = Long  // bit set, for at most 64 jobs (sufficient for 4-5 machines)
typealias Schedule4 = Pair<Assignment4, Long>

typealias Assignment = IntArray
typealias Schedule = Pair<Assignment, Long>

fun Assignment.toMachines(input: Input): Array<Machine> {
    val machines = Array(input.m) { Machine() }
    for (i in 0 until input.n) machines[this[i]].jobs += i
    for (m in machines) m.time = m.jobs.sumOf { input.t[it] }
    machines.sort()
    return machines
}

operator fun Schedule.compareTo(other: Schedule?) = if (other == null) -1 else second.compareTo(other.second)


/* * * * * * *\
 *   Utils   *
\* * * * * * */

fun <T> StringBuilder.appendPadded(x: T, length: Int, padChar: Char = ' '): StringBuilder {
    val s = if (x is CharSequence) x else x.toString()
    for (i in 1..max(length - s.length, 0)) append(padChar)
    append(s)
    return this
}

fun <T> List<T>.interlaced(): List<T> {
    val m = (size + 1) / 2
    return List(size) {
        if (it < m)
            this[2*it]
        else
            this[2*(it-m) + 1]
    }
}

fun LongArray.isSortedDescending() = asSequence().zipWithNext { a, b -> a >= b }.all { it }
