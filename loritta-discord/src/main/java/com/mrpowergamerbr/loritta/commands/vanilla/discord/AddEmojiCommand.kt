package com.mrpowergamerbr.loritta.commands.vanilla.discord

import com.mrpowergamerbr.loritta.commands.AbstractCommand
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.LoriReply
import com.mrpowergamerbr.loritta.utils.LorittaUtils
import com.mrpowergamerbr.loritta.utils.extensions.await
import com.mrpowergamerbr.loritta.utils.locale.LegacyBaseLocale
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.perfectdreams.loritta.api.commands.ArgumentType
import net.perfectdreams.loritta.api.commands.CommandArguments
import net.perfectdreams.loritta.api.commands.CommandCategory
import net.perfectdreams.loritta.api.commands.arguments

class AddEmojiCommand : AbstractCommand("addemoji", listOf("adicionaremoji"), CommandCategory.DISCORD) {
	override fun getUsage(locale: LegacyBaseLocale): CommandArguments {
		return arguments {
			argument(ArgumentType.TEXT) {
				optional = false
			}
			argument(ArgumentType.IMAGE) {
				optional = false
			}
		}
	}

	override fun getDescription(locale: LegacyBaseLocale): String {
		return locale.toNewLocale()["commands.discord.addemoji.description"]
	}

	override fun getDiscordPermissions(): List<Permission> {
		return listOf(Permission.MANAGE_EMOTES)
	}

	override fun getBotPermissions(): List<Permission> {
		return listOf(Permission.MANAGE_EMOTES)
	}

	override suspend fun run(context: CommandContext,locale: LegacyBaseLocale) {
		var imageArgument = 1
		var emoteName: String? = null

		if (context.message.emotes.isNotEmpty()) {
			imageArgument = 0
			emoteName = context.message.emotes[0].name
		}

		if (imageArgument > context.rawArgs.size) {
			context.explain()
			return
		}

		if (emoteName == null)
			emoteName = context.rawArgs[0]

		val imageUrl = context.getImageUrlAt(imageArgument, 1) ?: run { Constants.INVALID_IMAGE_REPLY.invoke(context); return; }

		try {
			val os = LorittaUtils.downloadFile(imageUrl, 5000)

			if (os != null) {
				os.use { inputStream ->
					val emote = context.guild.createEmote(emoteName, Icon.from(inputStream)).await()
					context.reply(
							LoriReply(
									context.legacyLocale.toNewLocale()["commands.discord.addemoji.success"],
									emote.asMention
							)
					)
				}
			} else {
				throw RuntimeException("Couldn't download image!")
			}
		} catch (e: Exception) {
			if (e is ErrorResponseException) {
				if (e.errorCode == 30008) {
					context.reply(
							LoriReply(
									context.legacyLocale.toNewLocale()["commands.discord.addemoji.limitReached"],
									Constants.ERROR
							)
					)
					return
				}
				if (e.errorCode == 400) {
					context.reply(
							LoriReply(
									context.legacyLocale.toNewLocale()["commands.discord.addemoji.emoteTooBig", "`256kb`"],
									Constants.ERROR
							)
					)
					return
				}
			}

			context.reply(
					LoriReply(
							context.legacyLocale.toNewLocale()["commands.discord.addemoji.error"],
							Constants.ERROR
					)
			)
		}
	}
}
