package net.perfectdreams.loritta.platform.discord

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.salomonbrys.kotson.*
import com.google.gson.GsonBuilder
import com.mrpowergamerbr.loritta.Loritta
import com.mrpowergamerbr.loritta.commands.vanilla.social.PerfilCommand
import com.mrpowergamerbr.loritta.dao.Background
import com.mrpowergamerbr.loritta.dao.Profile
import com.mrpowergamerbr.loritta.network.Databases
import com.mrpowergamerbr.loritta.profile.ProfileDesignManager
import com.mrpowergamerbr.loritta.utils.Constants
import com.mrpowergamerbr.loritta.utils.config.GeneralConfig
import com.mrpowergamerbr.loritta.utils.config.GeneralDiscordConfig
import com.mrpowergamerbr.loritta.utils.config.GeneralDiscordInstanceConfig
import com.mrpowergamerbr.loritta.utils.config.GeneralInstanceConfig
import com.mrpowergamerbr.loritta.utils.locale.BaseLocale
import com.mrpowergamerbr.loritta.utils.locale.LegacyBaseLocale
import com.mrpowergamerbr.loritta.utils.loritta
import com.mrpowergamerbr.loritta.utils.toBufferedImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.userAgent
import net.perfectdreams.loritta.api.LorittaBot
import net.perfectdreams.loritta.commands.vanilla.magic.LoriToolsCommand
import net.perfectdreams.loritta.platform.discord.commands.DiscordCommandMap
import net.perfectdreams.loritta.platform.discord.plugin.JVMPluginManager
import net.perfectdreams.loritta.platform.discord.utils.JVMLorittaAssets
import net.perfectdreams.loritta.tables.BackgroundPayments
import net.perfectdreams.loritta.tables.Backgrounds
import net.perfectdreams.loritta.utils.UserPremiumPlans
import net.perfectdreams.loritta.utils.config.FanArt
import net.perfectdreams.loritta.utils.config.FanArtArtist
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.File
import java.lang.reflect.Modifier
import java.util.*
import javax.imageio.ImageIO
import kotlin.random.Random

/**
 * Loritta Morenitta :3
 *
 * This should be extended by plataform specific Lori's
 */
abstract class LorittaDiscord(var discordConfig: GeneralDiscordConfig, var discordInstanceConfig: GeneralDiscordInstanceConfig, var config: GeneralConfig, var instanceConfig: GeneralInstanceConfig) : LorittaBot() {
    override val commandMap = DiscordCommandMap(this).apply {
        register(LoriToolsCommand.create(discordLoritta))
    }
    override val pluginManager = JVMPluginManager(this)
    override val assets = JVMLorittaAssets(this)
    var locales = mapOf<String, BaseLocale>()
    var legacyLocales = mapOf<String, LegacyBaseLocale>()
    override val http = HttpClient(Apache) {
        this.expectSuccess = false

        engine {
            this.socketTimeout = 25_000
            this.connectTimeout = 25_000
            this.connectionRequestTimeout = 25_000

            customizeClient {
                // Maximum number of socket connections.
                this.setMaxConnTotal(100)

                // Maximum number of requests for a specific endpoint route.
                this.setMaxConnPerRoute(100)
            }
        }
    }
    override val random = Random(System.currentTimeMillis())

    var fanArtArtists = listOf<FanArtArtist>()
    val fanArts: List<FanArt>
        get() = fanArtArtists.flatMap { it.fanArts }
    val profileDesignManager = ProfileDesignManager()

    val isMaster: Boolean
        get() {
            return loritta.instanceConfig.loritta.currentClusterId == 1L
        }

