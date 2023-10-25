package de.dkfz.odcf.guide.api.project

interface ProjectApiView {

    val name: String

    val pis: Set<ProjectApiPersonView>

    val pathProjectFolder: String

    val pathAnalysisFolder: String

    val sizeProjectFolder: Long

    val sizeAnalysisFolder: Long
}
