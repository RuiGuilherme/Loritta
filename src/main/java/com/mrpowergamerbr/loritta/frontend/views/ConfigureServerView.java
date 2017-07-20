package com.mrpowergamerbr.loritta.frontend.views;

import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.mrpowergamerbr.loritta.Loritta;
import com.mrpowergamerbr.loritta.LorittaLauncher;
import com.mrpowergamerbr.loritta.commands.CommandBase;
import com.mrpowergamerbr.loritta.commands.CommandOptions;
import com.mrpowergamerbr.loritta.commands.vanilla.fun.TristeRealidadeCommand;
import com.mrpowergamerbr.loritta.commands.vanilla.fun.TristeRealidadeCommand.TristeRealidadeCommandOptions;
import com.mrpowergamerbr.loritta.frontend.LorittaWebsite;
import com.mrpowergamerbr.loritta.frontend.utils.RenderContext;
import com.mrpowergamerbr.loritta.frontend.views.configure.*;
import com.mrpowergamerbr.loritta.userdata.JoinLeaveConfig;
import com.mrpowergamerbr.loritta.userdata.MusicConfig;
import com.mrpowergamerbr.loritta.userdata.ServerConfig;
import com.mrpowergamerbr.temmiediscordauth.TemmieDiscordAuth;
import com.mrpowergamerbr.temmiediscordauth.utils.TemmieGuild;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class ConfigureServerView {

    public static Object render(RenderContext context, TemmieDiscordAuth temmie, String guildId) {
        try {
            List<TemmieGuild> guilds = temmie.getUserGuilds();
            boolean allowed = false;
            for (TemmieGuild guild : guilds) {
                if (guild.getId().equals(guildId)) {
                    allowed = LorittaWebsite.canManageGuild(guild);
                    context.contextVars().put("currentServer", guild);

                    context.contextVars().put("currentServerJda", LorittaLauncher.getInstance().getLorittaShards().getGuildById(guild.getId()));
                    break;
                }
            }

            if (!allowed && temmie.getCurrentUserIdentification().getId().equals(Loritta.config.getOwnerId())) { // Ninguém viu nada...
                allowed = true;
            }

            context.contextVars().put("whereAmI", "idk");
            context.contextVars().put("autoGeneratedModals", Arrays.asList("<h1>Erro!</h1>"));

            if (allowed) {
                PebbleTemplate template = null;
                ServerConfig sc = LorittaLauncher.getInstance().getServerConfigForGuild(guildId);
                context.contextVars().put("serverConfig", sc);
                Map<CommandBase, CommandOptions> cmdOptions = new HashMap<>();

                for (CommandBase cmdBase : LorittaLauncher.getInstance().getCommandManager().getCommandMap()) {
                    CommandOptions cmdOpti = sc.getCommandOptionsFor(cmdBase);
                    cmdOptions.put(cmdBase, cmdOpti);
                }

                System.out.println("Website: " + cmdOptions.size());

                context.contextVars().put("cmdOptions", cmdOptions);

                if (context.request().path().endsWith("commands")) {
                    if (context.request().param("editingCommandOptions").isSet()) {
                        String val = context.request().param("editingCommandOptions").value();
                        for (CommandBase cmdBase : LorittaLauncher.getInstance().getCommandManager().getCommandMap()) {
                            if (cmdBase.getClass().getSimpleName().equals(val)) {
                                CommandOptions cmdOpti = sc.getCommandOptionsFor(cmdBase);

                                for (Field f : cmdOpti.getClass().getFields()) {
                                    try {
                                        if (context.request().param(f.getName()).isSet()) {
                                            f.setBoolean(cmdOpti, true);
                                        } else {
                                            f.setBoolean(cmdOpti, false);
                                        }
                                    } catch (IllegalArgumentException | IllegalAccessException e) {
                                        e.printStackTrace();
                                    }
                                }
                                sc.commandOptions().put(cmdBase.getClass().getSimpleName(), cmdOpti);
                                break;
                            }
                        }
                    }
                    if (context.request().param("editingCmds").isSet()) {
                        ArrayList<String> enabledModules = new ArrayList<String>();
                        for (CommandBase cmdBase : LorittaLauncher.getInstance().getCommandManager().getCommandMap()) {
                            if (!context.request().param(cmdBase.getClass().getSimpleName()).isSet()) {
                                enabledModules.add(cmdBase.getClass().getSimpleName());
                            }
                        }
                        sc.disabledCommands(enabledModules);
                    }
                    if (context.request().param("editingTristeRealidade").isSet()) {
                        TristeRealidadeCommandOptions cmdOpti = new TristeRealidadeCommand.TristeRealidadeCommandOptions();
                        cmdOpti.setMentionEveryone(context.request().param("mentionEveryone").isSet());
                        cmdOpti.setHideDiscordTags(context.request().param("hideDiscordTags").isSet());
                        sc.commandOptions().put("TristeRealidadeCommand", cmdOpti);
                    }
                    if (context.request().param("activateAllCommands").isSet()) {
                        ArrayList<String> enabledModules = new ArrayList<String>();
                        sc.disabledCommands(enabledModules);
                    }
                    if (context.request().param("deactivateAllCommands").isSet()) {
                        ArrayList<String> enabledModules = new ArrayList<String>();
                        for (CommandBase cmdBase : LorittaLauncher.getInstance().getCommandManager().getCommandMap()) {
                            enabledModules.add(cmdBase.getClass().getSimpleName());
                        }
                        sc.disabledCommands(enabledModules);
                    }
                    LorittaLauncher.getInstance().getDs().save(sc);
                    for (CommandBase cmdBase : LorittaLauncher.getInstance().getCommandManager().getCommandMap()) {
                        context.contextVars().put("commandOption" + cmdBase.getClass().getSimpleName(), new CommandOptions());
                    }
                    for (Entry<String, CommandOptions> entry : sc.commandOptions().entrySet()) {
                        context.contextVars().put("commandOption" + entry.getKey(), entry.getValue());
                    }

                    context.contextVars().put("whereAmI", "moduleConfig");

                    template = LorittaWebsite.getEngine().getTemplate("module_config.html");
                    context.contextVars().put("availableCmds", LorittaLauncher.getInstance().getCommandManager().getCommandMap());
                    context.contextVars().put("disabledCmds", LorittaLauncher.getInstance().getCommandManager().getCommandsDisabledIn(sc));
                } else if (context.request().path().endsWith("joinconfig")) {
                    if (context.request().param("canalJoin").isSet()) { // O usuário está salvando as configurações?
                        JoinLeaveConfig jlCnf = sc.joinLeaveConfig();
                        jlCnf.setEnabled(context.request().param("enableModule").isSet());
                        jlCnf.setTellOnJoin(context.request().param("tellOnJoin").isSet());
                        jlCnf.setTellOnLeave(context.request().param("tellOnLeave").isSet());
                        jlCnf.setCanalJoinId(context.request().param("canalJoin").value());
                        jlCnf.setCanalLeaveId(context.request().param("canalLeave").value());
                        jlCnf.setJoinMessage(context.request().param("joinMessage").value());
                        jlCnf.setLeaveMessage(context.request().param("leaveMessage").value());
                        jlCnf.setTellOnPrivate(context.request().param("tellOnPrivate").isSet());
                        jlCnf.setJoinPrivateMessage(context.request().param("privateMessage").value());

                        sc.joinLeaveConfig(jlCnf);
                        LorittaLauncher.getInstance().getDs().save(sc);
                    }
                    context.contextVars().put("whereAmI", "joinConfig");

                    template = LorittaWebsite.getEngine().getTemplate("join_config.html");
                } else if (context.request().path().endsWith("music")) {
                    if (context.request().param("musicGuildId").isSet()) { // O usuário está salvando as configurações?
                        MusicConfig mscCnf = sc.musicConfig();
                        mscCnf.setEnabled(context.request().param("enableModule").isSet());
                        mscCnf.setMusicGuildId(context.request().param("musicGuildId").value());
                        mscCnf.setHasMaxSecondRestriction(context.request().param("maxSecEnabled").isSet());
                        mscCnf.setMaxSeconds(context.request().param("maxSec").intValue());
                        mscCnf.setAutoPlayWhenEmpty(context.request().param("autoPlayEnabled").isSet());
                        mscCnf.setUrls(Arrays.asList(context.request().param("musicUrls").value().split(";")));
                        mscCnf.setAllowPlaylists(context.request().param("allowPlaylists").isSet());

                        sc.musicConfig(mscCnf);
                        LorittaLauncher.getInstance().getDs().save(sc);
                    }
                    context.contextVars().put("playlist", StringUtils.join(sc.musicConfig().getUrls(), ";"));
                    context.contextVars().put("whereAmI", "musicConfig");

                    template = LorittaWebsite.getEngine().getTemplate("music_config.html");
                } else if (context.request().path().endsWith("nashorn")) {
                    template = NashornCommandsView.render(context, temmie, sc);
                } else if (context.request().path().endsWith("amino")) {
                    template = AminoConfigView.render(context, temmie, sc);
                } else if (context.request().path().endsWith("youtube")) {
                    template = YouTubeConfigView.render(context, temmie, sc);
                } else if (context.request().path().endsWith("starboard")) {
                    template = StarboardConfigView.render(context, temmie, sc);
                } else if (context.request().path().endsWith("rss")) {
                    template = RssFeedsConfigView.render(context, temmie, sc);
                } else if (context.request().path().endsWith("autorole")) {
                    template = AutoroleConfigView.render(context, temmie, sc);
                }  else if (context.request().path().endsWith("eventlog")) {
                    template = EventLogConfigView.render(context, temmie, sc);
                } else {
                    if (context.request().param("commandPrefix").isSet()) {
                        sc.commandPrefix(context.request().param("commandPrefix").value());
                        LorittaLauncher.getInstance().getDs().save(sc);
                    }

                    if (context.request().param("commandMagic").isSet()) {
                        sc.explainOnCommandRun(context.request().param("explainOnCommandRun").isSet());
                        sc.mentionOnCommandOutput(context.request().param("mentionOnCommandOutput").isSet());
                        sc.debugOptions().enableAllModules(context.request().param("enableAllModules").isSet());
                        sc.warnOnMissingPermission(context.request().param("warnOnMissingPermission").isSet());
                        LorittaLauncher.getInstance().getDs().save(sc);
                    }

                    context.contextVars().put("whereAmI", "mainPage");

                    context.contextVars().put("commandPrefix", sc.commandPrefix());
                    template = LorittaWebsite.getEngine().getTemplate("server_config.html");
                }

                return template;
            } else {
                try {
                    context.response().redirect(LorittaWebsite.getWebsiteUrl());
                } catch (Throwable e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (PebbleException e) {
            // TODO Auto-generated catch block
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return e.toString();
        }
        return null;
    }
}