    /**
     * Gets an user's profile background
     *
     * @param id the user's profile
     * @return the background image
     */
    suspend fun getUserProfileBackground(profile: Profile): BufferedImage {
        val background = transaction(Databases.loritta) { profile.settings.activeBackground }

        if (background?.id?.value == Background.RANDOM_BACKGROUND_ID) {
            // Caso o usuário tenha pegado um background random, vamos pegar todos os backgrounds que o usuário comprou e pegar um aleatório de lá
            val defaultBlueBackground = if (background.id.value != Background.DEFAULT_BACKGROUND_ID) transaction(Databases.loritta) { Background.findById(Background.DEFAULT_BACKGROUND_ID)!! } else background
            val allBackgrounds = mutableListOf(defaultBlueBackground)

            allBackgrounds.addAll(
                    transaction(Databases.loritta) {
                        (BackgroundPayments innerJoin Backgrounds).select {
                            BackgroundPayments.userId eq profile.id.value
                        }.map { Background.wrapRow(it) }
                    }
            )
            return getUserProfileBackground(allBackgrounds.random())
        }

        if (background?.id?.value == Background.CUSTOM_BACKGROUND_ID) {
            // Background personalizado
            val donationValue = loritta.getActiveMoneyFromDonations(profile.userId)
            val plan = UserPremiumPlans.getPlanFromValue(donationValue)

            if (plan.customBackground) {
                val response = loritta.http.get<HttpResponse>("${loritta.instanceConfig.loritta.website.url}assets/img/profiles/backgrounds/custom/${profile.userId}.png?t=${System.currentTimeMillis()}") {
                    userAgent(loritta.lorittaCluster.getUserAgent())
                }

                val bytes = response.readBytes()
                val image = ImageIO.read(bytes.inputStream())
                return image
            }
        }

        return getUserProfileBackground(background)
    }

    suspend fun getUserProfileBackgroundUrl(profile: Profile): String {
        var background = transaction(Databases.loritta) { profile.settings.activeBackground }

        if (background?.id?.value == Background.RANDOM_BACKGROUND_ID) {
            // Caso o usuário tenha pegado um background random, vamos pegar todos os backgrounds que o usuário comprou e pegar um aleatório de lá
            val defaultBlueBackground = if (background.id.value != Background.DEFAULT_BACKGROUND_ID) transaction(Databases.loritta) { Background.findById(Background.DEFAULT_BACKGROUND_ID)!! } else background
            val allBackgrounds = mutableListOf(defaultBlueBackground)

            allBackgrounds.addAll(
                    transaction(Databases.loritta) {
                        (BackgroundPayments innerJoin Backgrounds).select {
                            BackgroundPayments.userId eq profile.id.value
                        }.map { Background.wrapRow(it) }
                    }
            )
            background = allBackgrounds.random()
        }

        if (background?.id?.value == Background.CUSTOM_BACKGROUND_ID) {
            // Background personalizado
            val donationValue = loritta.getActiveMoneyFromDonations(profile.userId)
            val plan = UserPremiumPlans.getPlanFromValue(donationValue)

            if (plan.customBackground)
                return "${loritta.instanceConfig.loritta.website.url}assets/img/profiles/backgrounds/custom/${profile.userId}.png?t=${System.currentTimeMillis()}"
        }

        val backgroundOrDefault = background ?: transaction(Databases.loritta) {
            Background.findById(Background.DEFAULT_BACKGROUND_ID)!!
        }

        return "${loritta.instanceConfig.loritta.website.url}assets/img/profiles/backgrounds/${backgroundOrDefault.imageFile}"
    }

    /**
     * Gets an user's profile background
     *
     * @param background the user's background
     * @return the background image
     */
    suspend fun getUserProfileBackground(background: Background?): BufferedImage {
        val backgroundOrDefault = background ?: transaction(Databases.loritta) {
            Background.findById(Background.DEFAULT_BACKGROUND_ID)!!
        }

        val response = loritta.http.get<HttpResponse>("${loritta.instanceConfig.loritta.website.url}assets/img/profiles/backgrounds/${backgroundOrDefault.imageFile}") {
            userAgent(loritta.lorittaCluster.getUserAgent())
        }

        val bytes = response.readBytes()

        if (backgroundOrDefault.imageFile.endsWith(".loribg"))
            throw PerfilCommand.IsAnimatedBackgroundHack(bytes)

        val image = ImageIO.read(bytes.inputStream())
        val crop = backgroundOrDefault.crop
        if (crop != null) {
            // Perfil possível um crop diferenciado
            val offsetX = crop["offsetX"].int
            val offsetY = crop["offsetY"].int
            val width = crop["width"].int
            val height = crop["height"].int

            // Se o background possui um width/height diferenciado, mas é idêntico ao tamanho correto do perfil... apenas faça nada
            if (!(offsetX == 0 && offsetY == 0 && width == image.width && height == image.height)) {
                // Mas... e se for diferente? sad_cat
                return image.getSubimage(offsetX, offsetY, width, height).toBufferedImage()
            }
        }

        return image
    }

