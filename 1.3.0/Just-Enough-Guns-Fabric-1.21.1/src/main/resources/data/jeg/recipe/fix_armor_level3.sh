#!/bin/bash

# 修正三级防弹护具配方 - 直接用铁装备合成

# 三级防弹头盔 - 用铁头盔而不是二级防弹头盔
cat > bulletproof_helmet_iii.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "minecraft:iron_helmet",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_helmet_iii",
    "count": 1
  }
}
INNEREOF

# 三级防弹背心 - 用铁胸甲而不是二级防弹背心
cat > bulletproof_vest_iii.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "minecraft:iron_chestplate",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_vest_iii",
    "count": 1
  }
}
INNEREOF

echo "三级防弹护���配方修正完成 - 直接用铁装备合成"
