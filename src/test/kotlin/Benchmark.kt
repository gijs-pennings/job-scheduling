fun Input.sorted() = Input(t.sortedDescending().run { if (m > 3) interlaced() else this }, m)

fun List<Long>.average() = sum().toDouble() / size
fun List<Long>.median(): Double {
    val ordered = sorted()
    val mid = size / 2
    return if (size % 2 == 0)
        (ordered[mid-1] + ordered[mid]) / 2.0
    else
        ordered[mid].toDouble()
}

fun main() {
    // warm-up
    repeat(10_000) { solve(Input.random(30, 3).sorted()) }
    Thread.sleep(1000)

    println("n\tm\truns\tmsAverage\tmsMedian")
    for (m in 3..5) {
        for (n in 10..50) {
            val runs = when {
                n <= 30 -> 10_000
                n <= 40 ->  1_000
                else    ->    100
            }

            // warm-up
            repeat(runs / 10) { solve(Input.random(n, m).sorted()) }
            Thread.sleep(1000)

            val inputs = List(runs) { Input.random(n, m).sorted() }
            val times = mutableListOf<Long>()
            for (i in inputs) {
                val time0 = System.nanoTime()
                solve(i)
                val time1 = System.nanoTime()
                times += time1 - time0
            }

            println(listOf(
                n, m, runs,
                "%.6f".format(times.average() / 1e6),
                "%.6f".format(times.median() / 1e6)
            ).joinToString("\t"))
            System.out.flush()
        }
    }
}
