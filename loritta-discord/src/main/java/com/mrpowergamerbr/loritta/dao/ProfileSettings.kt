package com.mrpowergamerbr.loritta.dao

import com.mrpowergamerbr.loritta.tables.UserSettings
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass

class ProfileSettings(id: EntityID<Long>) : LongEntity(id) {
	companion object : LongEntityClass<ProfileSettings>(UserSettings)

	var aboutMe by UserSettings.aboutMe
	var gender by UserSettings.gender
	var activeProfile by UserSettings.activeProfile
	var activeBackground by Background optionalReferencedOn UserSettings.activeBackground
	var activeBackgroundInternalName by UserSettings.activeBackground
	var boughtProfiles by UserSettings.boughtProfiles
	var birthday by UserSettings.birthday
	var doNotSendXpNotificationsInDm by UserSettings.doNotSendXpNotificationsInDm
	var discordAccountFlags by UserSettings.discordAccountFlags
	var discordPremiumType by UserSettings.discordPremiumType
}