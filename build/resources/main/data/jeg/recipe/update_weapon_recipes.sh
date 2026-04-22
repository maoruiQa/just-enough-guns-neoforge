#!/bin/bash

# 更新武器配方，根据威力等级调整成本

# burst_rifle - 中级武器 (7.5伤害，30发) - 增加成本
cat > burst_rifle.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "III",
    "GRG",
    " SS"
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone_block",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:burst_rifle",
    "count": 1
  }
}
INNEREOF

# double_barrel_shotgun - 高威力霰弹枪 (22×22=484总伤害) - 显著增加成本
cat > double_barrel_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "IBI",
    "GGG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:double_barrel_shotgun",
    "count": 1
  }
}
INNEREOF

# grenade_launcher - 区域效果武器 (爆炸伤害) - 保持合理成本
cat > grenade_launcher.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "IBI",
    "GTG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "T": "minecraft:tnt",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:grenade_launcher",
    "count": 1
  }
}
INNEREOF

# hypersonic_cannon - 重型武器 - 高成本
cat > hypersonic_cannon.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "DBD",
    "GTG",
    " S "
  ],
  "key": {
    "D": "minecraft:diamond",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "T": "minecraft:tnt",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:hypersonic_cannon",
    "count": 1
  }
}
INNEREOF

# pump_shotgun - 高威力霰弹枪 (18×12=216总伤害) - 增加成本
cat > pump_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "III",
    "GGG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:pump_shotgun",
    "count": 1
  }
}
INNEREOF

# repeating_shotgun - 高射速霰弹枪 - 中高成本
cat > repeating_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "IBI",
    "GGG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:repeating_shotgun",
    "count": 1
  }
}
INNEREOF

# rocket_launcher - 超重型武器 - 极高成本
cat > rocket_launcher.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "DBD",
    "GTG",
    " S "
  ],
  "key": {
    "D": "minecraft:diamond_block",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "T": "minecraft:tnt",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:rocket_launcher",
    "count": 1
  }
}
INNEREOF

# waterpipe_shotgun - 基础霰弹枪 - 低成本
cat > waterpipe_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "I I",
    " G ",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:waterpipe_shotgun",
    "count": 1
  }
}
INNEREOF

echo "武器配方更新完成，已根据威力等级调整成本"
