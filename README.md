# mcPlugincode
# ManaPlugin - 我的世界法力值插件
 Minecraft 法力值系统，支持等级成长和武器技能消耗。

# 功能特性
初始法力值 20点
每升一级法力上限 +5点
升级自动回满法力
支持通过物品 Lore 设置技能法力消耗
数据自动保存，重载服务器不丢失

# 指令
 `/mana`  查看当前法力值  无 
 `/manareload`  重载配置文件 

# 配置文件 (config.yml)
```yaml
base-mana: 20        # 初始法力值
mana-per-level: 5    # 每级增加
