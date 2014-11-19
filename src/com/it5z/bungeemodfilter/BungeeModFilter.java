package com.it5z.bungeemodfilter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Administrator on 2014/11/16.
 */
public class BungeeModFilter extends Plugin implements Listener {
    private Logger logger;
    private Configuration configuration;
    private PluginManager pluginmanager;

    @Override
    public void onEnable() {
        logger = this.getLogger();
        pluginmanager = this.getProxy().getPluginManager();
        superConfig();
        superCommand();
        pluginmanager.registerListener(this, this);
        logger.info(ChatColor.GREEN + "BungeeModFilter已被加载!");
    }

    @Override
    public void onDisable() {
        saveConfig();
        logger.info(ChatColor.GREEN + "BungeeModFilter已被卸载!");
    }

    private void superConfig() {
        File datafolder = this.getDataFolder();
        if (datafolder.exists() || datafolder.mkdir()) {
            File configfile = new File(datafolder, "config.yml");
            try {
                if(!configfile.exists()) {
                    Files.copy(this.getResourceAsStream("config.yml"), configfile.toPath());
                }
                configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configfile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveConfig() {
        if(configuration != null) {
            try {
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, new File(getDataFolder(), "config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void superCommand() {
        pluginmanager.registerCommand(this, new Command("bungeemodfilter", "bungeemodfilter.command", "bmf") {
            @SuppressWarnings("deprecation")
            @Override
            public void execute(CommandSender sender, String[] args) {
                if(args.length > 0) {
                    if(args[0].equalsIgnoreCase("help")) {
                        sender.sendMessage(ChatColor.AQUA + "=====BungeeModFilter帮助=====");
                        sender.sendMessage(ChatColor.GREEN + "/bungeemodfilter help\t查看帮助");
                        sender.sendMessage(ChatColor.GREEN + "/bungeemodfilter mods <Player>\t查看模组");
                        sender.sendMessage(ChatColor.GREEN + "/bungeemodfilter reload\t重载配置");
                    } else if(args[0].equalsIgnoreCase("mods")) {
                        if(args.length == 2) {
                            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[1]);
                            if(player != null) {
                                Map<String, String> modlist = player.getModList();
                                if(!modlist.isEmpty()) {
                                    Set<String> mods = modlist.keySet();
                                    sender.sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.RESET + " <" + mods.size() + ">: " + ChatColor.GREEN + mods);
                                } else {
                                    sender.sendMessage(ChatColor.GREEN + "玩家没有安装模组或所在服务器无FML.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "玩家不在线!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "错误的参数,输入\"/bungeemodfilter help\"查看帮助.");
                        }
                    }else if(args[0].equalsIgnoreCase("reload")) {
                        superConfig();
                        sender.sendMessage(ChatColor.GREEN + "重载配置!");
                    } else {
                        sender.sendMessage(ChatColor.RED + "不存在的命令,输入\"/bungeemodfilter help\"查看帮助.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "输入\"/bungeemodfilter help\"查看帮助.");
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void Connected(ServerConnectedEvent event) {
        if(configuration == null) return;
        ProxiedPlayer player = event.getPlayer();
        if(!player.hasPermission("bungeemodfilter.ignore")) {
            Map<String, String> modlist = player.getModList();
            if(!modlist.isEmpty()) {
                List<String> blacklist = configuration.getStringList("Blacklist");
                Set<String> mods = modlist.keySet();
                blacklist.retainAll(mods);
                if(!blacklist.isEmpty()) {
                    player.disconnect(configuration.getString("DisconnectMessage", "包含非法的模组 <%amount%>: %mods%").replace("%amount%", String.valueOf(blacklist.size())).replace("%mods%", String.valueOf(blacklist)));
                }
            }
        }
    }
}
