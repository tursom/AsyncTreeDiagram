package cn.tursom.treediagram.modinterface

@Target(AnnotationTarget.CLASS)
annotation class AbsPath(vararg val paths: String)