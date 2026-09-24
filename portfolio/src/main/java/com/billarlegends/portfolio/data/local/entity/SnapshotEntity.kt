package com.billarlegends.portfolio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.billarlegends.portfolio.domain.Source

/** Valor total (en USD) de una cuenta/negocio en un momento dado, para poder graficar evolución. */
@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: Source,
    val ownerId: Long,
    val timestampMillis: Long,
    val valueUsd: Double,
)
