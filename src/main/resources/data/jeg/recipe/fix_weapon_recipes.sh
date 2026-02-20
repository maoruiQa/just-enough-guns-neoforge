#!/bin/bash

# 武器配方 - 使用shaped格式
cat > burst_rifle.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "III",
    "GRG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:burst_rifle",
    "count": 1
  }
}
INNEREOF

cat > double_barrel_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "III",
    "GSG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:double_barrel_shotgun",
    "count": 1
  }
}
INNEREOF

cat > grenade_launcher.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "IBI",
    "GRG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:grenade_launcher",
    "count": 1
  }
}
INNEREOF

cat > hypersonic_cannon.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "DBD",
    "GRG",
    " S "
  ],
  "key": {
    "D": "minecraft:diamond",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:hypersonic_cannon",
    "count": 1
  }
}
INNEREOF

cat > pump_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "III",
    "GRG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:pump_shotgun",
    "count": 1
  }
}
INNEREOF

cat > repeating_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "III",
    "GRG",
    " S "
  ],
  "key": {
    "I": "minecraft:iron_ingot",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:repeating_shotgun",
    "count": 1
  }
}
INNEREOF

cat > rocket_launcher.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "DBD",
    "GRG",
    " S "
  ],
  "key": {
    "D": "minecraft:diamond",
    "B": "minecraft:iron_block",
    "G": "minecraft:gunpowder",
    "R": "minecraft:redstone",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "jeg:rocket_launcher",
    "count": 1
  }
}
INNEREOF

cat > waterpipe_shotgun.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "I I",
    "GSG",
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

echo "修复完成所有武器配方"
