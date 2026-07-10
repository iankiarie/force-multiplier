package com.ian.forcemultiplier.domain.model

data class Folder(
    val id: String,
    val name: String,
    val icon: String? = null,
    val tagFilter: String? = null
)

