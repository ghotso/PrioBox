package com.vipmail.utils

fun String?.toEmailList(): List<String> =
    this.orEmpty()
        .split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }

