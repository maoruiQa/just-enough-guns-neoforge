"""Generate/verify the combat guide using stdlib only. --all targets the six maintained siblings."""
from pathlib import Path
import argparse
import json
import re

VEHICLES = ('speedboat', 'bmp2', 'lav150', 'ah6', 'mi28')
FACTIONS = ('night_of_the_undead', 'the_rattlers', 'nosy_business', 'bad_piggies', 'hell_hogs', 'lost_souls')
EXCLUDED_GUNS = {'abstract_gun': 'Developer weapon, no survival acquisition', 'typhoonee': 'Disabled weapon'}
UNOBTAINABLE = {'phantom_smg': 'No recipe; excluded from gun loot; PhantomGunner main-hand drop chance is zero'}
TABS = {
    'training': ('Combat Training', '战斗训练', 'combat_pistol'),
    'arsenal': ('Know Your Arsenal', '武器实战', 'assault_rifle'),
    'battle': ('Campaign Service', '战术与战役', 'infantry_rifle'),
    'gunsmith': ('Build Your Loadout', '改装与个性化', 'reflex_sight'),
    'supply': ('Protection and Logistics', '防护与补给', 'bulletproof_vest_i'),
    'special': ('Special Operations', '特种作战', 'monitor'),
    'vehicles': ('Vehicle Warfare', '载具作战', 'vehicle_assembling_table'),
    'exploration': ('Beyond the Front Line', '探索与扩展挑战', 'phantom_smg'),
}
OPERATIONS = {
    'grenade': ('Throw a Grenade', '手雷实战', 'Prime and throw a grenade, then damage an enemy with its explosion. Next, practise shortening its fuse.', '拉开引信后投掷手雷，让爆炸实际伤害敌人；随后练习控制引信。'),
    'stun': ('Flash the Enemy', '闪光压制', 'Throw a stun grenade at an enemy and apply its flash effect. Distance and line of sight affect success.', '向敌人投掷闪光弹并实际施加闪光效果；距离和视线决定效果。'),
    'smoke': ('Deploy Smoke', '烟幕掩护', 'Throw a smoke grenade and let it release smoke. Next, use smoke to break a hostile missile lock.', '投掷烟雾弹并等待烟幕实际释放；随后尝试用烟幕打断敌方导弹跟踪。'),
    'molotov': ('Burning Area Denial', '燃烧阻滞', 'Throw a Molotov at an enemy and cause actual fire damage. Ignition without damage does not count.', '投掷燃烧瓶，让敌人实际受到燃烧伤害；只点燃但未伤害不计入。'),
    'water_convert': ('Water Changes the Payload', '水下转化', 'Use a grenade while submerged to convert it into a water bomb. Next, extinguish a fire with it.', '在水下使用手雷将其转化为水弹；随后用水弹扑灭火焰。'),
    'water_use': ('Firefighting', '扑灭火焰', 'Throw a water bomb at a burning entity or fire block and actually extinguish it.', '向燃烧的实体或火焰方块投掷水弹，并实际将其扑灭。'),
    'place_c4': ('Place C4', '布设C4', 'Place C4 on a valid surface. Set timed or remote mode before placement; then test the corresponding detonation.', '在有效表面放置C4；放置前选择定时或遥控模式，然后完成对应引爆。'),
    'c4_remote': ('Remote Demolition', '遥控爆破', 'Bind placed C4 to your detonator and successfully detonate it within range. Empty or failed requests do not count.', '将已放置C4绑定到起爆器，在有效范围内实际遥控引爆；空请求或失败不计入。'),
    'c4_effect': ('C4 in Combat', 'C4实战', 'Damage an enemy with C4 you placed. Credit follows the explosive owner after switching items.', '用自己布设的C4实际伤害敌人；切换手持物品不会改变爆炸归属。'),
    'place_claymore': ('Directional Mine', '布设定向雷', 'Place a claymore on a valid surface facing an approach. Next, let an enemy enter its trigger zone.', '在有效表面朝向敌人来路布设定向雷；随后让敌人进入触发区。'),
    'claymore_effect': ('Ambush Triggered', '定向伏击', 'Cause actual enemy damage with your claymore. Placement alone does not count.', '让自己布设的定向雷实际伤害敌人；仅放置不计入。'),
    'place_tm62': ('Anti-vehicle Minefield', '布设反载具雷', 'Place a TM-62 on a valid surface. Heavy entities and vehicles can trigger it; small mobs may not.', '在有效表面放置TM-62；重型实体和载具能够压发，小型生物未必触发。'),
    'tm62_effect': ('Minefield Contact', '雷区接敌', 'Damage an enemy using your TM-62. Use an enemy vehicle or a sufficiently heavy hostile target.', '用自己布设的TM-62伤害敌人；选择敌方载具或足够重的敌对目标。'),
    'defuse': ('Clear the Minefield', '排除爆炸物', 'Hold the defuser on a placed explosive within reach and sight until defusing finishes. Interrupting cancels the operation.', '在距离和视线允许时，持续用拆弹器操作已放置爆炸物至完成；中断操作会取消拆弹。'),
    'drone_deploy': ('Launch a Drone', '部署无人机', 'Deploy a drone in clear space. Then bind a monitor before controlling it.', '在空旷位置部署无人机，然后绑定监视器以取得控制。'),
    'drone_bind': ('Link the Monitor', '绑定监视器', 'Use a monitor on a reachable drone to bind it. Binding to a missing drone does not count.', '对可触及的无人机使用监视器完成绑定；不存在的目标不会完成。'),
    'drone_control': ('Remote Pilot', '遥控无人机', 'Activate a bound monitor and successfully enter drone control. The link must satisfy range and availability checks.', '使用已绑定监视器实际进入无人机控制；须满足连接距离和可用条件。'),
    'drone_interact': ('Reach from Above', '空中互动', 'While controlling a drone, successfully interact with a block or attack an enemy through the monitor.', '控制无人机时，通过监视器成功操作方块或攻击敌人。'),
    'drone_unload': ('Recover the Payload', '卸下载荷', 'Remove a loaded payload from the drone and receive the returned item. An empty drone cannot satisfy this.', '从无人机实际卸下载荷并取回物品；空载无人机不计入。'),
    'drone_pack': ('Bring It Home', '回收无人机', 'Use a crowbar on an accessible drone to recover it. Clear or unload its payload first as required.', '对可触及的无人机使用撬棍完成回收；按提示先处理或卸下载荷。'),
    'javelin_lock': ('Acquire a Ground Target', '锁定地面目标', 'Aim Javelin at a valid large target with clear sight until lock completes. Ordinary small mobs cannot be locked.', '用Javelin瞄准视线内符合条件的大型目标，保持瞄准至锁定；普通小型生物无法锁定。'),
    'javelin_direct': ('Direct Attack', 'Javelin直射', 'Select direct mode, lock an enemy ground target and deal actual missile damage. Carry Javelin ammunition.', '选择直射模式，锁定敌方地面目标并以导弹造成有效伤害；携带Javelin弹药。'),
    'javelin_top': ('Attack from Above', 'Javelin攻顶', 'Select top-attack mode, lock an enemy and hit it with the guided missile. Leave space for the lofted trajectory.', '选择攻顶模式，锁定敌人并以制导导弹有效命中；为爬升弹道留出空间。'),
    'igla_hit': ('Clear the Sky', 'Igla防空', 'Lock an eligible airborne enemy with Igla, then deal actual guided missile damage. Ground targets do not satisfy air guidance.', '用Igla锁定符合条件的空中敌人，再以制导导弹造成有效伤害；地面目标不满足防空制导条件。'),
}
BATTLE_INSTRUCTIONS = {
    'headshot': ('Hit an enemy head with a gun projectile. Aim at the head hit region; firing into the air does not count.', '用枪弹实际命中敌人头部区域；空射不计入。'),
    'melee': ('Deal actual melee damage to an enemy using the gun melee action. Next, fit a compatible bayonet.', '使用枪械近战操作实际伤害敌人，然后安装兼容刺刀。'),
    'bayonet': ('Install a compatible sword in the barrel slot and strike an enemy with gun melee.', '在枪管槽位安装兼容剑，再用枪械近战实际伤害敌人。'),
    'charge': ('With a bayonet fitted, sprint and use the bayonet charge to damage an enemy.', '安装刺刀后冲刺，使用刺刀冲锋实际伤害敌人。'),
    'patrol': ('Defeat a faction patrol and earn Faction Omen. Patrol spawning must be enabled; next, return near your respawn point.', '清剿派系巡逻并获得派系不祥之兆；需启用巡逻生成。然后返回重生点附近。'),
    'home_raid': ('Return near your respawn point while carrying Faction Omen and successfully start a faction raid.', '携带派系不祥之兆返回重生点附近，实际触发派系袭击。'),
    'raid': ('Defeat every raid wave and qualify for victory under the existing participation rules. Stay in the battle area and contribute.', '击退全部袭击波次，并满足现有战役参与资格；留在战区并参与战斗。'),
}

