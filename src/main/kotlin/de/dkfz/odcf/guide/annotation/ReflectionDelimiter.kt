package de.dkfz.odcf.guide.annotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReflectionDelimiter(val delimiter: String)
