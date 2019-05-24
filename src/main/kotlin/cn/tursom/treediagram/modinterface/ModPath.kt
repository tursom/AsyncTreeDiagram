package cn.tursom.treediagram.modinterface

@Target(AnnotationTarget.CLASS)
annotation class ModPath(vararg val path: String)
