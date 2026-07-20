package com.example.manaplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends JavaPlugin implements Listener {

    private final Map<UUID, ManaData> manaData = new HashMap<>();
    private int baseMana = 20;
    private int manaPerLevel = 5;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("mana").setExecutor(this);
        getCommand("manareload").setExecutor(this);
        
        // 加载在线玩家数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }
        
        getLogger().info("§a法力值插件已加载！");
    }

    @Override
    public void onDisable() {
        // 保存所有玩家数据
        for (Map.Entry<UUID, ManaData> entry : manaData.entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue());
        }
        getLogger().info("§c法力值插件已卸载！");
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        baseMana = config.getInt("base-mana", 20);
        manaPerLevel = config.getInt("mana-per-level", 5);
    }

    private void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid.toString();
        FileConfiguration config = getConfig();
        
        int currentMana = config.getInt(path + ".mana", baseMana + (player.getLevel() * manaPerLevel));
        int maxMana = baseMana + (player.getLevel() * manaPerLevel);
        
        manaData.put(uuid, new ManaData(currentMana, maxMana));
    }

    private void savePlayerData(UUID uuid, ManaData data) {
        String path = "players." + uuid.toString();
        getConfig().set(path + ".mana", data.getCurrentMana());
        saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (manaData.containsKey(uuid)) {
            ManaData data = manaData.get(uuid);
            int newMax = baseMana + (player.getLevel() * manaPerLevel);
            data.setMaxMana(newMax);
            // 升级时补满法力值
            data.setCurrentMana(newMax);
            player.sendMessage(ChatColor.AQUA + "✦ 升级！法力值上限提升至 " + newMax + "，已回满！");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (command.getName().equalsIgnoreCase("manareload")) {
            if (!sender.hasPermission("mana.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限！");
                return true;
            }
            reloadConfig();
            loadConfig();
            sender.sendMessage(ChatColor.GREEN + "✓ 配置文件已重载！");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该指令仅限玩家使用！");
            return true;
        }

        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("mana")) {
            UUID uuid = player.getUniqueId();
            ManaData data = manaData.get(uuid);
            
            if (data == null) {
                player.sendMessage(ChatColor.RED + "数据加载失败！");
                return true;
            }
            
            player.sendMessage(ChatColor.AQUA + "✦ 法力值: " + 
                              ChatColor.WHITE + data.getCurrentMana() + 
                              ChatColor.GRAY + "/" + 
                              ChatColor.WHITE + data.getMaxMana());
            
            // 显示法力条（10格）
            int barLength = 10;
            int filled = (int) ((double) data.getCurrentMana() / data.getMaxMana() * barLength);
            StringBuilder bar = new StringBuilder("§7[");
            for (int i = 0; i < barLength; i++) {
                bar.append(i < filled ? "§b■" : "§7■");
            }
            bar.append("§7]");
            player.sendMessage(bar.toString());
            return true;
        }
        
        return false;
    }

    /**
     * 检查玩家法力值是否足够，并扣除
     * @param player 玩家
     * @param cost 消耗量
     * @return true=消耗成功, false=法力不足
     */
    public boolean consumeMana(Player player, int cost) {
        UUID uuid = player.getUniqueId();
        ManaData data = manaData.get(uuid);
        
        if (data == null) return false;
        if (data.getCurrentMana() < cost) return false;
        
        data.setCurrentMana(data.getCurrentMana() - cost);
        return true;
    }

    /**
     * 获取玩家当前法力值
     */
    public int getCurrentMana(Player player) {
        UUID uuid = player.getUniqueId();
        ManaData data = manaData.get(uuid);
        return data != null ? data.getCurrentMana() : 0;
    }

    /**
     * 获取玩家最大法力值
     */
    public int getMaxMana(Player player) {
        UUID uuid = player.getUniqueId();
        ManaData data = manaData.get(uuid);
        return data != null ? data.getMaxMana() : 0;
    }

    /**
     * 从物品 Lore 中提取法力消耗值
     * @param item 物品
     * @return 消耗值，如果没有则返回 0
     */
    public int getManaCostFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        if (!item.hasItemMeta()) return 0;
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;
        
        Pattern pattern = Pattern.compile("法力消耗[:：]\\s*(\\d+)");
        for (String line : meta.getLore()) {
            Matcher matcher = pattern.matcher(ChatColor.stripColor(line));
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
