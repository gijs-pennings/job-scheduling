fun main() {
    val input = Input("challenge.txt")
    val rule = "-".repeat(40) + "\n\n"

    println("SNPEHS on 2-machine problem\n")
    with (Input(input.t.take(20), 2)) {
        printSummary(solve2(t.toLongArray()))
    }

    println(rule + "SNPESS on 4-machine problem\n")
    with (Input(input.t.drop(20).take(40), 4)) {
        printSummary(solve(this))
    }

    println(rule + "pairwise optimization (on challenge problem)\n")
    input.printSummary(optimizePairwise(input))

    println(rule + "quadruplewise optimization (on challenge problem)\n")
    input.printSummary(optimizeTuplewise(input))

    // warning: will take several hours!
 /* println(rule + "sextuplewise optimization with restarts (on challenge problem)\n")
    input.printSummary(optimizeTuplewise(input, 6, 100)) */
}