def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def generate(module):
    resource = module / 'src/main/resources'
    java = module / 'src/main/java/ttv/migami/jeg'
    modern = '1.21.1' not in module.name
    languages = {locale: read_json(resource / f'assets/jeg/lang/{locale}.json') for locale in ('en_us', 'zh_cn')}
    nodes, manifest = {}, []
    core = []

    def name(item, locale):
        return languages[locale].get('item.jeg.' + item, languages[locale].get('entity.jeg.' + item, languages[locale].get('faction.jeg.' + item, item.replace('_', ' ').title())))

    def add(path, icon, en, zh, en_desc, zh_desc, criteria, parent=None, xp=0, optional=False, frame='task'):
        if modern:
            for criterion in criteria.values():
                conditions = criterion.get('conditions', {})
                if '26.3' in module.name and 'recipe_id' in conditions:
                    conditions['recipes'] = conditions.pop('recipe_id')
                if 'player' in conditions:
                    predicate = conditions['player']
                    predicate = {('minecraft:type_specific/player' if key == 'type_specific' else 'minecraft:' + key): ({k: v for k, v in value.items() if k != 'type'} if key == 'type_specific' else value) for key, value in predicate.items()}
                    conditions['player'] = {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': predicate} if '26.3' in module.name else predicate
        key = 'advancement.jeg.guide.' + path.replace('/', '.')
        for locale, title, desc in [('en_us', en, en_desc), ('zh_cn', zh, zh_desc)]:
            languages[locale][key + '.title'] = title
            languages[locale][key + '.description'] = desc
        node = {
            'display': {'icon': {'id': icon if ':' in icon else 'jeg:' + icon}, 'title': {'translate': key + '.title'},
                        'description': {'translate': key + '.description'}, 'frame': frame, 'show_toast': not path.endswith('/root'),
                        'announce_to_chat': frame == 'challenge', 'hidden': False},
            'criteria': criteria, 'requirements': [[criterion] for criterion in criteria], 'sends_telemetry_event': False,
        }
        if parent: node['parent'] = 'jeg:guide/' + parent
        else: node['display']['background'] = 'minecraft:textures/gui/advancements/backgrounds/adventure.png' if not modern else 'minecraft:gui/advancements/backgrounds/adventure'
        if xp: node['rewards'] = {'experience': xp}
        nodes['guide/' + path] = node
        manifest.append({'id': 'jeg:guide/' + path, 'optional': optional, 'criteria': criteria, 'title': en})
        if parent and not optional and not path.endswith('/complete'): core.append('jeg:guide/' + path)
        return path

    def action(path, icon, en, zh, en_desc, zh_desc, event, subject=None, parent=None, xp=5, optional=False, extra=None):
        conditions = {'action': event}
        if subject is not None: conditions['subject'] = subject
        if extra: conditions.update(extra)
        return add(path, icon, en, zh, en_desc, zh_desc, {'done': {'trigger': 'jeg:gameplay_action', 'conditions': conditions}}, parent or path.split('/')[0] + '/root', xp, optional)

    def inventory(path, item, parent=None, optional=False):
        return add(path, item, 'Obtain ' + name(item, 'en_us'), '获得' + name(item, 'zh_cn'),
                   'Craft or obtain this item in survival. Follow its use objective to learn its role in combat.',
                   '通过本版本的配方或生存掉落获取；随后完成使用目标，了解它在战斗中的作用。',
                   {'obtained': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': 'jeg:' + item}]}}},
                   parent or path.split('/')[0] + '/root', optional=optional)

    for tab, (en, zh, icon) in TABS.items():
        add(tab + '/root', icon, en, zh,
            'A survival field guide. Controls refer to your current key bindings. Optional challenges do not block mastery.',
            '生存战地指南；操作以当前按键绑定为准。标为扩展的目标不阻塞全玩法精通。', {'start': {'trigger': 'minecraft:tick'}}, optional=tab == 'exploration')

    gun_source = (java / 'gun/GunDefinitions.java').read_text(encoding='utf-8')
    guns = sorted(set(re.findall(r'Reference.MOD_ID, "([^"]+)"\), new GunStats', gun_source)) - set(EXCLUDED_GUNS))
    guns += ['javelin', 'igla_9k38']
    gun_details = {}
    for line in gun_source.splitlines():
        match = re.search(r'Reference.MOD_ID, "([^\"]+)"\), new GunStats', line)
        if not match: continue
        detail = re.search(r'new GunStats\(.*?\), (Identifier\.fromNamespaceAndPath\("([^\"]+)", "([^\"]+)"\)|null), "([^\"]+)", (\d+)', line)
        if detail: gun_details[match[1]] = (detail[2], detail[3], detail[4], detail[5])
    categories = {}
    category_source = (java / 'gun/GunCategory.java').read_text(encoding='utf-8')
    for members, category in re.findall(r'case (.+?) -> (\w+);', category_source):
        for item in re.findall(r'"([^"]+)"', members): categories[item] = category.lower()
    for gun in guns:
        acquire = inventory('arsenal/obtain_' + gun, gun, optional=gun in UNOBTAINABLE)
        event = 'signal' if gun == 'flare_gun' else 'hit'
        action('arsenal/use_' + gun, gun, 'Field Test: ' + name(gun, 'en_us'), '实战：' + name(gun, 'zh_cn'),
               'Successfully activate the signal flare in survival.' if event == 'signal' else 'Deal actual damage to an enemy using this weapon. Ammunition, lock and reload requirements still apply; empty shots and friendly targets do not count.',
               '在生存中成功触发信号弹功能。' if event == 'signal' else '使用此武器对敌人造成有效伤害；需满足弹药、装填或锁定条件。空射和友方目标不计入。',
               event, 'jeg:' + gun, acquire, optional=gun in UNOBTAINABLE)
        if gun in UNOBTAINABLE:
            for path in (acquire, 'arsenal/use_' + gun):
                for locale, text in [('en_us', ' Extended content: no default survival acquisition. Requires a server-provided item; does not block mastery.'), ('zh_cn', ' 扩展内容：默认生存无获取途径；需要服务器提供物品，不阻塞总精通。')]:
                    languages[locale]['advancement.jeg.guide.' + path.replace('/', '.') + '.description'] += text
        if gun in gun_details:
            namespace, ammo_id, reload_type, capacity = gun_details[gun]
            mode = {'jeg:mag_fed': ('Use a compatible loaded magazine and finish the swap.', '使用兼容的已装弹弹匣并完成换弹。'),
                    'jeg:manual': ('Load rounds individually and finish the loading animation.', '逐发装入弹药并完成装填动画。'),
                    'jeg:inventory_fed': ('Keep compatible ammunition in your inventory; this weapon feeds directly.', '在背包保留兼容弹药；此武器直接从库存供弹。')}.get(reload_type, ('Follow the weapon reload prompt.', '按照武器提示完成装填。'))
            for locale, text in [('en_us', f' Ammunition: {name(ammo_id, "en_us") if namespace == "jeg" else ammo_id}; base capacity {capacity}. {mode[0]}'), ('zh_cn', f' 弹药：{name(ammo_id, "zh_cn") if namespace == "jeg" else ammo_id}；基础容量{capacity}。{mode[1]}')]:
                languages[locale]['advancement.jeg.guide.arsenal.use_' + gun + '.description'] += text
    for category in sorted(set(categories.get(gun, 'heavy') for gun in guns) - {'special'}):
        label = {'smg': '冲锋枪', 'pistol': '手枪', 'rifle': '步枪', 'sniper': '精确射击', 'lmg': '支援火力', 'shotgun': '霰弹枪', 'heavy': '重型武器'}[category]
        action('arsenal/category_' + category, 'combat_rifle', 'Combat Role: ' + category.title(), '战斗角色：' + label,
               'Defeat a hostile target with a weapon in this category. Credit follows the fired projectile, even after changing weapons.',
               '使用该类别武器击败敌对目标；归属跟随发射时的武器，换枪不会改变击杀归属。', 'category_kill', category, xp=25)

    lessons = [
        ('load', 'pistol_magazine', 'Load a Magazine', '手动装弹', 'magazine_load', None, 'Hold a compatible magazine and ammunition, then transfer a round into the magazine.', '按提示将兼容弹药实际装入弹匣；空弹匣不能用于作战。'),
        ('reload', 'combat_pistol', 'Ready to Fight', '完成换弹', 'reload', None, 'Finish a valid reload with ammunition available. Interrupted or failed reloads do not count.', '准备兼容弹药并完成一次装填；中断和失败装填不计入。'),
        ('aim', 'reflex_sight', 'Aim, Then Fire', '瞄准后开火', 'aimed_hit', None, 'Aim a gun and hit an enemy. Aim is recorded when the shot leaves the weapon.', '瞄准后有效命中敌人；瞄准状态以发射时为准。'),
        ('kill', 'combat_rifle', 'Defeat Your First Gunner', '击败首名枪手', 'first_gunner', None, 'Defeat an armed JEG gunner with credited server-side damage, then follow the campaign branch.', '击败一名JEG武装枪手并获得服务器确认的击杀归属，然后继续战役分支。'),
        ('attachment', 'reflex_sight', 'Fit for the Mission', '适合任务的改装', 'attachment_install', None, 'Install a compatible attachment in the attachment menu. Opening the menu is not installation.', '在配件界面实际安装兼容配件；只打开菜单不会完成。'),
        ('repair', 'repair_kit', 'Maintain Your Equipment', '维护装备', 'gun_repair', None, 'Repair a damaged gun with its repair material at an anvil.', '在铁砧使用维修材料修复一把受损枪械。'),
        ('cool', 'coolant', 'Keep It Running', '保持火力', 'cool', None, 'Use coolant on a heated compatible gun and finish cooling.', '对有热量的兼容武器完成冷却操作。'),
    ]
    previous = 'training/root'
    previous = action('training/recipes', 'combat_pistol', 'Read the Recipe Book', '查看配方',
                      'Open the crafting recipe book with a known JEG recipe, and inspect its ingredients. Recipes must be enabled on the server.',
                      '打开制作界面的配方书，查看已解锁的JEG配方及材料；服务器需启用对应配方。', 'recipe_view', parent=previous)
    for ident, icon, en, zh, event, subject, en_desc, zh_desc in lessons:
        previous = action('training/' + ident, icon, en, zh, en_desc, zh_desc, event, subject, previous)
    craftable = [gun for gun in guns if (resource / f'data/jeg/recipe/{gun}.json').exists()]
    first = add('training/first_weapon', 'combat_pistol', 'Equip for Combat', '制作首把武器', 'Craft any survival gun using its recipe.', '使用配方制作任意可生存获取的枪械。',
                {gun: {'trigger': 'minecraft:recipe_crafted', 'conditions': {'recipe_id': 'jeg:' + gun}} for gun in craftable}, 'training/root', 5)
    nodes['guide/' + first]['requirements'] = [craftable]
    ammo_crafts = [path.stem for path in (resource / 'data/jeg/recipe').glob('*.json') if path.stem in ('pistol_ammo', 'rifle_ammo', 'shotgun_shell', 'handmade_round', 'handmade_shell')]
    add('training/first_ammo', 'pistol_ammo', 'Make Your Ammunition', '制作首批弹药', 'Craft compatible ammunition in the crafting grid; the magazine determines which rounds it accepts.', '在合成栏制作兼容弹药；弹匣型号决定可装入的弹种。',
        {item: {'trigger': 'minecraft:recipe_crafted', 'conditions': {'recipe_id': 'jeg:' + item}} for item in ammo_crafts}, first, 5)
    nodes['guide/training/first_ammo']['requirements'] = [ammo_crafts]
    nodes['guide/training/first_weapon']['parent'] = 'jeg:guide/training/recipes'
    nodes['guide/training/load']['parent'] = 'jeg:guide/training/first_ammo'
    action('battle/range', 'bolt_action_rifle', 'Reach Beyond the Patrol', '远距命中', 'Deal actual gun damage to an enemy at least 50 blocks away. Use a long-range weapon and account for projectile flight.', '用远程枪械对至少50格外的敌人造成有效伤害，注意弹道飞行与武器射程。', 'range_hit')
    action('special/cook', 'grenade', 'Shorten the Fuse', '控制引信', 'Hold a grenade beyond its initial priming time, then successfully throw it with a shorter fuse. Holding until it explodes in your hand does not teach a controlled throw.', '拉开引信后继续握持一段时间，再成功投掷缩短引信的手雷；握持至手中爆炸不计入。', 'grenade_cook', 'jeg:grenade')
    for mode, en, zh in [('single', 'Single Shots', '单发射击'), ('automatic', 'Automatic Fire', '自动射击')]:
        action('arsenal/fire_' + mode, 'combat_rifle', en, zh,
               'Deal actual damage with a weapon using this firing mode. The current Burst Rifle uses automatic trigger behaviour; its own field test remains required.',
               '使用此发射模式的武器造成有效伤害；当前连发步枪使用自动扳机行为，仍需单独完成其武器实战目标。', 'fire_mode_hit', mode)
    for feed, en, zh in [('manual', 'Individual Loading', '逐发装填'), ('mag_fed', 'Magazine Feed', '弹匣供弹'), ('inventory_fed', 'Inventory Feed', '库存供弹')]:
        action('arsenal/feed_' + feed, 'rifle_ammo', en, zh,
               'Load compatible ammunition using this weapon feed system, then damage an enemy. Inventory-fed weapons draw rounds directly; the others need completed reloads.',
               '按此供弹方式准备兼容弹药并实际命中敌人；库存供弹直接消耗背包弹药，其余方式需完成装填。', 'feed_hit', 'jeg:' + feed)
        if feed != 'inventory_fed':
            action('arsenal/reload_' + feed, 'rifle_magazine', 'Complete Reload: ' + en, '完成装填：' + zh,
                   'Finish a successful reload of this type, with compatible ammunition or a loaded magazine. Interrupted loading does not count.',
                   '准备兼容弹药或已装弹弹匣，实际完成此方式的装填；中断操作不计入。', 'reload_type', 'jeg:' + feed)

    battle = [
        ('headshot', 'bolt_action_rifle', 'Headshot', '精准爆头', 'headshot', None),
        ('melee', 'minecraft:iron_sword', 'Close Quarters', '近距离交锋', 'melee_hit', None),
        ('bayonet', 'minecraft:iron_sword', 'Fix Bayonets', '刺刀出击', 'bayonet_hit', None),
        ('charge', 'minecraft:diamond_sword', 'Bayonet Charge', '刺刀冲锋', 'bayonet_charge', None),
        ('patrol', 'infantry_rifle', 'Break the Patrol', '清剿巡逻', 'patrol_win', None),
        ('home_raid', 'flare', 'Defend Your Home', '保卫家园', 'home_raid', None),
        ('raid', 'assault_rifle', 'Hold the Line', '守住阵地', 'raid_win', None),
    ]
    for ident, icon, en, zh, event, subject in battle:
        en_desc, zh_desc = BATTLE_INSTRUCTIONS[ident]
        action('battle/' + ident, icon, en, zh,
               en_desc, zh_desc, event, subject, xp=25)
    gunner_keys = ('skeleton', 'zombie', 'husk', 'zombifiedPiglin', 'piglin', 'witherSkeleton', 'drowned', 'zombieVillager', 'stray', 'pillager', 'vindicator', 'piglinBrute', 'generic') + (('parched',) if modern else ())
    for key in gunner_keys:
        entity = {'zombifiedPiglin': 'zombified_piglin', 'witherSkeleton': 'wither_skeleton', 'zombieVillager': 'zombie_villager', 'piglinBrute': 'piglin_brute'}.get(key, key)
        zh_label = {'skeleton': '骷髅', 'zombie': '僵尸（含食尸鬼）', 'husk': '尸壳', 'zombified_piglin': '僵尸猪灵', 'piglin': '猪灵', 'wither_skeleton': '凋灵骷髅', 'drowned': '溺尸', 'zombie_villager': '僵尸村民', 'stray': '流浪者', 'pillager': '掠夺者', 'vindicator': '卫道士', 'piglin_brute': '猪灵蛮兵', 'parched': '枯骸', 'generic': '扩展：自定义类型'}[entity]
        action('battle/gunner_' + key.lower(), 'combat_rifle', 'Gunner: ' + entity.replace('_', ' ').title(), '枪手：' + zh_label,
               'Defeat an armed gunner of this type. Ordinary unarmed mobs do not count.' + (' Requires a custom server faction; optional.' if key == 'generic' else ''),
               '击败此类型的武装枪手；普通未武装生物不计入。' + ('需要服务器自定义派系；不阻塞总精通。' if key == 'generic' else ''), 'gunner_kill', key, optional=key == 'generic', xp=100 if key == 'generic' else 5)
    for faction in FACTIONS:
        action('battle/faction_' + faction, 'infantry_rifle', 'Face ' + name(faction, 'en_us'), '迎战' + name(faction, 'zh_cn'),
               'Defeat a member of this default faction patrol. Custom server factions are not additional mastery requirements.',
               '击败该默认派系巡逻成员；服务器自定义派系不额外增加精通条件。', 'faction_kill', faction)
        action('exploration/raid_' + faction, 'flare', 'Extended: ' + name(faction, 'en_us'), '扩展：' + name(faction, 'zh_cn') + '袭击',
               'Win a raid against this faction. Requires the faction to be enabled; not required for mastery.', '赢得该派系袭击；需服务器启用该派系，不阻塞总完成。', 'raid_win', faction, optional=True, xp=100)
    for vehicle in VEHICLES[1:]:
        action('battle/destroy_' + vehicle, 'rocket_launcher', 'Defeat Enemy ' + name(vehicle, 'en_us'), '击败敌方' + name(vehicle, 'zh_cn'),
               'Destroy an enemy-controlled vehicle. Natural enemy vehicles begin appearing at the configured progression day (default day 80); your own vehicles do not count.',
               '击毁敌方控制的载具。自然敌方载具从配置的进度天数开始出现（默认第80天）；自己的载具不计入。', 'enemy_vehicle_kill', 'jeg:' + vehicle, xp=25)

    item_source = (java / 'init/ModItems.java').read_text(encoding='utf-8')
    attachments = sorted(set(re.findall(r'registerAttachment\(\s*"([^"]+)"', item_source)))
    slots = dict(re.findall(r'registerAttachment\(\s*"([^"]+)"\s*,\s*AttachmentType\.(\w+)', item_source))
    supported = dict(re.findall(r'support\("([^"]+)", (.*?)\)', (java / 'item/attachment/GunAttachmentRules.java').read_text(encoding='utf-8')))
    slot_labels = {'SCOPE': ('scope', '瞄具'), 'BARREL': ('barrel', '枪管'), 'STOCK': ('stock', '枪托'), 'UNDER_BARREL': ('under-barrel', '下挂'), 'MAGAZINE': ('magazine expansion', '弹匣扩展'), 'SPECIAL': ('special', '特殊'), 'PAINT_JOB': ('paint', '涂装'), 'KILL_EFFECT': ('kill effect', '击杀效果')}
    cosmetics = ['classic_spray_can', 'toy_spray_can', 'whiteout_spray_can', 'golden_spray_can', 'creeper_birthday_party_badge', 'headpoppper_badge', 'trickshot_badge']
    for item in attachments + cosmetics:
        config_limited = item in ('extended_mag', 'drum_mag')
        acquire = inventory('gunsmith/obtain_' + item, item, optional=config_limited)
        slot = slots.get(item, 'PAINT_JOB' if item.endswith('spray_can') else 'KILL_EFFECT')
        compatible = [gun for gun in guns if gun not in UNOBTAINABLE and (slot in ('PAINT_JOB', 'KILL_EFFECT') or 'AttachmentType.' + slot in supported.get(gun, ''))]
        example = compatible[0] if compatible else 'assault_rifle'
        en_slot, zh_slot = slot_labels[slot]
        action('gunsmith/install_' + item, item, 'Fit ' + name(item, 'en_us'), '安装' + name(item, 'zh_cn'),
               f'Install in the {en_slot} slot of a compatible gun, such as {name(example, "en_us")}. Binding curses remain enforced; next, use it in combat.',
               f'实际安装到兼容枪械的{zh_slot}槽，例如{name(example, "zh_cn")}；仍受绑定诅咒限制。然后完成实战使用。', 'attachment_install', 'jeg:' + item, acquire, optional=config_limited)
        if item in attachments:
            event = {'flashlight': 'flashlight_on', 'laser_pointer': 'laser_on'}.get(item, 'attachment_use')
            action('gunsmith/use_' + item, item, 'Field Use: ' + name(item, 'en_us'), '实际使用：' + name(item, 'zh_cn'),
                   'Use this installed attachment in combat: aim with a sight and hit an enemy; fire other gun attachments for an effective hit. Powered lights and lasers must actually operate.',
                   '让已安装配件实际生效：瞄具需瞄准命中敌人，其余枪械配件需参与有效命中；照明和激光需实际供电运行。', event, 'jeg:' + item, 'gunsmith/install_' + item, optional=config_limited, xp=100 if config_limited else 5)
        if config_limited:
            for prefix in ('obtain_', 'install_', 'use_'):
                path = 'gunsmith/' + prefix + item
                for locale, note in [('en_us', ' Extended: requires magazineFeed disabled. In the default mode use physical extended/drum magazines instead; excluded from mastery.'), ('zh_cn', ' 扩展：需关闭magazineFeed配置。默认模式改用实体加长弹匣或弹鼓；此配置目标不阻塞总精通。')]:
                    languages[locale]['advancement.jeg.guide.' + path.replace('/', '.') + '.description'] += note
    for item in cosmetics[-3:]:
        action('gunsmith/effect_' + item, item, 'Combat Effect: ' + name(item, 'en_us'), '击杀效果：' + name(item, 'zh_cn'),
               'With this badge installed, kill an enemy with a headshot to activate its effect.', '安装此徽章后，爆头击败敌人并实际触发效果。', 'kill_effect', 'jeg:' + item, 'gunsmith/install_' + item)
    for ident, item, event in [('light', 'flashlight', 'flashlight_on'), ('charge_light', 'flashlight', 'flashlight_charge'), ('laser', 'laser_pointer', 'laser_on'), ('kill_effect', 'headpoppper_badge', 'kill_effect')]:
        action('gunsmith/' + ident, item, 'Use ' + name(item, 'en_us'), '发挥作用：' + name(item, 'zh_cn'),
               'Successfully activate this attachment or its combat effect. A flat battery or disabled feature does not count.',
               '实际启用该配件或触发战斗效果；电量耗尽或功能关闭时不计入。', event)
    action('gunsmith/dye', 'minecraft:red_dye', 'Personal Colours', '个性配色', 'Apply a dye to a compatible gun through its cosmetic slot.', '通过外观槽位给兼容枪械实际染色。', 'gun_dye')
    add('gunsmith/enchant', 'minecraft:enchanted_book', 'Enchanted Arsenal', '附魔军械', 'Enchant a gun using the enchanting table.', '在附魔台给枪械附魔。',
        {'enchanted': {'trigger': 'minecraft:enchanted_item', 'conditions': {'item': {'items': ['jeg:' + gun for gun in guns]}}}}, 'gunsmith/root', 5)

    magazines = sorted(set(re.findall(r'new MagazineItem\(baseProperties\(Reference.id\("([^"]+)"', item_source)))
    for magazine in magazines:
        acquire = inventory('supply/obtain_' + magazine, magazine)
        action('supply/load_' + magazine, magazine, 'Load ' + name(magazine, 'en_us'), '装填' + name(magazine, 'zh_cn'),
               'Load compatible ammunition into this magazine. Extended magazines and drums have their own capacities.',
               '给此弹匣装入兼容弹药；加长弹匣与弹鼓拥有各自容量。', 'magazine_load', 'jeg:' + magazine, acquire)
        action('supply/use_' + magazine, magazine, 'Reload with ' + name(magazine, 'en_us'), '实用换弹：' + name(magazine, 'zh_cn'),
               'Complete a gun reload using this magazine variant.', '使用此型号弹匣完成枪械换弹。', 'magazine_used', 'jeg:' + magazine, 'supply/load_' + magazine)
    action('supply/unload', 'rifle_magazine', 'Recover Your Ammunition', '回收弹药', 'Transfer loaded rounds from a magazine back to your offhand.', '将弹匣中的弹药实际退回副手。', 'magazine_unload')
    inventory('supply/loader', 'magazine_loader')
    action('supply/automate', 'magazine_loader', 'Automated Supply', '自动装弹', 'Collect a magazine from the loader after it has loaded ammunition. Hopper output remains supported.', '从装弹机取出实际装有弹药的产物；仍支持漏斗自动输出。', 'loader_output')
    ammo = sorted(set(re.findall(r'"([a-z0-9_]+)"', item_source.split('AMMO_IDS = Set.of(')[1].split(');')[0])))
    for item in ammo + ['missile_engine', 'repair_kit', 'repair_tool', 'coolant', 'enhanced_coolant']:
        inventory('supply/obtain_' + item, item)
    action('supply/enhanced_cooling', 'enhanced_coolant', 'Enhanced Cooling', '增强冷却', 'Finish enhanced cooling on a heated compatible gun.', '对有热量的兼容武器完成增强冷却。', 'cool', 'jeg:enhanced_coolant')
    action('supply/normal_cooling', 'coolant', 'Field Cooling', '常规冷却', 'Finish cooling a heated compatible gun with ordinary coolant.', '用普通冷却剂完成有热量枪械的冷却操作。', 'cool', 'jeg:coolant')
    for tier in ('i', 'ii', 'iii', 'iv', 'v', 'vi'):
        for kind in ('helmet', 'vest'):
            item = 'bulletproof_' + kind + '_' + tier
            acquire = inventory('supply/obtain_' + item, item)
            action('supply/equip_' + item, item, 'Wear ' + name(item, 'en_us'), '装备' + name(item, 'zh_cn'),
                   'Equip this protection in its armour slot. Ballistic rating and penetration determine protection.', '穿戴该护具；弹道等级与穿甲值共同决定防护效果。', 'equip', 'jeg:' + item, acquire)
    action('supply/intercept', 'bulletproof_vest_vi', 'Armour Saves Lives', '护甲救命', 'Have worn ballistic armour reduce an incoming gun hit.', '让穿戴的弹道护甲实际降低一次枪弹伤害。', 'armour_intercept', xp=25)
    if modern:
        for tier in ('', '_diamond', '_netherite'):
            items = [path.stem for path in (resource / 'data/jeg/recipe').glob('armored_joy_harness_*.json') if path.stem.endswith(tier) and (tier or not path.stem.endswith(('_diamond', '_netherite')))]
            add('supply/harness' + tier, 'armored_joy_harness_white' + tier, 'Armoured Harness' + tier.replace('_', ' '), {'': '装甲悦灵挽具', '_diamond': '钻石装甲悦灵挽具', '_netherite': '下界合金装甲悦灵挽具'}[tier],
                'Equip a happy ghast with any colour of this harness tier. Colours do not change its combat role.', '给悦灵装备此等级任意颜色的装甲挽具；颜色不改变战斗作用。',
                {item: {'trigger': 'jeg:gameplay_action', 'conditions': {'action': 'harness_equip', 'subject': 'jeg:' + item}} for item in items}, 'supply/root', 5)
            nodes['guide/supply/harness' + tier]['requirements'] = [items]
        action('supply/harness_repair', 'repair_tool', 'Maintain Your Airlift', '维护空运装备', 'Actually restore damaged happy-ghast armour plating with the repair tool.', '用维修工具实际恢复受损悦灵装甲。', 'harness_repair')

    special_items = ['drone', 'monitor', 'c4_bomb', 'detonator', 'defuser', 'c4_vest', 'claymore_mine', 'tm_62', 'javelin', 'igla_9k38']
    for item in special_items: inventory('special/obtain_' + item, item)
    operations = [
        ('grenade', 'grenade', 'hit', 'jeg:grenade'), ('stun', 'stun_grenade', 'stun_enemy', None),
        ('smoke', 'smoke_grenade', 'smoke_release', None), ('molotov', 'molotov_cocktail', 'hit', 'jeg:molotov_cocktail'),
        ('water_convert', 'water_bomb', 'water_conversion', None), ('water_use', 'water_bomb', 'water_extinguish', None),
        ('place_c4', 'c4_bomb', 'explosive_place', 'jeg:c4_bomb'), ('c4_remote', 'detonator', 'c4_remote', 'jeg:c4_bomb'),
        ('c4_effect', 'c4_bomb', 'hit', 'jeg:c4_bomb'), ('place_claymore', 'claymore_mine', 'explosive_place', 'jeg:claymore_mine'),
        ('claymore_effect', 'claymore_mine', 'hit', 'jeg:claymore_mine'), ('place_tm62', 'tm_62', 'explosive_place', 'jeg:tm_62'),
        ('tm62_effect', 'tm_62', 'hit', 'jeg:tm_62'), ('defuse', 'defuser', 'defuse', None),
        ('drone_deploy', 'drone', 'drone_deploy', None), ('drone_bind', 'monitor', 'drone_bind', None),
        ('drone_control', 'monitor', 'drone_control', None), ('drone_interact', 'monitor', 'drone_interact', None), ('drone_unload', 'drone', 'drone_payload_unload', None),
        ('drone_pack', 'crowbar', 'drone_pack', None), ('javelin_lock', 'javelin', 'launcher_locked', 'jeg:javelin'),
        ('javelin_direct', 'javelin', 'guided_hit', 'direct'), ('javelin_top', 'javelin', 'guided_hit', 'top_attack'),
        ('igla_hit', 'igla_9k38', 'guided_hit', 'air'),
    ]
    for ident, icon, event, subject in operations:
        en, zh, en_desc, zh_desc = OPERATIONS[ident]
        action('special/' + ident, icon, en, zh, en_desc, zh_desc, event, subject)
    for payload in ('c4_bomb', 'tm_62', 'grenade'):
        action('special/drone_load_' + payload, payload, 'Load Payload: ' + name(payload, 'en_us'), '装载载荷：' + name(payload, 'zh_cn'),
               'Insert this payload into an empty drone before taking control.', '将该载荷实际装入空载无人机，然后绑定监视器并控制。', 'drone_payload_load', 'jeg:' + payload)
        action('special/drone_payload_' + payload, payload, 'Drone Payload: ' + name(payload, 'en_us'), '无人机载荷：' + name(payload, 'zh_cn'),
               'Load this payload into a drone and successfully release or activate it through the monitor.', '给无人机装载该载荷，并通过监视器成功投放或激活。', 'drone_payload_release', 'jeg:' + payload)
    action('exploration/c4_crew', 'c4_vest', 'Extended: Coordinated Detonation', '扩展：协同引爆', 'Detonate an owned vest worn by another player. Multiplayer challenge; not required for mastery.', '引爆另一名玩家穿戴的己方绑定背心；多人挑战，不阻塞总完成。', 'vest_detonate', optional=True, xp=100)

    for vehicle in VEHICLES:
        acquire = action('vehicles/assemble_' + vehicle, 'vehicle_assembling_table', 'Assemble ' + name(vehicle, 'en_us'), '组装' + name(vehicle, 'zh_cn'),
                         'Spend the ingredients in the assembly table to receive this vehicle container.', '在组装台实际消耗材料获得此载具容器。', 'assemble', 'jeg:' + vehicle)
        last = acquire
        for event, en, zh in [('deploy', 'Deploy', '部署'), ('mount', 'Board', '登乘'), ('drive', 'Operate', '驾驶'), ('vehicle_supply', 'Resupply', '补给'), ('vehicle_repair', 'Repair', '维修'), ('pack', 'Recover', '回收')]:
            last = action('vehicles/' + event + '_' + vehicle, 'vehicle_assembling_table', en + ' ' + name(vehicle, 'en_us'), zh + name(vehicle, 'zh_cn'),
                          'Perform the real operation on this vehicle. Land vehicles/boats need 20 blocks of controlled travel; helicopters need 10 blocks of controlled ascent and a safe landing. Supply and repair must change its state.',
                          '对该载具实际执行此操作。地面车/快艇需驾驶20格；直升机需受控升高10格并安全着陆。补给和维修必须实际改变状态。', event, 'jeg:' + vehicle, last)
        data = read_json(resource / f'data/jeg/vehicles/{vehicle}.json')
        weapons = {weapon['weapon'] for weapon in data.get('weapons', [])}
        for weapon in sorted(weapons):
            label = weapon.split(':')[-1]
            action('vehicles/weapon_' + vehicle + '_' + label, 'rocket_launcher', 'Vehicle Weapon: ' + label.replace('_', ' ').title(), '车载武器：' + label,
                   'Use this weapon from an authorised seat to deal actual damage to an enemy. Turret direction, ammunition and guidance apply.',
                   '在允许使用此武器的座位上，对敌人造成有效伤害；需满足炮塔方向、弹药与制导条件。', 'vehicle_hit', 'jeg:' + vehicle + '/' + weapon)
        action('vehicles/charge_' + vehicle, 'vehicle_charging_station', 'Recharge ' + name(vehicle, 'en_us'), '充电' + name(vehicle, 'zh_cn'),
               'Use a charging station to restore energy, while operating the vehicle or its station.', '使用充电站实际恢复能量；需驾驶该载具或操作其充电站。', 'vehicle_charge', 'jeg:' + vehicle)
        for event, en, zh, en_desc, zh_desc in [
            ('vehicle_hull_repair', 'Hull Maintenance', '车体维修', 'Use the repair tool to restore damaged hull health. A full-health hull does not count.', '用维修工具实际恢复受损车体生命；满血车体不计入。'),
            ('vehicle_component_repair', 'Component Maintenance', '部件维修', 'Use the repair tool to restore a damaged engine, wheel or turret component. Hull-only repair does not count.', '用维修工具实际恢复受损发动机、行走机构或炮塔部件；仅维修车体不计入。'),
        ]:
            action('vehicles/' + event + '_' + vehicle, 'repair_tool', en + ': ' + name(vehicle, 'en_us'), zh + '：' + name(vehicle, 'zh_cn'), en_desc, zh_desc, event, 'jeg:' + vehicle)
        if data.get('has_decoy'):
            action('vehicles/decoy_' + vehicle, 'flare', 'Countermeasures', '释放反制', 'Successfully deploy this vehicle countermeasure.', '成功释放该载具的反制装备。', 'decoy', 'jeg:' + vehicle)
        if len(data.get('weapons', [])) > 1:
            action('vehicles/switch_' + vehicle, 'rifle_ammo', 'Select Your Weapon', '选择车载武器', 'Successfully change to a different available vehicle weapon.', '成功切换到另一件当前可用车载武器。', 'weapon_change', 'jeg:' + vehicle)
        if vehicle in ('ah6', 'mi28'):
            for event in ('takeoff', 'land'):
                action('vehicles/' + event + '_' + vehicle, 'vehicle_assembling_table', event.title() + ' ' + name(vehicle, 'en_us'), ('起飞' if event == 'takeoff' else '安全着陆') + name(vehicle, 'zh_cn'),
                       'Control the helicopter: climb 10 blocks, then land without impact damage. Dismounting or teleporting does not count.', '受控升高10格后无撞击伤害着陆；下车或传送不计入。', event, 'jeg:' + vehicle)
    action('vehicles/seat', 'vehicle_assembling_table', 'Know Every Seat', '熟悉各座位', 'Successfully change seats in an available vehicle. Seat permissions still determine movement and weapons.', '在可用载具中实际换座；座位权限仍决定驾驶和武器操作。', 'seat_change')
    action('vehicles/reload', 'rifle_ammo', 'Keep the Turret Fed', '维持车载火力', 'Complete a vehicle weapon reload with matching inventory ammunition.', '使用库存兼容弹药完成车载武器装填。', 'vehicle_reload')
    action('vehicles/defence', 'flare', 'Break Missile Tracking', '打断导弹跟踪', 'Make an incoming hostile missile lose tracking to smoke or switch to a flare decoy.', '让来袭敌方导弹因烟雾失去跟踪或被诱饵吸引。', 'missile_defence', xp=25)
    for item in ('vehicle_assembling_table', 'vehicle_charging_station', 'crowbar'):
        inventory('vehicles/obtain_' + item, item)
    action('special/timed_c4', 'c4_bomb', 'Timed Demolition', '定时爆破', 'Set C4 to timed mode, place it and let its timer actually detonate. Remote detonation does not satisfy this objective.', '将C4设为定时模式后放置，等待计时器实际引爆；遥控引爆不计入此目标。', 'c4_timed')
    action('battle/bayonet_fit', 'minecraft:iron_sword', 'A Blade on the Barrel', '枪口装刃', 'Install a compatible sword in the barrel slot and deal actual bayonet damage to an enemy.', '将兼容剑安装在枪管槽位，再用刺刀对敌人造成有效伤害。', 'attachment_use', 'jeg:bayonet')

    add('exploration/armada', 'phantom_smg', 'End Ship Armada', '末地舰队', 'Explore the End Ship Armada in the End. Prepare air defence, ammunition and protection.', '探索末地舰队；提前准备防空武器、弹药和防护。',
        {'explore': {'trigger': 'minecraft:location', 'conditions': {'player': {'location': {'structures': 'jeg:sky_ship_armada'}}}}}, 'exploration/root', 25)
    action('exploration/loot', 'phantom_smg', 'Search the Fleet', '搜寻舰队战利品', 'Open a chest while inside the End Ship Armada.', '在末地舰队结构内实际打开箱子。', 'open_container', parent='exploration/armada', extra={'player': {'location': {'structures': 'jeg:sky_ship_armada'}}})
    action('exploration/guardian', 'igla_9k38', 'Defeat the Bound Terror Phantom', '击败束缚恐惧幻翼', 'Defeat the structure guardian with credited combat damage. Its death may start an aerial follow-up raid.', '在结构战斗中击败守护者并获得击杀归属；其死亡可触发后续空袭。', 'enemy_kill', 'jeg:terror_phantom_guardian', 'exploration/armada', 25)
    action('exploration/after_battle', 'igla_9k38', 'Survive the Follow-up', '迎战后续袭击', 'Win the aerial Terror raid triggered by the Bound Terror Phantom. Stay in raid range until every wave is defeated; prepare air defence before fighting the guardian.', '击败束缚恐惧幻翼触发空袭；在袭击范围内击退所有波次。挑战守护者前准备防空武器。', 'terror_raid_win', parent='exploration/guardian', xp=25)
    action('exploration/free_terror', 'igla_9k38', 'Extended: Free-roaming Terror', '扩展：自由游荡的恐惧', 'Free-roaming Terror Phantom conversion is disabled by default. Requires server/admin-enabled content; not required for mastery.', '自由游荡恐惧幻翼默认禁用；需服务器或管理员启用，不阻塞总完成。', 'enemy_kill', 'jeg:terror_phantom', optional=True, xp=100)
    action('exploration/sniper', 'bolt_action_rifle', 'Extended: Distant Headshot', '扩展：远距爆头', 'Deal a headshot at least 50 blocks away with a gun.', '用枪械在至少50格外完成有效爆头。', 'long_headshot', optional=True, xp=100)
    dyes = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
    add('exploration/colours', 'minecraft:red_dye', 'Extended: Every Colour', '扩展：全部配色', 'Apply all sixteen gun dyes across your weapons. Optional collection challenge.', '给枪械实际使用全部16种染料；可分不同武器完成的扩展收集。',
        {dye: {'trigger': 'jeg:gameplay_action', 'conditions': {'action': 'gun_dye', 'subject': 'minecraft:' + dye + '_dye'}} for dye in dyes}, 'exploration/root', 100, True, 'challenge')
    # Parent links only arrange the guide. Actual completion is an AND of saved objectives.
    mastery = add('training/complete', 'holy_shotgun', 'Complete Combat Mastery', '全玩法战斗精通',
                  'Complete every default survival objective in this version. Optional, multiplayer and colour challenges are excluded.',
                  '完成本版本全部默认生存玩法目标；扩展、多人和重复配色挑战不计入总完成。',
                  {'mastery': {'trigger': 'minecraft:tick', 'conditions': {'player': {'type_specific': {'type': 'minecraft:player', 'advancements': {ident: True for ident in sorted(set(core))}}}}}}, previous, 250, frame='challenge')
    for tab in TABS:
        if tab == 'training': continue
        members = [ident for ident in core if ident.startswith('jeg:guide/' + tab + '/')]
        if members:
            add(tab + '/complete', TABS[tab][2], 'Branch Mastery', '分支精通', 'Complete this branch’s survival objectives.', '完成该分支所有默认生存目标。',
                {'complete': {'trigger': 'minecraft:tick', 'conditions': {'player': {'type_specific': {'type': 'minecraft:player', 'advancements': {ident: True for ident in members}}}}}}, tab + '/root', 25, frame='goal')
    # Migrate valid recipe-unlock advancements without changing their identifiers.
    recipes = {path.stem for path in (resource / 'data/jeg/recipe').glob('*.json')}
    for folder in ('advancements', 'advancement'):
        old_root = resource / 'data/jeg' / folder
        if old_root.exists():
            for path in old_root.rglob('*.json'):
                rel = path.relative_to(old_root).with_suffix('').as_posix()
                if not rel.startswith('recipes/'): continue
                old = read_json(path)
                rewards = old.get('rewards', {}).get('recipes', [])
                valid = [ident for ident in rewards if ident.startswith('jeg:') and ident[4:] in recipes]
                if rewards and not valid: continue
                if rewards: old['rewards']['recipes'] = valid
                # Some 1.21.1 leftovers reference a vanilla happy-ghast item which does not exist.
                if not modern and 'harness' in rel: continue
                nodes[rel] = old
    mastery_conditions = nodes['guide/' + mastery]['criteria']['mastery']['conditions']['player']
    if '26.3' in module.name: mastery_conditions = mastery_conditions['predicate']
    catalogue = {'tabs': list(TABS), 'vehicles': list(VEHICLES), 'weapons': guns, 'attachments': attachments + cosmetics,
                 'magazines': magazines, 'ammo': ammo, 'excluded': EXCLUDED_GUNS | UNOBTAINABLE, 'objectives': manifest,
                 'mastery': mastery_conditions['minecraft:type_specific/player' if modern else 'type_specific']['advancements']}
    registered_items = set(re.findall(r'(?:REGISTER.register|registerAttachment)\(\s*"([^\"]+)"', item_source)) | set(guns) | set(ammo)
    if modern: registered_items.update(path.stem for path in (resource / 'data/jeg/recipe').glob('armored_joy_harness_*.json'))
    objective_text = json.dumps(manifest)
    catalogue['item_coverage'] = {}
    for item in sorted(registered_items):
        if item.endswith('_spawn_egg') or item == 'gunsmith_manual' or item in EXCLUDED_GUNS:
            catalogue['item_coverage'][item] = {'excluded': EXCLUDED_GUNS.get(item, 'Creative-only item; no default survival recipe or loot')}
        elif item == 'vehicle_container':
            catalogue['item_coverage'][item] = {'aggregate': ['jeg:guide/vehicles/assemble_' + vehicle for vehicle in VEHICLES]}
        elif item.startswith('armored_joy_harness_'):
            tier = '_netherite' if item.endswith('_netherite') else '_diamond' if item.endswith('_diamond') else ''
            catalogue['item_coverage'][item] = {'aggregate': ['jeg:guide/supply/harness' + tier]}
        else:
            ids = [row['id'] for row in manifest if 'jeg:' + item in json.dumps(row['criteria'])]
            assert ids, f'Uncovered registered content: {item}'
            catalogue['item_coverage'][item] = {'objectives': ids}
    return nodes, languages, catalogue

def validate(nodes, languages, catalogue):
    assert len([key for key in nodes if key.startswith('guide/') and key.endswith('/root')]) == 8
    assert len(catalogue['weapons']) >= 30 and len(catalogue['magazines']) == 11
    assert catalogue['vehicles'] == list(VEHICLES)
    for key, node in nodes.items():
        if not key.startswith('guide/'): continue
        for field in ('title', 'description'):
            translation = node['display'][field]['translate']
            assert all(translation in entries for entries in languages.values()), translation
        assert all(group and all(criterion in node['criteria'] for criterion in group) for group in node['requirements']), key
        seen = set()
        cursor = key
        while cursor.startswith('guide/'):
            assert cursor not in seen, f'cycle: {key}'
            seen.add(cursor)
            parent = nodes[cursor].get('parent')
            if not parent: break
            assert parent.startswith('jeg:') and parent[4:] in nodes, parent
            cursor = parent[4:]
    optional = {row['id'] for row in catalogue['objectives'] if row['optional']}
    assert not (set(catalogue['mastery']) & optional), 'optional content blocks mastery'

def run(module, check):
    nodes, languages, catalogue = generate(module)
    validate(nodes, languages, catalogue)
    source = '\n'.join(path.read_text(encoding='utf-8') for path in (module / 'src/main/java').rglob('*.java'))
    for row in catalogue['objectives']:
        for criterion in row['criteria'].values():
            if criterion['trigger'] == 'jeg:gameplay_action':
                event = criterion['conditions']['action']
                assert '"' + event + '"' in source, f'Missing gameplay event entry: {event}'
    for node in nodes.values():
        for recipe in node.get('rewards', {}).get('recipes', []):
            assert (module / f'src/main/resources/data/jeg/recipe/{recipe[4:]}.json').exists(), recipe
    resources = module / 'src/main/resources'
    expected = {resources / f'data/jeg/advancement/{key}.json': value for key, value in nodes.items()}
    expected.update({resources / f'assets/jeg/lang/{locale}.json': value for locale, value in languages.items()})
    expected[module / 'docs/advancement_coverage.json'] = catalogue
    for path, value in expected.items():
        if check:
            assert path.exists() and read_json(path) == value, f'stale: {path}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    # Only obsolete recipe advancements are migrated/deleted; never remove foreign files.
    if not check:
        old = resources / 'data/jeg/advancements'
        if old.exists():
            for path in old.rglob('*.json'):
                if path.relative_to(old).as_posix().startswith('recipes/'): path.unlink()
    print(f'{module.name}: {len(catalogue["objectives"])} guide nodes, {len(catalogue["mastery"])} mastery objectives verified')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--all', action='store_true')
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--module', type=Path)
    args = parser.parse_args()
    local = Path(__file__).resolve().parents[1]
    targets = [args.module or local]
    if args.all:
        targets = [local.parent / f'Just-Enough-Guns-{loader}-{version}' for loader in ('Fabric', 'NeoForge') for version in ('1.21.1', '26.2', '26.3')]
    for target in targets: run(target, args.check)
