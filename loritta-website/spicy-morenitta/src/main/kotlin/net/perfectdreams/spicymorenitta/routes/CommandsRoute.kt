package net.perfectdreams.spicymorenitta.routes

import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.parseList
import net.perfectdreams.loritta.api.commands.CommandCategory
import net.perfectdreams.spicymorenitta.SpicyMorenitta
import net.perfectdreams.spicymorenitta.application.ApplicationCall
import net.perfectdreams.spicymorenitta.http
import net.perfectdreams.spicymorenitta.locale
import net.perfectdreams.spicymorenitta.utils.select
import org.w3c.dom.HTMLDivElement
import kotlin.browser.document
import kotlin.browser.window

class CommandsRoute(val m: SpicyMorenitta) : UpdateNavbarSizePostRender("/commands") {
    override val keepLoadingScreen: Boolean
        get() = true
    override val requiresUserIdentification = false

    @UseExperimental(ImplicitReflectionSerializer::class)
    override fun onRender(call: ApplicationCall) {
        super.onRender(call)

        m.launch {
            val result = http.get<String> {
                url("${window.location.origin}/api/v1/loritta/commands/${locale.id}")
            }

            val list = kotlinx.serialization.json.JSON.nonstrict.parseList<Command>(result)

            fixDummyNavbarHeight(call)

            val entriesDiv = document.select<HTMLDivElement>("#commands")

            var index = 0

            entriesDiv.append {

            }

            for (category in CommandCategory.values().filter { it != CommandCategory.MAGIC }) {
                val commands = list.filter { it.category == category }
                if (commands.isNotEmpty()) {
                    entriesDiv.append {
                        var classes = if (index % 2 == 0) "even-wrapper" else "odd-wrapper"
                        if (index != 0)
                            classes += " wobbly-bg"

                        div(classes = classes) {
                            id = "category-${category.name.toLowerCase().replace("_", "-")}"

                            if (index % 2 == 0) {
                                div {
                                    style = "text-align: center;"

                                    ins("adsbygoogle") {
                                        style = "display:block"
                                        attributes["data-ad-client"] = "ca-pub-9989170954243288"
                                        attributes["data-ad-slot"] = "4611100335"
                                        attributes["data-ad-format"] = "auto"
                                    }

                                    script {
                                        +"(adsbygoogle = window.adsbygoogle || []).push({});"
                                    }
                                }
                            }

                            div(classes = "media") {
                                div(classes = "media-figure") {
                                    val imageSource = when (category) {
                                        CommandCategory.SOCIAL -> "https://loritta.website/assets/img/social.png"
                                        CommandCategory.POKEMON -> "https://loritta.website/assets/img/pokemon.png"
                                        CommandCategory.MINECRAFT -> "https://loritta.website/assets/img/loritta_pudim.png"
                                        CommandCategory.FUN -> "https://loritta.website/assets/img/vieirinha.png"
                                        CommandCategory.UTILS -> "https://loritta.website/assets/img/utils.png"
                                        CommandCategory.MUSIC -> "https://loritta.website/assets/img/loritta_headset.png"
                                        CommandCategory.ANIME -> "https://loritta.website/assets/img/loritta_anime.png"
                                        CommandCategory.ECONOMY -> "https://loritta.website/assets/img/loritta_money_discord.png"
                                        CommandCategory.FORTNITE -> "https://loritta.website/assets/img/loritta_fortnite_icon.png"
                                        else -> "https://loritta.website/assets/img/loritta_gabizinha_v1.png"
                                    }

                                    img(src = imageSource) {
                                        style = "width: 100%; border-radius: 8px;"
                                    }
                                }

                                div(classes = "media-body") {
                                    div {
                                        style = "text-align: center;"
                                        h1 {
                                            // + category.getLocalizedName(locale)
                                        }
                                    }
                                    p {
                                        // + category.getLocalizedDescription(locale)
                                    }
                                }
                            }

                            div (classes = "media") {
                                div(classes = "media-body") {
                                    style = "overflow: auto;"
                                    
                                    hr {}

                                    table(classes = "fancy-table") {
                                        style = "width: 100%;"

                                        thead {
                                            tr {
                                                th {
                                                    +"Comando"
                                                }
                                                th {
                                                    +"Descrição"
                                                }
                                                th {
                                                    +"Aliases"
                                                }
                                            }
                                        }

                                        for (command in commands) {
                                            tr {
                                                td {
                                                    + command.label
                                                    if (command.usage != null) {
                                                        + " "
                                                        code {
                                                            + command.usage
                                                        }
                                                    }
                                                }
                                                td {
                                                    + (command.description ?: "-")
                                                }
                                                td {
                                                    + (command.aliases.joinToString(", "))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    index++
                }
            }

            m.hideLoadingScreen()
        }
    }

    fun DIV.createCommandEntry(entry: Command) {
    }

    companion object {
        @Serializable
        class Command(
                // É deserializado para String pois JavaScript é burro e não funciona direito com Longs
                val name: String,
                val label: String,
                val aliases: Array<String>,
                val category: CommandCategory,
                @Optional
                val description: String? = null,
                @Optional
                val usage: String? = null
        )
    }
}