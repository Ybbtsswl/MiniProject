package com.example.miniproject.receipt

import java.time.DateTimeException
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

// ==========================================
// RESULT MODEL
// ==========================================
data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double
) {
    val lineTotal: Double get() = unitPrice * quantity
}

data class ReceiptData(
    val merchant: String?,
    val date: String?,
    val items: List<ReceiptItem>,
    val subtotal: Double?,
    val sstAmount: Double?,
    val serviceChargeAmount: Double?,
    val total: Double?,
    // Rates are derived from the printed AMOUNTS (tax / subtotal) so that the app's total
    // matches the receipt even when SST was charged on subtotal + service charge.
    val sstPercent: Double?,
    val serviceChargePercent: Double?,
    val warnings: List<String>,           // things the user should double-check
    val rawText: String = ""
) {
    val hasAnything: Boolean get() = items.isNotEmpty() || total != null
}

// ==========================================
// PARSER: OCR rows of text -> ReceiptData
// Pure Kotlin (no Android), so it can be unit tested on the JVM.
// ==========================================
object ReceiptParser {

    private val RM = Regex("""\b(?:RM|MYR)\s*(?=\d)""", RegexOption.IGNORE_CASE)
    // OCR often reads the decimal point as a comma: "12,50" -> "12.50" (but never "1,299")
    private val COMMA_DECIMAL = Regex("""(?<=\d),(?=\d{2}(?!\d))""")
    // ... or leaves a gap around the dot: "19. 00" -> "19.00"
    private val SPACED_DECIMAL = Regex("""(?<=\d)\s*\.\s*(?=\d{2}(?!\d))""")
    // OCR reads the "$" currency sign as "S" ("1x Pants S59.00"), so it ends up in the item name
    private val CURRENCY_TAIL = Regex("""\s+[S$]$""")
    // SPACED_CENTS
    private val SPACED_CENTS = Regex("""(?<![A-Za-z])([S$]\s?\d{1,6})\s(\d{2})$""")

