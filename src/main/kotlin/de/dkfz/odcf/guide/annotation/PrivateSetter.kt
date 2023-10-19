package de.dkfz.odcf.guide.annotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrivateSetter(val name: String)