    /**
     * Loads the artists from the Fan Arts folder
     *
     * In the future this will be loaded from Loritta's website!
     */
    fun loadFanArts() {
        val f = File(instanceConfig.loritta.folders.fanArts)

        fanArtArtists = f.listFiles().filter { it.extension == "conf" }.map {
            loadFanArtArtist(it)
        }
    }

    /**
     * Loads an specific fan art artist
     */
    fun loadFanArtArtist(file: File): FanArtArtist = Constants.HOCON_MAPPER.readValue(file)

    fun getFanArtArtistByFanArt(fanArt: FanArt) = fanArtArtists.firstOrNull { fanArt in it.fanArts }

    /**
     * Initializes the [id] locale and adds missing translation strings to non-default languages
     *
     * @see BaseLocale
     */
    fun loadLocale(id: String, defaultLocale: BaseLocale?): BaseLocale {
        val locale = BaseLocale(id)
        if (defaultLocale != null) {
            // Colocar todos os valores padrões
            locale.localeEntries.putAll(defaultLocale.localeEntries)
        }

        val localeFolder = File(instanceConfig.loritta.folders.locales, id)

        if (localeFolder.exists()) {
            localeFolder.listFiles().filter { it.extension == "yml" || it.extension == "json" }.forEach {
                val entries = Constants.YAML.load<MutableMap<String, Any?>>(it.readText())

                fun transformIntoFlatMap(map: MutableMap<String, Any?>, prefix: String) {
                    map.forEach { (key, value) ->
                        if (value is Map<*, *>) {
                            transformIntoFlatMap(value as MutableMap<String, Any?>, "$prefix$key.")
                        } else {
                            locale.localeEntries[prefix + key] = value
                        }
                    }
                }

                transformIntoFlatMap(entries, "")
            }
        }

        return locale
    }

    /**
     * Initializes the available locales and adds missing translation strings to non-default languages
     *
     * @see BaseLocale
     */
    fun loadLocales() {
        val locales = mutableMapOf<String, BaseLocale>()

        val defaultLocale = loadLocale(Constants.DEFAULT_LOCALE_ID, null)
        locales[Constants.DEFAULT_LOCALE_ID] = defaultLocale

        val localeFolder = File(instanceConfig.loritta.folders.locales)
        localeFolder.listFiles().filter { it.isDirectory && it.name != Constants.DEFAULT_LOCALE_ID && !it.name.startsWith(".") /* ignorar .git */ }.forEach {
            locales[it.name] = loadLocale(it.name, defaultLocale)
        }

        for ((localeId, locale) in locales) {
            val languageInheritsFromLanguageId = locale["loritta.inheritsFromLanguageId"]

            if (languageInheritsFromLanguageId != Constants.DEFAULT_LOCALE_ID) {
                // Caso a linguagem seja filha de outra linguagem que não seja a default, nós iremos recarregar a linguagem usando o pai correto
                // Isso é útil já que linguagens internacionais seriam melhor que dependa de "en-us" em vez de "default".
                // Também seria possível implementar "linguagens auto geradas" com overrides específicos, por exemplo: "auto-en-us" -> "en-us"
                locales[localeId] = loadLocale(localeId, locales[languageInheritsFromLanguageId])
            }
        }

        this.locales = locales
    }

