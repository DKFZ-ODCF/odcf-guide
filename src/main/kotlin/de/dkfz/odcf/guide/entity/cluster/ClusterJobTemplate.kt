package de.dkfz.odcf.guide.entity.cluster

import javax.persistence.*

@Entity
class ClusterJobTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0

    @Column(unique = true)
    lateinit var name: String

    lateinit var command: String

    var maximumRuntime: Int = 1 * 24 * 60

    var estimatedRuntimePerSample: Int = 0

    var group: String = "default-guide-group"

    var clusterJobVisibleForUser: Boolean = true

    @OneToOne(orphanRemoval = true)
    @JoinColumn
    var subsequentJobTemplate: ClusterJobTemplate? = null
}
