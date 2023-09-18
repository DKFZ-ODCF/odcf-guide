package de.dkfz.odcf.guide.entity.otpCached

import java.text.DecimalFormat
import java.util.*
import javax.persistence.*
import kotlin.math.pow

@Entity
class OtpCachedProject {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    // only for DB to check if cron not working
    lateinit var latestUpdate: Date

    lateinit var name: String

    lateinit var unixGroup: String

    lateinit var pis: String

    var closed = false

    lateinit var seqTypes: String

    lateinit var lastDataReceived: String

    lateinit var pathProjectFolder: String

    lateinit var pathAnalysisFolder: String

    var sizeProjectFolder: Long = 0

    var sizeAnalysisFolder: Long = 0

    var quotaProjectFolder: Long = -1

    var quotaAnalysisFolder: Long = -1

    fun getProjectSize(): String {
        return getSize(this.sizeProjectFolder)
    }
    fun getAnalysisSize(): String {
        return getSize(this.sizeAnalysisFolder)
    }
    fun getProjectQuotaSize(): String {
        return getSizeInTB(quotaProjectFolder)
    }
    fun getAnalysisQuotaSize(): String {
        return getSizeInTB(quotaAnalysisFolder)
    }

    private fun getSize(size: Long): String {
        val dec = DecimalFormat("###,###,##0")
        return if (size > 0) {
            // from B (2^0) to GB (2^30)
            return "${dec.format(size / 2.0.pow(30))} GB"
        } else {
            "N/A"
        }
    }

    private fun getSizeInTB(size: Long): String {
        val dec = DecimalFormat("###,###,##0")
        return if (size > 0) {
            // from B (2^0) to TB (2^40)
            return dec.format(size / 2.0.pow(40))
        } else {
            ""
        }
    }
}
