#!/bin/bash

# 更新防弹头盔配方
cat > bulletproof_helmet_ii.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_helmet_i",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_helmet_ii",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_helmet_iii.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_helmet_ii",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_helmet_iii",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_helmet_iv.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_helmet_iii",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_helmet_iv",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_helmet_v.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_helmet_iv",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_helmet_v",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_helmet_vi.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_helmet_v",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_helmet_vi",
    "count": 1
  }
}
INNEREOF

# 更新防弹背心配方
cat > bulletproof_vest_ii.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_vest_i",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_vest_ii",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_vest_iii.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_vest_ii",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_vest_iii",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_vest_iv.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_vest_iii",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_vest_iv",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_vest_v.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_vest_iv",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_vest_v",
    "count": 1
  }
}
INNEREOF

cat > bulletproof_vest_vi.json << 'INNEREOF'
{
  "type": "minecraft:crafting_shapeless",
  "category": "equipment",
  "ingredients": [
    "jeg:bulletproof_vest_v",
    "minecraft:iron_ingot",
    "minecraft:iron_ingot"
  ],
  "result": {
    "id": "jeg:bulletproof_vest_vi",
    "count": 1
  }
}
INNEREOF

echo "防弹护具配方更新完成"
