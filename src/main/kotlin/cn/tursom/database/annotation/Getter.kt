package cn.tursom.database.annotation

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class Getter(val getter: String)