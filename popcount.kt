fun main(args: Array<String>) {
    when {
        args.size == 1 && args[0] == "test" -> test()
        args.size == 1 && args[0] == "time" -> time()
        else -> println("Usage: popcount [test|time]")
    }
}

fun time() {
    timeFoo(::popcount_Naive, ::popcount_Naive.name)
    timeFoo(::popcount_NaiveOptimised, ::popcount_NaiveOptimised.name)
    timeFoo(::popcount_HammingWeight, ::popcount_HammingWeight.name)
    timeFoo(::popcount_HammingWeightOptimised, ::popcount_HammingWeightOptimised.name)
    timeFoo(::popcount_SWAR, ::popcount_SWAR.name)
    timeFoo(::popcount_JavaLangIntegerBitCount, ::popcount_JavaLangIntegerBitCount.name)
    timeFoo(::popcount_String, ::popcount_String.name)
}

fun timeFoo(foo: (Int) -> Int, name: String) {
    val TEST_SIZE = 100_000_000

    print("${String.format("%-34s", (name + "()"))}  ")

    val start = java.lang.System.nanoTime()

    repeat(TEST_SIZE / 100) {
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
        foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it); foo(it)
    }

    val elapsed = System.nanoTime() - start

    val t = elapsed.toDouble() / TEST_SIZE
    println("${String.format("%,.1f", t)} ns")
}

fun test() {
    println("Trying the various algorithms on the ints MIN_VALUE to MAX_VALUE...")

    val STEP = 1            // 1=thorough & slow, 100=indicative & quick

    // potentially special case and don't want to skip it if the value of STEP
    // means the loop terminates before hitting it
    testN(Int.MAX_VALUE)

    var prev = -1

    for (i in Int.MIN_VALUE..Int.MAX_VALUE step STEP) {
        testN(i)

        val percent = Math.round(100 *
                (i.toDouble() - Int.MIN_VALUE) /
                (Int.MIN_VALUE * -2.0)
            ).toInt()

        if (prev != percent) {
            println("popcount(${i.toUnsignedString()})=${String.format("%-2d", popcount_String(i))}  ${percent}%")
            prev = percent
        }
    }
}

fun testN(n: Int) {
    val a = popcount_Naive(n)
    val b = popcount_NaiveOptimised(n)
    val c = popcount_HammingWeight(n)
    val d = popcount_HammingWeightOptimised(n)
    val e = popcount_JavaLangIntegerBitCount(n)
    val f = popcount_SWAR(n)

    check(a == b && b == c && c == d && d == e && e == f) { n }
}

fun popcount_HammingWeight(_n: Int): Int {
    // each bit in n is a one-bit integer that indicates
    // how many bits are set in that bit
    var n = _n

    n = ((n and 0xAAAAAAAA.toInt()) ushr 1) + (n and 0x55555555.toInt())
    // Now every two bits are a two bit integer that
    // indicate how many bits were set in those two
    // bits in the original number

    n = ((n and 0xCCCCCCCC.toInt()) ushr 2) + (n and 0x33333333.toInt())
    // Now we're at 4 bits

    n = ((n and 0xF0F0F0F0.toInt()) ushr 4) + (n and 0x0F0F0F0F.toInt())
    // 8 bits

    n = ((n and 0xFF00FF00.toInt()) ushr 8) + (n and 0x00FF00FF.toInt())
    // 16 bits

    n = ((n and 0xFFFF0000.toInt()) ushr 16) + (n and 0x0000FFFF.toInt())
    // kaboom - 32 bits

    return n
}