    /**
     * Initializes the available locales and adds missing translation strings to non-default languages
     *
     * @see LegacyBaseLocale
     */
    fun loadLegacyLocales() {
        val locales = mutableMapOf<String, LegacyBaseLocale>()

        // Carregar primeiro o locale padrão
        val defaultLocaleFile = File(instanceConfig.loritta.folders.locales, "default.json")
        val localeAsText = defaultLocaleFile.readText(Charsets.UTF_8)
        val defaultLocale = Loritta.GSON.fromJson(localeAsText, LegacyBaseLocale::class.java) // Carregar locale do jeito velho
        val defaultJsonLocale = Loritta.JSON_PARSER.parse(localeAsText).obj // Mas também parsear como JSON

        defaultJsonLocale.entrySet().forEach { (key, value) ->
            if (!value.isJsonArray) { // TODO: Listas!
                defaultLocale.strings.put(key, value.string)
            }
        }

        // E depois guardar o nosso default locale
        locales.put("default", defaultLocale)

        // Carregar todos os locales
        val localesFolder = File(instanceConfig.loritta.folders.locales)
        val prettyGson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        for (file in localesFolder.listFiles()) {
            if (file.extension == "json" && file.nameWithoutExtension != "default") {
                // Carregar o BaseLocale baseado no locale atual
                val localeAsText = file.readText(Charsets.UTF_8)
                val locale = prettyGson.fromJson(localeAsText, LegacyBaseLocale::class.java)
                locale.strings = HashMap<String, String>(defaultLocale.strings) // Clonar strings do default locale
                locales.put(file.nameWithoutExtension, locale)
                // Yay!
            }
        }

        // E agora preencher valores nulos e salvar as traduções
        for ((id, locale) in locales) {
            if (id != "default") {
                val jsonObject = Loritta.JSON_PARSER.parse(Loritta.GSON.toJson(locale))

                val localeFile = File(instanceConfig.loritta.folders.locales, "$id.json")
                val asJson = Loritta.JSON_PARSER.parse(localeFile.readText()).obj

                for ((id, obj) in asJson.entrySet()) {
                    if (obj.isJsonPrimitive && obj.asJsonPrimitive.isString) {
                        locale.strings.put(id, obj.string)
                    }
                }

                // Usando Reflection TODO: Remover
                for (field in locale::class.java.declaredFields) {
                    if (field.name == "strings" || Modifier.isStatic(field.modifiers)) {
                        continue
                    }
                    field.isAccessible = true

                    val ogValue = field.get(defaultLocale)
                    val changedValue = field.get(locale)

                    if (changedValue == null || ogValue.equals(changedValue)) {
                        field.set(locale, ogValue)
                        jsonObject[field.name] = null
                        if (ogValue is List<*>) {
                            val tree = prettyGson.toJsonTree(ogValue)
                            jsonObject["[Translate!]${field.name}"] = tree
                        } else {
                            jsonObject["[Translate!]${field.name}"] = ogValue
                        }
                    } else {
                        if (changedValue is List<*>) {
                            val tree = prettyGson.toJsonTree(changedValue)
                            jsonObject[field.name] = tree
                        }
                    }
                }

                for ((id, ogValue) in defaultLocale.strings) {
                    val changedValue = locale.strings[id]

                    if (ogValue.equals(changedValue)) {
                        jsonObject["[Translate!]$id"] = ogValue
                    } else {
                        jsonObject[id] = changedValue
                        locale.strings.put(id, changedValue!!)
                    }
                }

                File(instanceConfig.loritta.folders.locales, "$id.json").writeText(prettyGson.toJson(jsonObject))
            }
        }

        this.legacyLocales = locales
    }

    /**
     * Gets the BaseLocale from the ID, if the locale doesn't exist, the default locale ("default") will be retrieved
     *
     * @param localeId the ID of the locale
     * @return         the locale on BaseLocale format or, if the locale doesn't exist, the default locale will be loaded
     * @see            LegacyBaseLocale
     */
    fun getLocaleById(localeId: String): BaseLocale {
        return locales.getOrDefault(localeId, locales[Constants.DEFAULT_LOCALE_ID]!!)
    }

    /**
     * Gets the BaseLocale from the ID, if the locale doesn't exist, the default locale ("default") will be retrieved
     *
     * @param localeId the ID of the locale
     * @return         the locale on BaseLocale format or, if the locale doesn't exist, the default locale will be loaded
     * @see            LegacyBaseLocale
     */
    fun getLegacyLocaleById(localeId: String): LegacyBaseLocale {
        return legacyLocales.getOrDefault(localeId, legacyLocales["default"]!!)
    }
}