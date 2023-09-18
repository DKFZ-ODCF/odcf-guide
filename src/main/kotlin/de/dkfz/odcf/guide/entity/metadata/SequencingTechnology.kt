package de.dkfz.odcf.guide.entity.metadata

import de.dkfz.odcf.guide.entity.basic.ImportAliases
import de.dkfz.odcf.guide.entity.cluster.ClusterJobTemplate
import de.dkfz.odcf.guide.entity.validation.ValidationLevel
import javax.persistence.*

@Entity
class SequencingTechnology() : ImportAliases() {

    constructor(id: Int, name: String, importAliases: String, validationLevel: ValidationLevel? = null) : this() {
        this.id = id
        this.name = name
        this.importAliasesString = importAliases
        this.validationLevel = validationLevel
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    @Column(unique = true)
    lateinit var name: String

    var defaultObject: Boolean = false

    var checkExternalMetadataSource: Boolean = true

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    var validationLevel: ValidationLevel? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    var clusterJobTemplate: ClusterJobTemplate? = null

    /*================================================================================================================*/

    val hasClusterJobTemplate: Boolean
        get() = clusterJobTemplate != null
}