fun popcount_HammingWeightOptimised(_n: Int): Int {
    var n = _n

    /*  Each bit in n is a 1-bit integer that indicates how many bits are
        set in that bit. It's an oddity of binary arithmetic that if you
        take a 2-bit number and subtract 1 from it if the high-bit is set
        then you get the count of set bits in that 2-bit number. So do that
        to all 2-bit pairings. Now the 32-bit n is 16 2-bit integer counts. */
    n = n - ((n and 0xAAAAAAAA.toInt()) ushr 1)

    /*  Take the even-numbered 2-bit ints and shift them 2 bits to the
        right before adding them to the odd-numbered 2-bit counts. Now
        you have 8 4-bit counts */
    n = ((n ushr 2) and 0x33333333.toInt()) + (n and 0x33333333.toInt())

    /*  This next part is quite cunning. Imagine the 8 4-bit nibbles were
        named A-H. What we want to do is reduce this to 4 8-bit counts by
        adding A+B, C+D, E+F, G+H. One way we could do that is the same
        as the line above, where we could mask the even-numbered nibbles
        (A,C,E,G), right-shift them 4 bits and add them to the odd-numbered
        nibbles (B,D,F,H). That would work but there's a quicker way:

        Right-shift n, ABCDEFGH, by 4 bits, giving 0ABCDEFG, and add them.
        That sum is equal to (0+A) + (A+B) + (B+C) + (C+D) + (D+E) + (E+F)
        + (F+G) + (G+H). Notice that every nibble appears twice. What we
        want is the sum of A + B + C + D + E + F + G + H, so we could just
        mask out every second term and that's what we get: (A+B) + (C+D) +
        (E+F) + (G+H). */
    n = (n + (n ushr 4)) and 0x0f0f0f0f.toInt()

    /*  The same trick works for 4x8-bits → 2x16-bits. Except we don't
        need to mask the resulting sum with 0x00ff00ff as you might think.
        If we weren't to apply that mask then there's going to be garbage
        in these bits: 0xff00ff00. Those two 16-bit quantities will be
        added in the next step but the garbage won't affect the lowest
        two hex digits. Given that the maximum count of set bits for a
        32-bit int can't be greater than 32, we can just mask the final
        result in the last step with 0xff and discard any garbage. Same
        applies when adding the two 16-bit counts; no mask needed. */
    n = n + (n ushr 8)
    n = n + (n ushr 16)

    return n and 0xff
}

fun popcount_SWAR(_n: Int): Int {
    var n = _n - ((_n ushr 1) and 0x55555555.toInt())
    n = (n and 0x33333333.toInt()) + ((n ushr 2) and 0x33333333.toInt());
    return (((n + (n ushr 4)) and 0x0F0F0F0F.toInt()) * 0x01010101.toInt()) ushr 24;
}

fun popcount_JavaLangIntegerBitCount(n: Int): Int = java.lang.Integer.bitCount(n)

fun popcount_Naive(_n: Int): Int {
    var n = _n
    var count = 0
    while (n != 0) {
        if (n and 0x1 != 0) ++count
        n = n ushr 1
    }
    return count
}

fun popcount_NaiveOptimised(_n: Int): Int {
    var n = _n
    var count = 0

    while (n != 0) {
        when (n and 0xf) {
            0b0001 -> count += 1
            0b0010 -> count += 1
            0b0011 -> count += 2
            0b0100 -> count += 1
            0b0101 -> count += 2
            0b0110 -> count += 2
            0b0111 -> count += 3
            0b1000 -> count += 1
            0b1001 -> count += 2
            0b1010 -> count += 2
            0b1011 -> count += 3
            0b1100 -> count += 2
            0b1101 -> count += 3
            0b1110 -> count += 3
            0b1111 -> count += 4
        }
        n = n ushr 4
    }

    return count
}

fun popcount_String(n: Int): Int {
    return when {
        n >= 0 -> n.toString(2).count() { it == '1' }
        else -> n.toUnsignedString().count() { it == '1' }
    }
}

fun Int.toUnsignedString(): String {
    if (this >= 0) return toString(2).padStart(32, '0')

    // Kotlin doesn't have unsigned ints so 0xffffffff converts to -1,
    // 0xfffffffe to -10, 0xfffffffd to -11, etc

    val n = this + 1
    var s = n.toString(2)
    if (s[0] == '-') s = s.substring(1)
    s = s.padStart(32, '0')
    s = s.replace('1', '*').replace('0', '1').replace('*', '0')

    return s
}
