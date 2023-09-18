package de.dkfz.odcf.guide.entity

import de.dkfz.odcf.guide.entity.submissionData.Submission
import java.text.SimpleDateFormat
import java.util.*
import javax.persistence.*

@Entity
class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id = 0

    var date: Date = Date()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    lateinit var submission: Submission

    @Enumerated(EnumType.STRING)
    var rating: Rating = Rating.NEUTRAL
        private set

    var message: String = ""

    var ticket: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    var user: Person? = null

    /*================================================================================================================*/

    val formattedDate: String?
        get() {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd")
            return formattedDate.format(this.date)
        }

    fun setRating(smiley: String?) {
        if (!smiley.isNullOrEmpty()) {
            when (smiley) {
                "happy" -> {
                    this.rating = Rating.HAPPY
                    return
                }
                "neutral" -> {
                    this.rating = Rating.NEUTRAL
                    return
                }
                "sad" -> {
                    this.rating = Rating.SAD
                    return
                }
            }
        }
    }
    enum class Rating {
        SAD,
        NEUTRAL,
        HAPPY
    }
}
