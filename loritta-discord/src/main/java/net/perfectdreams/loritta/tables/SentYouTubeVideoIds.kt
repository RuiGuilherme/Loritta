package net.perfectdreams.loritta.tables

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.sql.Column

object SentYouTubeVideoIds : IdTable<String>() {
	val channelId = text("channel")
	val videoId = text("video")
	override val id: Column<EntityID<String>> = videoId.entityId()

	val receivedAt = long("received_at")
}