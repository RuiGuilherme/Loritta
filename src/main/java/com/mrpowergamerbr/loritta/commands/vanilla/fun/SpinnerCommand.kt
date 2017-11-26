package com.mrpowergamerbr.loritta.commands.vanilla.`fun`

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Aggregates.project
import com.mongodb.client.model.Aggregates.sort
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mrpowergamerbr.loritta.Loritta
import com.mrpowergamerbr.loritta.commands.CommandBase
import com.mrpowergamerbr.loritta.commands.CommandCategory
import com.mrpowergamerbr.loritta.commands.CommandContext
import com.mrpowergamerbr.loritta.userdata.LorittaProfile
import com.mrpowergamerbr.loritta.utils.ImageUtils
import com.mrpowergamerbr.loritta.utils.LoriReply
import com.mrpowergamerbr.loritta.utils.LorittaUtils
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import com.mrpowergamerbr.loritta.utils.loritta
import com.mrpowergamerbr.loritta.utils.lorittaShards
import com.mrpowergamerbr.loritta.utils.makeRoundedCorners
import com.mrpowergamerbr.loritta.utils.save
import com.mrpowergamerbr.loritta.utils.toBufferedImage
import org.bson.Document
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class SpinnerCommand : CommandBase("spinner") {
	var spinningSpinners: MutableMap<String, FidgetSpinner> = mutableMapOf<String, FidgetSpinner>()

	data class FidgetSpinner(var emoji: String, var threadId: Long, var forTime: Int, var spinnedAt: Long, var lastRerotation: Long)

	override fun getAliases(): List<String> {
		return listOf("fidget", "fidgetspinner");
	}

	override fun getDescription(locale: BaseLocale): String {
		return locale["SPINNER_DESCRIPTION"]
	}

	override fun getCategory(): CommandCategory {
		return CommandCategory.FUN;
	}

	override fun run(context: CommandContext) {
		if (context.args.isNotEmpty()) {
			val arg = context.args[0]
			val page = if (context.args.size == 2) { context.args[1].toIntOrNull() ?: 1 } else { 1 }
			if (arg == "rank") {
				val documents = loritta.mongo
						.getDatabase("loritta")
						.getCollection("users")
						.aggregate(listOf(
								Aggregates.match(Filters.exists("spinnerScores")),
								project(
										Projections.include("_id", "spinnerScores")
								),
								Document("\$unwind", "\$spinnerScores"),
								sort(Document("spinnerScores.forTime", -1))
								// limit(5)
							)
						)

				val rankHeader = ImageIO.read(File(Loritta.ASSETS, "rank_header.png"))
				val base = BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB_PRE)
				val graphics = base.graphics as Graphics2D

				graphics.setRenderingHint(
						java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
						java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

				graphics.color = Color(30, 33, 36)
				graphics.fillRect(0, 0, 400, 37)
				graphics.color = Color.WHITE
				// graphics.drawImage(serverIcon, 259, -52, null)

				graphics.drawImage(rankHeader, 0, 0, null)

				val oswaldRegular10 = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,
						java.io.FileInputStream(java.io.File(com.mrpowergamerbr.loritta.Loritta.ASSETS + "oswald_regular.ttf")))
						.deriveFont(10F)

				val oswaldRegular12 = oswaldRegular10
						.deriveFont(12F)

				val oswaldRegular16 = oswaldRegular10
						.deriveFont(16F)

				val oswaldRegular20 = oswaldRegular10
						.deriveFont(20F)

				graphics.font = oswaldRegular16

				ImageUtils.drawCenteredString(graphics, "Ranking Global de Spinners", Rectangle(0, 0, 400, 26), oswaldRegular16)

				var idx = 0
				var currentY = 37;

				var total = 0L
				for ((index, document) in documents.withIndex()) {
					val spinnerScore = document["spinnerScores"] as Document
					val forTime = spinnerScore.getLong("forTime")
					total += forTime
					if (index !in (4 * (page - 1) + if (page != 1) 1 else 0)..(4 * page) + if (page != 1) 1 else 0) {
						continue
					}
					val userId = document.getString("_id")

					val user = lorittaShards.getUserById(userId)

					if (user != null) {
						// val userProfile = loritta.getLorittaProfileForUser(id)
						val file = java.io.File("/home/servers/loritta/frontend/static/assets/img/backgrounds/${userId}.png");
						val imageUrl = if (file.exists()) "https://loritta.website/assets/img/backgrounds/${userId}.png?time=" + System.currentTimeMillis() else "https://loritta.website/assets/img/backgrounds/default_background.png";

						val rankBackground = LorittaUtils.downloadImage(imageUrl)
						graphics.drawImage(rankBackground.getScaledInstance(400, 300, BufferedImage.SCALE_SMOOTH)
								.toBufferedImage()
								.getSubimage(0, idx * 52, 400, 53), 0, currentY, null)

						graphics.color = Color(0, 0, 0, 127)
						graphics.fillRect(0, currentY, 400, 53)

						graphics.color = Color(255, 255, 255)

						graphics.font = oswaldRegular20

						ImageUtils.drawTextWrap(user.name, 143, currentY + 21, 9999, 9999, graphics.fontMetrics, graphics)

						graphics.font = oswaldRegular16

						ImageUtils.drawTextWrap("${forTime} segundos", 144, currentY + 38, 9999, 9999, graphics.fontMetrics, graphics)

						graphics.font = oswaldRegular10

						// ImageUtils.drawTextWrap("Nível " + userData.getCurrentLevel().currentLevel, 145, currentY + 48, 9999, 9999, graphics.fontMetrics, graphics)

						val avatar = LorittaUtils.downloadImage(user.effectiveAvatarUrl).getScaledInstance(143, 143, BufferedImage.SCALE_SMOOTH)

						var editedAvatar = BufferedImage(143, 143, BufferedImage.TYPE_INT_ARGB)
						val avatarGraphics = editedAvatar.graphics as Graphics2D

						val path = Path2D.Double()
						path.moveTo(0.0, 45.0)
						path.lineTo(132.0, 45.0)
						path.lineTo(143.0, 98.0)
						path.lineTo(0.0, 98.0)
						path.closePath()

						avatarGraphics.clip = path

						avatarGraphics.drawImage(avatar, 0, 0, null)

						editedAvatar = editedAvatar.getSubimage(0, 45, 143, 53)
						graphics.drawImage(editedAvatar, 0, currentY, null)

						val emoji = spinnerScore.getString("emoji")
						val image = when (emoji) {
							"<:spinner8:344292269836206082>" -> "https://cdn.discordapp.com/emojis/344292269836206082.png"
							"<:spinner2:327245670052397066>" -> "https://cdn.discordapp.com/emojis/327245670052397066.png"
							"<:spinner3:327246151591919627>" -> "https://cdn.discordapp.com/emojis/327246151591919627.png"
							"<:spinner4:344292269764902912>" -> "https://cdn.discordapp.com/emojis/344292269764902912.png"
							"<:spinner5:344292269160923147>" -> "https://cdn.discordapp.com/emojis/344292269160923147.png"
							"<:spinner6:344292270125613056>" -> "https://cdn.discordapp.com/emojis/344292270125613056.png"
							"<:spinner7:344292270268350464>" -> "https://cdn.discordapp.com/emojis/344292270268350464.png"
							"<:spinner1:327243530244325376>" -> "https://cdn.discordapp.com/emojis/327243530244325376.png"
							else -> "https://cdn.discordapp.com/emojis/366047906689581085.png"
						}

						val spinner = LorittaUtils.downloadImage(image).getScaledInstance(49, 49, BufferedImage.SCALE_SMOOTH)
						graphics.drawImage(spinner, 400 - 49 - 2, currentY + 2, null)

						idx++
						currentY += 53;
					}
				}

				var _total = total * 1000

				val days = TimeUnit.MILLISECONDS.toDays(_total)
				_total -= TimeUnit.DAYS.toMillis(days)
				val hours = TimeUnit.MILLISECONDS.toHours(_total)
				_total -= TimeUnit.HOURS.toMillis(hours)
				val minutes = TimeUnit.MILLISECONDS.toMinutes(_total)
				_total -= TimeUnit.MINUTES.toMillis(minutes)
				val seconds = TimeUnit.MILLISECONDS.toSeconds(_total)

				// embed.setFooter("⏰ No total, ${days}d ${hours}h ${minutes}m ${seconds}s foram gastos girando spinners! (Wow, quanto tempo!)", null)
				graphics.font = oswaldRegular12
				ImageUtils.drawCenteredString(graphics, "No total, ${days}d ${hours}h ${minutes}m ${seconds}s foram gastos girando spinners! (Wow, quanto tempo!)", Rectangle(0, 11, 400, 28), oswaldRegular12)

				context.sendFile(base.makeRoundedCorners(15), "spinner_rank.png", context.getAsMention(true))
				return
			}
		}
		if (spinningSpinners.contains(context.userHandle.id)) {
			val spinner = spinningSpinners[context.userHandle.id]!!

			val diff = (System.currentTimeMillis() - spinner.lastRerotation) / 1000

			if (diff in spinner.forTime-10..spinner.forTime) {
				var time = Loritta.random.nextInt(10, 61);

				var lowerBound = Math.max(0, time - Loritta.random.nextInt(-5, 6))
				var upperBound = Math.max(0, time - Loritta.random.nextInt(-5, 6))

				if (lowerBound > upperBound) {
					val temp = upperBound;
					upperBound = lowerBound
					lowerBound = temp
				}

				context.reply(
						LoriReply(
								message = context.locale["SPINNER_RESPINNED"],
								prefix = spinner.emoji
						),
						LoriReply(
								message = "*" + context.locale["SPINNER_MAGIC_BALL", lowerBound, upperBound] + "*",
								prefix = "\uD83D\uDD2E"
						)
				)

				val waitThread = thread {
					Thread.sleep((time * 1000).toLong());

					if (spinningSpinners.contains(context.userHandle.id)) {
						val spinner = spinningSpinners[context.userHandle.id]!!

						if (spinner.threadId != Thread.currentThread().id) {
							return@thread
						}
						val diff = (System.currentTimeMillis() - spinner.spinnedAt) / 1000

						context.reply(
								LoriReply(
										message = context.locale["SPINNER_SPINNED", diff],
										prefix = spinner.emoji
								),
								LoriReply(
										message = context.locale["SPINNER_ViewRank", context.config.commandPrefix],
										prefix = "\uD83C\uDFC6"
								)
						)

						spinningSpinners.remove(context.userHandle.id)
						val profile = loritta.getLorittaProfileForUser(context.userHandle.id)
						profile.spinnerScores.add(LorittaProfile.SpinnerScore(spinner.emoji, diff))
						loritta save profile
					}
				}

				spinner.lastRerotation = System.currentTimeMillis()
				spinner.threadId = waitThread.id
				spinner.forTime = time
				spinningSpinners.put(context.userHandle.id, spinner)
			} else {
				val diff = (System.currentTimeMillis() - spinner.spinnedAt) / 1000

				context.reply(
						LoriReply(
								message = "${context.locale["SPINNER_OUCH"]} ${context.locale["SPINNER_SPINNED", diff]}",
								prefix = spinner.emoji
						),
						LoriReply(
								message = context.locale["SPINNER_ViewRank", context.config.commandPrefix],
								prefix = "\uD83C\uDFC6"
						)
				)

				spinningSpinners.remove(context.userHandle.id)

				val profile = loritta.getLorittaProfileForUser(context.userHandle.id)
				profile.spinnerScores.add(LorittaProfile.SpinnerScore(spinner.emoji, diff))
				loritta save profile
			}
			return
		}
		var time = Loritta.random.nextInt(10, 61); // Tempo que o Fidget Spinner irá ficar rodando

		var random = listOf("<:spinner1:327243530244325376>", "<:spinner2:327245670052397066>", "<:spinner3:327246151591919627>", "<:spinner4:344292269764902912>", "<:spinner5:344292269160923147>", "<:spinner6:344292270125613056>", "<:spinner7:344292270268350464>", "<:spinner8:344292269836206082>") // Pegar um spinner aleatório
		var spinnerEmoji = random[Loritta.random.nextInt(random.size)]

		var lowerBound = Math.max(0, time - Loritta.random.nextInt(-5, 6))
		var upperBound = Math.max(0, time - Loritta.random.nextInt(-5, 6))

		if (lowerBound > upperBound) {
			val temp = upperBound;
			upperBound = lowerBound
			lowerBound = temp
		}

		val msg = context.reply(
				LoriReply(
						message = context.locale["SPINNER_SPINNING"],
						prefix = spinnerEmoji
				),
				LoriReply(
						message = "*" + context.locale["SPINNER_MAGIC_BALL", lowerBound, upperBound] + "*",
						prefix = "\uD83D\uDD2E"
				)
		)

		val waitThread = thread {
			Thread.sleep((time * 1000).toLong());

			if (spinningSpinners.contains(context.userHandle.id)) {
				val spinner = spinningSpinners[context.userHandle.id]!!

				if (spinner.threadId != Thread.currentThread().id) {
					return@thread
				}
				msg.delete().complete()

				context.reply(
						LoriReply(
								message = context.locale["SPINNER_SPINNED", time],
								prefix = spinnerEmoji
						),
						LoriReply(
								message = context.locale["SPINNER_ViewRank", context.config.commandPrefix],
								prefix = "\uD83C\uDFC6"
						)
				)

				spinningSpinners.remove(context.userHandle.id)
				val profile = loritta.getLorittaProfileForUser(context.userHandle.id)
				profile.spinnerScores.add(LorittaProfile.SpinnerScore(spinner.emoji, time.toLong()))
				loritta save profile
			}
		}

		val fidgetSpinner = FidgetSpinner(spinnerEmoji, waitThread.id, time, System.currentTimeMillis(), System.currentTimeMillis())

		spinningSpinners.put(context.userHandle.id, fidgetSpinner)
	}
}