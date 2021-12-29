import java.util.*

// Horowitz & Sahni, 1974 (with an optimization by Korf & Schreiber, 2013)
fun solve2(t: LongArray): Schedule2 {
 /* assert(t.size <= 32)
    assert(t.all { it > 0 }) */

    // to prune permutations, the last job is assumed *not* part of the current set
    val A = generateSubsets2(t, 0, t.size / 2)
    val B = generateSubsets2(t, t.size / 2, t.size - 1)
    A.sortBy { it.second }
    B.sortByDescending { it.second }

    val sum = t.sum()
    val perfect0 =  sum      / 2  // floor
    val perfect1 = (sum + 1) / 2  // ceil

    var assignment: Assignment2? = null
    var upper = sum + 1
    var lower = -1L
    var kA = 0
    var kB = 0
    while (kA < A.size && kB < B.size) {
        val a = A[kA]
        val b = B[kB]
        val x = a.second + b.second
        if (x <= lower) {
            kA++
        } else if (x < perfect0) {
            assignment = a.first or b.first
            upper = sum - x
            lower = x
            kA++
        } else if (x <= perfect1) {
            assignment = a.first or b.first
            break
        } else if (x < upper) {
            assignment = a.first or b.first
            upper = x
            lower = sum - x
            kB++
        } else {
            kB++
        }
    }

    return Pair(assignment!!, upper)
}

// TODO: since only n ≈ 20, a simple quadratic approach may be faster
// Karmarkar & Karp, 1982  (unused)
private fun computeUpperbound2(t: LongArray, sum: Long): Long {
    val q = PriorityQueue<Long>(t.size, reverseOrder())
    for (x in t) q.add(x)
    while (q.size > 1) q += q.poll() - q.poll()
    return (q.peek() + sum) / 2
}

private fun generateSubsets2(t: LongArray, fromIndex: Int, toIndex: Int): MutableList<Schedule2> {
    val subsets = ArrayList<Schedule2>(1 shl toIndex - fromIndex)  // initial capacity: 2^jobs
    subsets += Pair(0, 0)
    for (i in fromIndex until toIndex) {
        for (sIdx in 0 until subsets.size) {  // manual iteration; otherwise ConcurrentModificationException is thrown
            val s = subsets[sIdx]
            subsets += Pair(1 shl i or s.first, t[i] + s.second)
        }
    }
    return subsets  // we do not remove subsets with equal sum since they are rare and it would be expensive
}