    // PRICE_TOKEN
    private val PRICE_TOKEN = Regex("""(?<![A-Za-z0-9])[S$]?\s?([0-9OSIliB]{1,7})([.,])([0-9OSIliB]{2})(?![A-Za-z0-9])""")
    // 12.50 / 1,299.00 (not 6.00% and not part of a longer number)
    private val AMT = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{3})+|\d+)\.(\d{2})(?!\d)(?!\s*%)""")
    private val PCT = Regex("""(\d{1,2}(?:\.\d+)?)\s*%""")

    private val SUBTOTAL = Regex("""sub\s*-?\s*total|jumlah\s*kecil|excl""", RegexOption.IGNORE_CASE)
    private val INCL_TOTAL = Regex("""incl|inclusive""", RegexOption.IGNORE_CASE)
    private val SST = Regex("""service\s*tax|\b(?:sst|gst|vat|tax|cukai)\b""", RegexOption.IGNORE_CASE)
    private val SERVICE = Regex("""service\s*(?:charge|chg)|\bsvc\b|\bs/c\b""", RegexOption.IGNORE_CASE)
    private val ROUNDING = Regex("""round""", RegexOption.IGNORE_CASE)
    private val NEG_END = Regex("""(?:-\s?\d+\.\d{2}|\(\d+\.\d{2}\))\s*$""")
    private val DISCOUNT = Regex("""discount|\bdisc\b|promo|voucher|saving""", RegexOption.IGNORE_CASE)
    private val COUNT = Regex("""total\s*(?:qty|quantity|items?|pax|point)""", RegexOption.IGNORE_CASE)
    private val TOTAL = Regex(
        """grand\s*total|net\s*total|\btotal\b|amount\s*(?:due|payable)|balance\s*due|jumlah""",
        RegexOption.IGNORE_CASE
    )
    private val STRONG_TOTAL = Regex("""grand|net|payable|due""", RegexOption.IGNORE_CASE)
    private val PAYMENT = Regex(
        """\b(?:cash|change|visa|master\s*card|mastercard|amex|debit|credit|card|tender|tendered|paid|payment|e-?wallet|tng|grabpay|duitnow|qr)\b""",
        RegexOption.IGNORE_CASE
    )
    private val HEADER = Regex(
        """\b(?:tel|phone|fax|invoice|receipt|inv|bill\s*no|table|date|time|cashier|server|reg|order|pax|guest|check|doc)\b""",
        RegexOption.IGNORE_CASE
    )

    // ----- merchant name detection -----
    // Lines that can never be the shop name
    private val MERCHANT_HARD_SKIP = Regex(
        """receipt|invoice|welcome|thank|\btel\b|phone|\bfax\b|www|http|@|\b(?:reg|roc|ssm|tin)\b|co\.?\s*reg|company\s*no|(?:gst|sst)\s*(?:id|no|reg)|\(\s*[\w-]+\s*\)|[&=?]|\S{25,}""",
        RegexOption.IGNORE_CASE
    )
    // Words that make a line look like an address
    private val ADDRESS_WORDS = Regex(
        """\b(?:street|avenue|road|boulevard|blvd|jalan|jln|lorong|lrg|taman|tmn|bandar|seksyen|section|persiaran|lebuh|kampung|kg|bukit|lot|level|tingkat|blok|block|unit|ground\s*floor|kuala\s*lumpur|selangor|penang|pulau\s*pinang|johor|perak|sabah|sarawak|melaka|malacca|negeri\s*sembilan|kedah|kelantan|terengganu|pahang|perlis|putrajaya|labuan|malaysia)\b|\bno\.?\s*\d|\d{5}""",
        RegexOption.IGNORE_CASE
    )
    // Words that make a line look like a business name
    private val COMPANY_WORDS = Regex(
        """\b(?:sdn\.?\s*bhd|bhd|enterprise|enterprises|restoran|restaurant|cafe|kedai|mart|mini\s*market|minimarket|supermarket|store|stores|bakery|kopitiam|trading|resources|pharmacy|farmasi|mamak|bistro|coffee|food|foods|kitchen|catering|gerai|warung|bbq|steamboat|tea|bubble)\b|café""",
        RegexOption.IGNORE_CASE
    )

    private val NAME_NOISE = Regex("""^[\W_]+|[\W_]+$""")
    // "1x Tshirt" / "2 Teh Tarik": group 2 is the optional "x"
    private val LEAD_QTY = Regex("""^(\d{1,2})\s*([xX*])?\s+(?=[A-Za-z])""")
    private val TRAIL_X = Regex("""\s[xX]\s*(\d{1,2})$""")
    private val TRAIL_NUM = Regex("""\s(\d{1,2})$""")
    private val ITEM_CODE = Regex("""^\d{4,}\s+""")
    private val TWO_LETTERS = Regex("""[A-Za-z]{2}""")
    private val ANY_LETTER = Regex("""[A-Za-z]""")

    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    private enum class Kind { COUNT, SUBTOTAL, TOTAL, SST, SERVICE, ROUNDING, DISCOUNT, PAYMENT, OTHER }

    private val SUMMARY_KINDS = setOf(Kind.SUBTOTAL, Kind.SST, Kind.SERVICE, Kind.TOTAL, Kind.PAYMENT)

    private data class Amt(val range: IntRange, val value: Double)

    // ---------- helpers ----------
    private fun digitsOf(s: String): String = s.map {
        when (it) {
            'O' -> '0'
            'S' -> '5'
            'I', 'l', 'i' -> '1'
            'B' -> '8'
            else -> it
        }
    }.joinToString("")

    // Rewrites every price-looking token as plain digits and drops its currency sign
    private fun fixPrices(line: String): String = PRICE_TOKEN.replace(line) { m ->
        val whole = m.groupValues[1]
        // needs at least one real digit, otherwise it is a word ("Bill.50")
        if (whole.none { it.isDigit() }) m.value
        else digitsOf(whole) + "." + digitsOf(m.groupValues[3])
    }

    private fun norm(line: String): String =
        fixPrices(
            RM.replace(line, "")
                .replace(COMMA_DECIMAL, ".")
                .replace(SPACED_DECIMAL, ".")
                .replace(SPACED_CENTS, "\$1.\$2")
        ).replace(Regex("""\s+"""), " ").trim()

    private fun kindOf(line: String): Kind = when {
        COUNT.containsMatchIn(line) -> Kind.COUNT
        SUBTOTAL.containsMatchIn(line) -> Kind.SUBTOTAL
        INCL_TOTAL.containsMatchIn(line) && TOTAL.containsMatchIn(line) -> Kind.TOTAL
        SST.containsMatchIn(line) -> Kind.SST
        SERVICE.containsMatchIn(line) -> Kind.SERVICE
        ROUNDING.containsMatchIn(line) -> Kind.ROUNDING
        NEG_END.containsMatchIn(line) || DISCOUNT.containsMatchIn(line) -> Kind.DISCOUNT
        TOTAL.containsMatchIn(line) -> Kind.TOTAL
        PAYMENT.containsMatchIn(line) -> Kind.PAYMENT
        else -> Kind.OTHER
    }

    private fun amounts(line: String): List<Amt> =
        AMT.findAll(line).map {
            Amt(it.range, (it.groupValues[1].replace(",", "") + "." + it.groupValues[2]).toDouble())
        }.toList()

    private fun lastAmount(line: String): Double? = amounts(line).lastOrNull()?.value

    private fun titleCase(s: String): String {
        if (s.none { it.isLetter() } || s != s.uppercase(Locale.ROOT)) return s
        return s.split(" ").joinToString(" ") { w ->
            if (w.any { it.isDigit() }) w
            else w.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }

    // ---------- date (Malaysian receipts are day-first) ----------
    private val DATE_YMD = Regex("""(?<!\d)(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})(?!\d)""")
    private val DATE_DMY = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})(?!\d)""")
    private val DATE_DMY2 = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})(?!\d)""")
    private val DATE_DMON = Regex(
        """(?<!\d)(\d{1,2})\s*[-/ ]?\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?,?\s*[-/ ]?\s*(\d{4}|\d{2})(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    // 20/11/2026 is day/month; 03/18/2026 can only be month/day (there is no month 18)
    private fun dayFirstOrMonthFirst(year: Int, first: Int, second: Int): LocalDate =
        if (second in 1..12) LocalDate.of(year, second, first) else LocalDate.of(year, first, second)

    private fun parseDate(lines: List<String>, today: LocalDate): String? {
        for (line in lines) {
            val candidates = listOf<() -> LocalDate?>(
                {
                    DATE_YMD.find(line)?.let {
                        LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
                    }
                },
                {
                    DATE_DMY.find(line)?.let {
                        dayFirstOrMonthFirst(it.groupValues[3].toInt(), it.groupValues[1].toInt(), it.groupValues[2].toInt())
                    }
                },
                {
                    DATE_DMY2.find(line)?.let {
                        dayFirstOrMonthFirst(2000 + it.groupValues[3].toInt(), it.groupValues[1].toInt(), it.groupValues[2].toInt())
                    }
                },
                {
                    DATE_DMON.find(line)?.let {
                        val month = MONTHS.indexOf(it.groupValues[2].lowercase(Locale.ROOT).take(3)) + 1
                        var year = it.groupValues[3].toInt()
                        if (year < 100) year += 2000
                        LocalDate.of(year, month, it.groupValues[1].toInt())
                    }
                }
            )
            for (candidate in candidates) {
                val date = try {
                    candidate()
                } catch (e: DateTimeException) {
                    null
                } ?: continue
                // ignore OCR garbage such as 2062-11-20
                if (!date.isBefore(today.minusDays(3650)) && !date.isAfter(today.plusDays(1))) {
                    return date.toString()
                }
            }
        }
        return null
    }

    // ---------- merchant ----------
    private fun editDistance(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return dp[b.length]
    }

    // "REGEIPT" (OCR typo for RECEIPT) is a heading, not a shop name
    private fun looksLikeDocTitle(line: String): Boolean =
        line.lowercase(Locale.ROOT).split(Regex("""[^a-z]+""")).any { w ->
            w.length >= 5 && listOf("receipt", "invoice").any { editDistance(w, it) <= 2 }
        }

    // Scores the first lines: business-name words and being near the top raise the score,
    // address words, postcodes, commas and digits lower it. Returns null rather than guess
    // an address, so the user just types the title.
    private fun merchant(lines: List<String>): String? {
        var best: String? = null
        var bestScore = Int.MIN_VALUE

        for ((index, raw) in lines.take(8).withIndex()) {
            val line = NAME_NOISE.replace(raw, "").trim()
            val letters = line.count { it.isLetter() }
            val digits = line.count { it.isDigit() }
            if (letters < 3 || digits > letters) continue
            if (MERCHANT_HARD_SKIP.containsMatchIn(line)) continue
            if (looksLikeDocTitle(line)) continue
            if (AMT.containsMatchIn(line)) continue          // a price line, not a name

            var score = 0
            if (COMPANY_WORDS.containsMatchIn(line)) score += 6
            if (ADDRESS_WORDS.containsMatchIn(line)) score -= 5
            if (line.contains(',')) score -= 1
            if (digits > 0) score -= 1
            if (line.length > 40) score -= 2
            score += when (index) { 0 -> 2; 1 -> 1; else -> 0 }

            if (score > bestScore) {
                bestScore = score
                best = line
            }
        }
        return if (bestScore >= 0) best?.let { titleCase(it) } else null
    }

    // ---------- one item line: "2 x Nasi Lemak 25.00" ----------
    private fun parseItem(line: String): ReceiptItem? {
        val a = amounts(line)
        if (a.isEmpty()) return null
        val last = a.last()
        // the price must sit at the end of the line
        val tail = line.substring(last.range.last + 1).trim()
        if (tail.length > 3) return null

        var head = line.substring(0, a.first().range.first).replace("@", " ")
        // a currency sign glued to the price: "...256GBS1199.00"
        if (head.isNotEmpty() && head.last() in "S$") head = head.dropLast(1)
        head = head.trim()
        var qty = 1

        val lead = LEAD_QTY.find(head)
        val leadQty = lead?.groupValues?.get(1)?.toInt()
        // "1x Tshirt" may be any small number; a bare leading number must be 1-9 ("15 Pro Max" is not a quantity)
        val explicitX = lead?.groupValues?.get(2)?.isNotEmpty() == true
        if (lead != null && leadQty != null && leadQty in 1..(if (explicitX) 20 else 9)) {
            qty = leadQty
            head = head.substring(lead.range.last + 1)
        } else {
            val x = TRAIL_X.find(head)
            val xQty = x?.groupValues?.get(1)?.toInt()
            if (x != null && xQty != null && xQty in 1..20) {
                qty = xQty
                head = head.substring(0, x.range.first)
            } else {
                val t = TRAIL_NUM.find(head)
                val tQty = t?.groupValues?.get(1)?.toInt()
                // 1-9 only: a trailing "15" in "AppleCare for iPhone 15" is a model number, not 15 pieces
                if (t != null && tQty != null && tQty in 1..9 &&
                    TWO_LETTERS.containsMatchIn(head.substring(0, t.range.first))
                ) {
                    qty = tQty
                    head = head.substring(0, t.range.first)
                }
            }
        }

        head = CURRENCY_TAIL.replace(head, "")
        head = ITEM_CODE.replace(head, "")
        val name = NAME_NOISE.replace(head, "").trim()
        if (ANY_LETTER.findAll(name).count() < 2 || HEADER.containsMatchIn(name)) return null
        if (last.value <= 0.0) return null

        return ReceiptItem(
            name = titleCase(name),
            quantity = qty,
            unitPrice = Math.round(last.value / qty * 100.0) / 100.0
        )
    }

    // ==========================================
    // PUBLIC ENTRY POINT
    // ==========================================
    fun parse(rows: List<String>, today: LocalDate = LocalDate.now()): ReceiptData {
        val lines = rows.map { norm(it) }.filter { it.isNotEmpty() }
        val kinds = lines.map { kindOf(it) }

        // Items live above the first summary line (subtotal / tax / total / payment)
        val firstSummary = kinds.indexOfFirst { it in SUMMARY_KINDS }.let { if (it < 0) lines.size else it }

        val warnings = mutableListOf<String>()
        val items = mutableListOf<ReceiptItem>()
        for (i in 0 until firstSummary) {
            val line = lines[i]
            when (kinds[i]) {
                Kind.DISCOUNT -> {
                    val w = "Discount found on the receipt - apply it manually."
                    if (w !in warnings) warnings += w
                }
                Kind.OTHER -> parseItem(line)?.let { items += it }
                else -> Unit
            }
        }
        val itemsSum = Math.round(items.sumOf { it.lineTotal } * 100.0) / 100.0

        fun firstOf(kind: Kind): Pair<String, Double>? {
            for (i in lines.indices) {
                if (kinds[i] == kind) lastAmount(lines[i])?.let { return lines[i] to it }
            }
            return null
        }

        val subLine = firstOf(Kind.SUBTOTAL)
        val sstLine = firstOf(Kind.SST)
        val scLine = firstOf(Kind.SERVICE)

        val subtotal = subLine?.second
        var sst = sstLine?.second
        var sc = scLine?.second

        val totals = lines.indices
            .filter { kinds[it] == Kind.TOTAL && lastAmount(lines[it]) != null }
            .map { lines[it] to lastAmount(lines[it])!! }
        val total = (totals.firstOrNull { STRONG_TOTAL.containsMatchIn(it.first) } ?: totals.firstOrNull())?.second

        val basis = subtotal ?: itemsSum

        // Rate that reproduces the printed amount, as simple as possible (6, 6.6, 8.875 ...)
        fun pct(amount: Double?, line: String?): Double? {
            if (amount == null) return null
            if (basis > 0) {
                val exact = amount / basis * 100
                for (decimals in 0..3) {
                    val f = Math.pow(10.0, decimals.toDouble())
                    val p = Math.round(exact * f) / f
                    if (abs(basis * p / 100 - amount) <= 0.005) return p
                }
                return Math.round(exact * 1000) / 1000.0
            }
            return line?.let { PCT.find(it)?.groupValues?.get(1)?.toDouble() }
        }

        var sstPct = pct(sst, sstLine?.first)
        var scPct = pct(sc, scLine?.first)

        // Total equals the items => the tax line is only informational (prices already include it)
        if (total != null && ((sst ?: 0.0) > 0 || (sc ?: 0.0) > 0) && abs(total - basis) <= 0.10) {
            sst = null; sc = null; sstPct = null; scPct = null
            warnings += "Prices already include tax/service charge, so none was added."
        }

        if (items.isEmpty()) warnings += "No items could be read - add them manually."
        if (subtotal != null && items.isNotEmpty() && abs(subtotal - itemsSum) > 0.05) {
            warnings += "Items add up to %.2f but the receipt subtotal is %.2f. Check for missing items."
                .format(Locale.US, itemsSum, subtotal)
        }
        if (total != null && items.isNotEmpty()) {
            val expected = (subtotal ?: itemsSum) + (sst ?: 0.0) + (sc ?: 0.0)
            if (abs(expected - total) > 0.10) {
                warnings += "Items + tax come to %.2f but the receipt total is %.2f. Check the amounts."
                    .format(Locale.US, expected, total)
            }
        }

        return ReceiptData(
            merchant = merchant(lines),
            date = parseDate(lines, today),
            items = items,
            subtotal = subtotal,
            sstAmount = sst,
            serviceChargeAmount = sc,
            total = total,
            sstPercent = sstPct,
            serviceChargePercent = scPct,
            warnings = warnings
        )
    }
}
