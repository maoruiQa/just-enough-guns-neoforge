"""Generate/check the four-chapter career guide and native-save migration. Stdlib only."""
from pathlib import Path
from collections import Counter
import argparse
import copy
import json
import re

VEHICLES = ('speedboat', 'bmp2', 'lav150', 'ah6', 'mi28')
FACTIONS = ('night_of_the_undead', 'the_rattlers', 'nosy_business', 'bad_piggies', 'hell_hogs', 'lost_souls')
EXCLUDED = {'abstract_gun': 'Developer weapon', 'typhoonee': 'Disabled weapon', 'phantom_smg': 'No default survival recipe or loot'}
TABS = ('training', 'battle', 'special', 'vehicles')


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def event(action, subject=None):
    conditions = {'action': action}
    if subject is not None:
        conditions['subject'] = subject
    return {'trigger': 'jeg:gameplay_action', 'conditions': conditions}


def group(label, action, subjects=None):
    return label, [event(action, subject) for subject in subjects] if subjects is not None else [event(action)]


def generate(module):
    resource = module / 'src/main/resources'
    java = module / 'src/main/java/ttv/migami/jeg'
    modern = '1.21.1' not in module.name
    legacy = read_json(module / 'scripts/legacy_advancement_criteria.json')
    languages = {locale: read_json(resource / f'assets/jeg/lang/{locale}.json') for locale in ('en_us', 'zh_cn')}
    for entries in languages.values():
        for key in list(entries):
            if key.startswith('advancement.jeg.guide.'):
                del entries[key]
    nodes, objectives, completion = {}, [], {}

    def native(criterion):
        criterion = copy.deepcopy(criterion)
        conditions = criterion.get('conditions', {})
        if '26.3' in module.name and 'recipe_id' in conditions:
            conditions['recipes'] = conditions.pop('recipe_id')
        if modern and 'player' in conditions:
            predicate = conditions['player']
            if 'type_specific' in predicate or 'location' in predicate:
                predicate = {('minecraft:type_specific/player' if key == 'type_specific' else 'minecraft:' + key):
                             ({k: v for k, v in value.items() if k != 'type'} if key == 'type_specific' else value)
                             for key, value in predicate.items()}
                conditions['player'] = {'type': 'minecraft:entity_properties', 'entity': 'this', 'predicate': predicate} if '26.3' in module.name else predicate
        return criterion

    def add(path, parent, icon, en, zh, en_desc, zh_desc, groups=None, xp=5, frame='task', optional=False):
        if groups is None:
            criteria = copy.deepcopy(legacy['guide/' + path]['criteria'])
            requirements = copy.deepcopy(legacy['guide/' + path]['requirements'])
        else:
            criteria, requirements = {}, []
            for label, alternatives in groups:
                requirement = []
                for index, criterion in enumerate(alternatives):
                    conditions = criterion.get('conditions', {})
                    value = conditions.get('subject', conditions.get('recipe_id', str(index)))
                    key = label if len(alternatives) == 1 else label + '_' + re.sub(r'[^a-z0-9_]', '_', value)
                    assert key not in criteria, (path, key)
                    criteria[key] = native(criterion)
                    requirement.append(key)
                requirements.append(requirement)
        if optional:
            en_desc += ' Independent challenge; not required for chapter or career completion.'
            zh_desc += ' 独立挑战，不计入章节完成或总完成。'
            xp, frame = 100, 'challenge'
        key = 'advancement.jeg.guide.' + path.replace('/', '.')
        for locale, title, description in [('en_us', en, en_desc), ('zh_cn', zh, zh_desc)]:
            languages[locale][key + '.title'] = title
            languages[locale][key + '.description'] = description
        node = {'display': {'icon': {'id': icon if ':' in icon else 'jeg:' + icon}, 'title': {'translate': key + '.title'},
                            'description': {'translate': key + '.description'}, 'frame': frame,
                            'show_toast': parent is not None, 'announce_to_chat': frame == 'challenge', 'hidden': False},
                'criteria': criteria, 'requirements': requirements, 'sends_telemetry_event': False}
        chapter = path.split('/')[0]
        if parent:
            node['parent'] = 'jeg:guide/' + parent
            chapter = next(row['chapter'] for row in objectives if row['id'] == 'jeg:guide/' + parent)
        else:
            node['display']['background'] = 'minecraft:gui/advancements/backgrounds/adventure' if modern else 'minecraft:textures/gui/advancements/backgrounds/adventure.png'
        if xp:
            node['rewards'] = {'experience': xp}
        nodes['guide/' + path] = node
        objectives.append({'id': 'jeg:guide/' + path, 'chapter': chapter, 'optional': optional,
                           'title': en, 'title_zh': zh, 'criteria': criteria})

    def one(path, parent, icon, en, zh, en_desc, zh_desc, action=None, subject=None, **kwargs):
        add(path, parent, icon, en, zh, en_desc, zh_desc, [('done', [event(action, subject)])] if action else None, **kwargs)

    gun_source = (java / 'gun/GunDefinitions.java').read_text(encoding='utf-8')
    guns = sorted(set(re.findall(r'Reference.MOD_ID, "([^"]+)"\), new GunStats', gun_source)) - {'abstract_gun', 'typhoonee'}) + ['javelin', 'igla_9k38']
    item_source = (java / 'init/ModItems.java').read_text(encoding='utf-8')
    slots = dict(re.findall(r'registerAttachment\(\s*"([^"]+)"\s*,\s*AttachmentType\.(\w+)', item_source))
    magazines = sorted(set(re.findall(r'new MagazineItem\(baseProperties\(Reference.id\("([^"]+)"', item_source)))
    ammo = sorted(set(re.findall(r'"([a-z0-9_]+)"', item_source.split('AMMO_IDS = Set.of(')[1].split(');')[0])))
    categories = {}
    for members, category in re.findall(r'case (.+?) -> (\w+);', (java / 'gun/GunCategory.java').read_text(encoding='utf-8')):
        categories.update({gun: category.lower() for gun in re.findall(r'"([^"]+)"', members)})
    recipes = {path.stem for path in (resource / 'data/jeg/recipe').glob('*.json')}

    for tab, icon, en, zh, desc, zh_desc in [
        ('training', 'minecraft:book', 'New Recruit', '新兵报到', 'Chapter I: Ready for Action. Prepare, fight and maintain your equipment; equivalent equipment is your choice.', '第一章·整装待发。学习准备、开火和维护装备；同类装备自由选择。'),
        ('battle', 'infantry_rifle', 'Front-line Experience', '前线历练', 'Chapter II: Choose your combat role, then defend your home through a full campaign.', '第二章·前线历练。选择作战角色，经历巡逻与保卫家园的完整战役。'),
        ('special', 'monitor', 'Special Operations', '特种行动', 'Chapter III: Three routes through throwing weapons, demolition and remote operations.', '第三章·特种行动。沿投掷、爆破和遥控三条路线掌握特种装备。'),
        ('vehicles', 'vehicle_assembling_table', 'Steel and Expeditions', '钢铁与远征', 'Chapter IV: Lead five vehicles into battle and take the expedition to the End.', '第四章·钢铁与远征。体验五种载具的特色任务，最终远征末地。'),
    ]:
        add(tab + '/root', None, icon, en, zh, desc, zh_desc, [('start', [{'trigger': 'minecraft:tick'}])], xp=0)

    one('training/recipes', 'training/root', 'minecraft:book', 'Read Before You Build', '先读后造', 'Open the crafting recipe book with a known JEG recipe. Check its materials and your current key bindings.', '打开制作配方书，查看已知JEG配方及材料；操作以当前按键绑定为准。')
    craft_guns = [gun for gun in guns if gun in recipes and gun not in EXCLUDED]
    craft_ammo = sorted(recipes & {'pistol_ammo', 'rifle_ammo', 'shotgun_shell', 'handmade_round', 'handmade_shell'})
    craft = lambda ids: [{'trigger': 'minecraft:recipe_crafted', 'conditions': {'recipe_id': 'jeg:' + item}} for item in ids]
    add('training/ready', 'training/recipes', 'combat_pistol', 'Ready for Action', '整装待发', 'Craft any survival gun and compatible ammunition. Separate sessions count; merely owning the items does not.', '制作任意生存枪械和兼容弹药，可分次制作；仅持有物品不计入。', [('weapon', craft(craft_guns)), ('ammo', craft(craft_ammo))])
    add('training/ammunition_ready', 'training/ready', 'pistol_magazine', 'Ammunition Ready', '弹药就位', 'Load compatible rounds into any magazine, then finish a gun reload. Failed or interrupted operations do not count.', '给任意弹匣实际装入兼容弹药，再完成一次枪械装填；失败或中断不计入。', [group('load', 'magazine_load'), group('reload', 'reload')])
    one('training/aim', 'training/ammunition_ready', 'reflex_sight', 'Inside the Reticle', '准星之内', 'Aim before firing and deal actual enemy damage. Aim and weapon attribution follow the fired projectile.', '瞄准后对敌人造成有效伤害；瞄准状态和武器归属以发射时为准。')
    one('training/kill', 'training/aim', 'combat_rifle', 'First Contact', '初次交锋', 'Defeat any armed JEG gunner with credited combat damage. Ordinary unarmed mobs do not count.', '击败任意JEG武装枪手并获得击杀归属；普通未武装生物不计入。')
    add('training/loadout', 'training/ammunition_ready', 'reflex_sight', 'Made to Measure', '量身定制', 'Install a compatible attachment. Then land enemy hits with one sight while aiming, one stock or grip, and one functional muzzle. Choose compatible models; separate loadouts and fights count.', '成功安装配件，再分别让任意瞄具在瞄准时、任意枪托或握把、任意功能枪口参与有效命中。可选兼容型号，可使用不同配装、分次战斗。', [group('installed', 'attachment_install'), group('sight', 'attachment_use', ['jeg:' + item for item, slot in sorted(slots.items()) if slot == 'SCOPE']), group('handling', 'attachment_use', ['jeg:' + item for item, slot in sorted(slots.items()) if slot in ('STOCK', 'UNDER_BARREL')]), group('muzzle', 'attachment_use', ['jeg:' + item for item, slot in sorted(slots.items()) if slot == 'BARREL'])], xp=25, frame='goal')
    add('training/electronics', 'training/loadout', 'flashlight', 'Light and Guidance', '光电协同', 'Operate a powered flashlight and laser, and recharge the flashlight. Separate sessions count.', '实际使用有电的手电和激光，并给手电充电；操作可分次完成。', [group('light', 'flashlight_on'), group('laser', 'laser_on'), group('charge', 'flashlight_charge')])
    armour = lambda kind: ['jeg:bulletproof_' + kind + '_' + tier for tier in ('i', 'ii', 'iii', 'iv', 'v', 'vi')]
    add('training/protection', 'training/ready', 'bulletproof_vest_i', 'One More Layer', '多一层保障', 'Wear a ballistic helmet and vest of any tier, and have armour reduce an incoming gun hit. No six-tier collection is required.', '穿戴任意等级弹道头盔和防弹衣，并让护甲实际降低枪弹伤害；无需逐级收集。', [group('helmet', 'equip', armour('helmet')), group('vest', 'equip', armour('vest')), group('intercept', 'armour_intercept')], xp=25, frame='goal')
    add('training/maintenance', 'training/protection', 'repair_kit', 'Stay Battle Ready', '保持战备', 'Repair a damaged gun at an anvil and finish cooling a heated compatible gun. Ordinary or enhanced coolant counts.', '在铁砧修复受损枪械，并给有热量的兼容武器完成冷却；普通或增强冷却剂均可。', [group('repair', 'gun_repair'), group('cool', 'cool')])
    add('training/logistics', 'training/maintenance', 'magazine_loader', 'Supply Line', '补给流水线', 'Unload a magazine, collect loaded output from a magazine loader, and reload with any physical extended or drum magazine. Legacy capacity attachments are unnecessary.', '完成弹匣退弹、取出装弹机的实际装弹产物，并用任意实体加长弹匣或弹鼓换弹；无需旧式容量配件。', [group('unload', 'magazine_unload'), group('loader', 'loader_output'), group('capacity', 'magazine_used', ['jeg:' + item for item in magazines if 'extended' in item or 'drum' in item])], xp=25, frame='goal')
    add('training/signature', 'training/loadout', 'minecraft:red_dye', 'Personal Signature', '个人签名', 'Apply any gun dye or successfully install any paint job. Choose the look you like; collecting every colour is unnecessary.', '给枪械染任意颜色，或成功安装任意涂装；选择喜欢的外观，无需收集全部配色。', [('style', [event('gun_dye')] + [event('attachment_install', 'jeg:' + item) for item in ('classic_spray_can', 'toy_spray_can', 'whiteout_spray_can', 'golden_spray_can')])], optional=True)
    one('gunsmith/kill_effect', 'training/signature', 'headpoppper_badge', 'Leave Your Mark', '留下记号', 'With any kill-effect badge installed, headshot-kill an enemy and actually activate its effect.', '安装任意击杀徽章，爆头击败敌人并实际触发效果。', optional=True)
    one('gunsmith/enchant', 'training/signature', 'minecraft:enchanted_book', 'The Finishing Touch', '匠心加持', 'Enchant any gun at an enchanting table.', '在附魔台给任意枪械附魔。', optional=True)
    if modern:
        harnesses = sorted(item for item in recipes if item.startswith('armored_joy_harness_'))
        add('training/airlift', 'training/logistics', 'armored_joy_harness_white', 'Protected Airlift', '空运护航', 'Equip a happy ghast with any colour and tier of armoured harness, and repair damaged armour plating with the repair tool.', '给悦灵装备任意颜色、任意等级装甲挽具，再用维修工具实际修复受损装甲。', [group('equip', 'harness_equip', ['jeg:' + item for item in harnesses]), group('repair', 'harness_repair')])

    one('arsenal/category_rifle', 'battle/root', 'infantry_rifle', 'Rifleman', '步枪手', 'Defeat an enemy using any rifle. The projectile retains its weapon category after you switch guns.', '使用任意步枪击败敌人；换枪后弹丸仍保留发射时的武器类别。')
    one('battle/patrol', 'battle/root', 'infantry_rifle', 'Cut Off the Patrol', '截断巡逻', 'Clear a faction patrol and earn Faction Omen. This starts the faction Raid route; return near your respawn point next. Patrol spawning must be enabled.', '清剿派系巡逻并获得派系不祥之兆，开启派系Raid战役路线；随后返回重生点附近。需启用巡逻生成。', xp=25, frame='goal')
    one('battle/home_raid', 'battle/patrol', 'flare', 'War Comes Home', '烽火归家', 'Return near your respawn point with Faction Omen and successfully trigger the home raid.', '携带派系不祥之兆返回重生点附近，实际触发家园袭击。', xp=25, frame='goal')
    one('battle/raid', 'battle/home_raid', 'assault_rifle', 'Hold the Line', '守住阵地', 'Win every raid wave and qualify under the existing participation rules. Stay in the combat area and contribute.', '击退全部袭击波次，并满足现有战役参与资格；留在战区并参与战斗。', xp=25, frame='goal')
    one('arsenal/category_pistol', 'arsenal/category_rifle', 'combat_pistol', 'Sidearm Ready', '随身火力', 'Defeat an enemy with any pistol. Revolvers and other registered pistols count.', '用任意手枪击败敌人；左轮等已分类手枪同样计入。')
    add('battle/close_quarters', 'arsenal/category_pistol', 'pump_shotgun', 'Room to Room', '近距清场', 'Defeat an enemy using any submachine gun or shotgun. Choose either role; no individual weapon checklist.', '用任意冲锋枪或霰弹枪击败敌人，两类任选其一；无需逐件使用。', [group('kill', 'category_kill', ['smg', 'shotgun'])])
    add('battle/close_combat', 'battle/close_quarters', 'minecraft:iron_sword', 'Within Arm Reach', '贴身应敌', 'Deal actual enemy damage with gun melee or a fitted bayonet.', '用枪械近战或已安装刺刀，对敌人造成有效伤害。', [('hit', [event('melee_hit'), event('bayonet_hit')])])
    one('battle/charge', 'battle/close_combat', 'minecraft:diamond_sword', 'Bayonet Blood', '刺刀见红', 'Fit a compatible sword in the barrel slot, sprint and deal enemy damage with a bayonet charge.', '在兼容枪管槽安装剑，冲刺并用刺刀冲锋实际伤害敌人。', optional=True)
    add('battle/precision', 'arsenal/category_rifle', 'bolt_action_rifle', 'Steady, Accurate, Deadly', '稳准狠', 'Defeat an enemy with a precision-category weapon, land a headshot, and deal damage at least 50 blocks away. These feats may occur in different fights.', '用精确类武器击败敌人、完成爆头、在至少50格外有效命中；三项目标可在不同战斗完成。', [group('kill', 'category_kill', ['sniper']), group('headshot', 'headshot'), group('range', 'range_hit')], xp=25, frame='goal')
    add('battle/support', 'battle/precision', 'light_machine_gun', 'Suppress and Breach', '压制与破障', 'Defeat an enemy with any support-category weapon and another with any heavy-category weapon.', '分别用任意支援类和重型武器击败敌人；学习战斗角色，无需每把枪都使用。', [group('support', 'category_kill', ['lmg']), group('heavy', 'category_kill', ['heavy'])], xp=25, frame='goal')
    one('exploration/sniper', 'battle/precision', 'bolt_action_rifle', 'A Distant Bullseye', '百步穿杨', 'Land one effective headshot from at least 50 blocks away. Range and headshot must belong to the same hit.', '在至少50格外完成一次有效爆头；距离与爆头必须来自同一次命中。', optional=True)
    add('battle/enemy_profiles', 'battle/root', 'combat_rifle', 'Know Your Enemy', '知己知彼', 'Defeat an armed zombie-family gunner, a skeleton-family gunner, and a pillager or piglin-family gunner. Any member of each group counts; unarmed mobs do not.', '分别击败丧尸系、骷髅系，以及掠夺者或猪灵系的武装Gunner；每组任选一种，普通未武装生物不计入。', [group('undead', 'gunner_kill', ['zombie', 'husk', 'drowned', 'zombieVillager', 'zombifiedPiglin']), group('marksman', 'gunner_kill', ['skeleton', 'stray', 'witherSkeleton'] + (['parched'] if modern else [])), group('raider', 'gunner_kill', ['pillager', 'vindicator', 'piglin', 'piglinBrute'])], xp=25, frame='goal')
    one('battle/elite_hunter', 'battle/enemy_profiles', 'bulletproof_vest_iv', 'Elite Hunter', '精锐克星', 'Defeat an actual Elite Gunner with credited enemy damage. Its elite role is assigned by JEG; ordinary gunners wearing armour do not count.', '实际击败JEG生成的精锐Gunner并获得击杀归属；仅给普通枪手穿上护甲不算精锐。', 'elite_gunner_kill', xp=25, frame='goal')
    one('battle/bomber_intercept', 'battle/elite_hunter', 'c4_vest', 'Before the Last Beep', '截停自爆者', 'Defeat an enemy C4-vest bomber before its vest detonates. You need credited lethal damage; watching its suicide explosion does not count.', '在C4背心引爆前实际击败敌方自爆Gunner并获得致命伤害归属；旁观其自爆不计入。', 'bomber_gunner_kill', xp=25, frame='goal')
    one('battle/phantom_hunter', 'battle/elite_hunter', 'minecraft:phantom_membrane', 'Gunner in the Night', '夜空猎手', 'Defeat an armed Phantom Gunner. This is distinct from the Bound Terror Phantom guardian and requires Phantom Gunner spawning or conversion to be enabled.', '击败武装幻翼Gunner；与束缚恐惧幻翼守护者分别判定，需启用幻翼枪手生成或转化。', 'enemy_kill', 'jeg:phantom_gunner', optional=True)
    add('battle/anti_armour', 'battle/enemy_profiles', 'rocket_launcher', 'Against the Armour', '以小搏大', 'Destroy an enemy BMP2 or LAV150. Enemy vehicles appear from the configured progression day, default day 80. Own vehicles do not count.', '击毁敌方BMP2或LAV150，任选其一。敌方载具从配置进度天数开始出现，默认第80天；己方载具不计入。', [group('kill', 'enemy_vehicle_kill', ['jeg:bmp2', 'jeg:lav150'])], xp=25, frame='goal')
    add('battle/anti_air', 'battle/anti_armour', 'igla_9k38', 'Bring Down the Threat', '击落威胁', 'Destroy an enemy AH-6 or MI-28. Any correctly credited combat weapon may finish it.', '击毁敌方AH-6或MI-28，任选其一；有正确战斗归属的武器均可。', [group('kill', 'enemy_vehicle_kill', ['jeg:ah6', 'jeg:mi28'])], xp=25, frame='goal')
    add('battle/six_fronts', 'battle/raid', 'infantry_rifle', 'Six Fronts', '六面迎敌', 'Defeat a patrol member of each of the six default factions. Custom factions are not required.', '分别击败六个默认派系的巡逻成员；不要求自定义派系。', [group(faction, 'faction_kill', [faction]) for faction in FACTIONS], optional=True)

    add('special/fuse', 'special/root', 'grenade', 'Master the Fuse', '掌握引信', 'Shorten a grenade fuse with a controlled throw, and damage an enemy with a grenade. Letting it explode in your hand does not count as a controlled throw.', '握持已拉开引信的手雷以缩短引信，再成功投掷；并让手雷实际伤害敌人。握持至手中爆炸不算受控投掷。', [group('cook', 'grenade_cook', ['jeg:grenade']), group('hit', 'hit', ['jeg:grenade'])])
    one('special/stun', 'special/fuse', 'stun_grenade', 'White Light', '白光突袭', 'Actually flash an enemy with a stun grenade. Distance and line of sight affect success.', '用闪光弹实际影响敌人；距离和视线影响效果。')
    one('special/smoke', 'special/stun', 'smoke_grenade', 'Behind the Smoke', '烟幕之后', 'Throw a smoke grenade and let it actually release smoke.', '投掷烟雾弹，等待烟幕实际释放。')
    one('arsenal/use_flare_gun', 'special/smoke', 'flare_gun', 'Light Up the Night', '照亮夜空', 'Use a signal flare to successfully trigger its ignition or illumination function. Merely firing into empty space does not count.', '用信号枪成功触发点火或照明功能；只向空处开火不计入。', optional=True)
    one('special/molotov', 'special/fuse', 'molotov_cocktail', 'Denied Ground', '拒止火线', 'Cause actual enemy fire damage with your Molotov. Ignition without damage is insufficient.', '用燃烧瓶造成敌人实际燃烧伤害；只点燃而未伤害不计入。')
    add('special/water_response', 'special/molotov', 'water_bomb', 'Adapt to Water', '遇水应变', 'Convert a grenade into a water bomb while submerged, and extinguish a burning entity or fire block with a water bomb.', '在水下将手雷转化为水弹，并用水弹扑灭燃烧实体或火焰方块。', [group('convert', 'water_conversion'), group('extinguish', 'water_extinguish')])
    one('special/place_c4', 'special/root', 'c4_bomb', 'Set the Charge', '布设爆破', 'Place C4 on a valid surface. Choose its timer or remote mode before placement.', '在有效表面实际放置C4；放置前选择定时或遥控模式。')
    one('special/timed_c4', 'special/place_c4', 'c4_bomb', 'The Countdown', '倒数时刻', 'Place timed C4 and let the timer actually detonate it. Remote detonation does not count.', '放置定时C4，等待计时器实际引爆；遥控引爆不计入。')
    add('special/remote_ambush', 'special/timed_c4', 'detonator', 'One Touch', '一触即发', 'Remotely detonate bound C4, and damage an enemy with your C4. Separate detonations count; switching items does not change ownership.', '实际遥控引爆已绑定C4，并用自己布设的C4伤害敌人；可分次引爆完成，换手持物不会改变归属。', [group('remote', 'c4_remote', ['jeg:c4_bomb']), group('hit', 'hit', ['jeg:c4_bomb'])], xp=25, frame='goal')
    add('special/ambush', 'special/remote_ambush', 'claymore_mine', 'An Unwelcome Guest', '请君入瓮', 'Damage an enemy with your claymore or TM-62; choose either. TM-62 needs a heavy target or enemy vehicle.', '用自己布设的定向雷或TM-62实际伤害敌人，任选一种；TM-62需要重型目标或敌方载具。', [group('hit', 'hit', ['jeg:claymore_mine', 'jeg:tm_62'])])
    one('special/defuse', 'special/ambush', 'defuser', 'Cut the Danger', '剪断危机', 'Keep the defuser on a reachable placed explosive until defusing finishes. Interrupted or failed requests do not count.', '对可触及的已放置爆炸物持续使用拆弹器至完成；中断或失败不计入。')
    one('exploration/c4_crew', 'special/remote_ambush', 'c4_vest', 'Perfect Timing', '默契引爆', 'Detonate your bound C4 vest worn by another player. This requires multiplayer.', '引爆由另一名玩家穿戴、由自己绑定的C4背心；此挑战需要多人。', optional=True)
    add('special/remote_control', 'special/root', 'monitor', 'Beyond Your Sight', '视线之外', 'Deploy a drone, bind a monitor, enter valid control and perform a successful remote block interaction or enemy attack.', '部署无人机、绑定监视器、实际取得控制，并成功遥控操作方块或攻击敌人。', [group('deploy', 'drone_deploy'), group('bind', 'drone_bind'), group('control', 'drone_control'), group('interact', 'drone_interact')])
    add('special/air_delivery', 'special/remote_control', 'drone', 'Air Delivery', '空中投送', 'Load and successfully release any grenade, C4 or TM-62 drone payload through the monitor. A failed release does not count.', '装载手雷、C4或TM-62中的任意一种无人机载荷，通过监视器实际释放；失败投放不计入。', [group('release', 'drone_payload_release', ['jeg:grenade', 'jeg:c4_bomb', 'jeg:tm_62'])])
    add('special/return_home', 'special/air_delivery', 'crowbar', 'Bring It All Home', '完整返航', 'Unload a payload and recover a drone with the crowbar. Separate payloads or drones count.', '完成载荷卸载，并用撬棍回收无人机；可使用不同载荷或无人机分次完成。', [group('unload', 'drone_payload_unload'), group('recover', 'drone_pack')])
    add('special/guidance', 'special/remote_control', 'javelin', 'Guidance Specialist', '制导专家', 'Deal valid guided damage with Javelin in direct and top-attack modes, and with Igla against an eligible airborne enemy. All three are required; ammunition and lock rules apply.', '分别以Javelin直射、攻顶以及Igla对合格空中敌人的制导命中造成有效伤害；三项均需完成，仍须满足弹药与锁定要求。', [group('direct', 'guided_hit', ['direct']), group('top', 'guided_hit', ['top_attack']), group('air', 'guided_hit', ['air'])], xp=25, frame='goal')

    add('vehicles/workshop', 'vehicles/root', 'vehicle_assembling_table', 'From Workshop to Front', '车间出发', 'Spend materials to assemble a vehicle container, then successfully deploy a vehicle. Failed placement and creative spawning do not count.', '实际消耗材料组装载具容器，并成功部署载具；失败放置或创造生成不计入。', [group('assemble', 'assemble'), group('deploy', 'deploy')])
    add('vehicles/crew', 'vehicles/workshop', 'rifle_ammo', 'Everyone Has a Role', '各司其职', 'Change seats, switch to another available weapon and finish a vehicle weapon reload. Separate vehicles count; seat permissions still apply.', '实际换座、切换可用武器并完成车载装填；可在不同载具分次完成，仍受座位权限限制。', [group('seat', 'seat_change'), group('weapon', 'weapon_change'), group('reload', 'vehicle_reload')])
    add('vehicles/resupply', 'vehicles/crew', 'vehicle_charging_station', 'Mobile Supply', '移动补给', 'Add inventory supplies to a vehicle and actually restore vehicle energy at a charging station.', '向载具库存实际补入补给，并在充电站实际恢复载具能量。', [group('supply', 'vehicle_supply'), group('charge', 'vehicle_charge')])
    add('vehicles/repair', 'vehicles/crew', 'repair_tool', 'Field Repairs', '战地抢修', 'Actually restore damaged hull health and a damaged engine, running gear or turret component. Separate vehicles count.', '实际修复受损车体和发动机、行走机构或炮塔部件；可分不同载具完成。', [group('hull', 'vehicle_hull_repair'), group('component', 'vehicle_component_repair')])
    add('vehicles/countermeasures', 'vehicles/resupply', 'flare', 'Shake the Lock', '摆脱锁定', 'Deploy a vehicle countermeasure and make a hostile missile lose tracking or switch to a decoy. Deployment alone is insufficient.', '释放载具反制，并让敌方导弹实际失去跟踪或转向诱饵；仅释放反制不足以完成。', [group('deploy', 'decoy'), group('defence', 'missile_defence')], xp=25, frame='goal')
    one('vehicles/recover', 'vehicles/crew', 'crowbar', 'Return to Camp', '收兵回营', 'Recover any usable vehicle with a crowbar.', '用撬棍实际回收任意可用载具。', 'pack')
    one('exploration/armada', 'vehicles/root', 'minecraft:end_stone', 'Sky Ship Expedition', '空舰远征', 'Explore the Sky Ship Armada in the End. This opens the fleet-loot, Bound Terror Phantom and follow-up Raid route. Prepare ammunition, protection and air defence.', '进入末地Sky Ship空舰舰队，展开舰队战利品、束缚恐惧幻翼与后续Raid路线；提前准备弹药、防护与防空装备。', xp=25, frame='goal')
    one('exploration/loot', 'exploration/armada', 'minecraft:chest', 'Fleet Relics', '舰队遗珍', 'Open a chest while inside the End Ship Armada.', '在末地舰队结构内实际打开箱子。')
    one('exploration/guardian', 'exploration/armada', 'igla_9k38', 'Break the Bonds', '打破束缚', 'Defeat the Bound Terror Phantom guardian with credited combat damage. Be ready for the aerial follow-up.', '击败束缚恐惧幻翼守护者并获得战斗归属，准备迎战后续空袭。', xp=25, frame='goal')
    one('exploration/after_battle', 'exploration/guardian', 'igla_9k38', 'After the Storm', '风暴之后', 'Win the guardian-triggered Terror raid under its existing participation rules. Remain in battle range until all waves are defeated.', '按现有参与资格赢得守护者触发的恐惧袭击；留在战区直至击退全部波次。', xp=25, frame='goal')
    one('exploration/free_terror', 'exploration/after_battle', 'igla_9k38', 'The Roaming Terror', '游荡的恐惧', 'Defeat a free-roaming Terror Phantom. Its conversion is disabled by default and requires server configuration.', '击败自由游荡恐惧幻翼；其转化默认禁用，需要服务器配置启用。', optional=True)
    for path, parent, vehicle, en, zh, weapons, desc, zh_desc in [
        ('bmp2_assault', 'vehicles/workshop', 'bmp2', 'Armoured Assault', '装甲突击', [['jeg:vehicle_30mm_cannon'], ['jeg:vehicle_bmp2_missile']], 'Drive BMP2 at least 20 blocks, then land enemy hits with its cannon and anti-tank missile.', '实际驾驶BMP2至少20格，并分别用机炮和反坦克导弹有效命中敌人。'),
        ('lav_firepower', 'vehicles/bmp2_assault', 'lav150', 'Wheeled Firepower', '轮式火力', [['jeg:vehicle_20mm_cannon']], 'Drive LAV150 at least 20 blocks and land an enemy hit with its cannon.', '实际驾驶LAV150至少20格，并用机炮有效命中敌人。'),
        ('river_push', 'vehicles/bmp2_assault', 'speedboat', 'Push Along the River', '沿河突进', [['jeg:vehicle_coax_machine_gun']], 'Pilot a Speedboat at least 20 blocks in water and land an enemy hit with its machine gun.', '在水中实际驾驶Speedboat至少20格，并用机枪有效命中敌人。'),
        ('low_support', 'vehicles/workshop', 'ah6', 'Low-level Support', '贴地支援', [['jeg:light_machine_gun'], ['jeg:vehicle_70mm_rocket']], 'Control AH-6 through a 10-block climb and safe landing, and land enemy hits with its machine gun and rockets.', '实际操控AH-6升高至少10格后安全着陆，并分别用机枪和火箭有效命中敌人。'),
        ('heavy_hunter', 'vehicles/low_support', 'mi28', 'Heavy Hunter', '重装猎手', [['jeg:vehicle_80mm_rocket'], ['jeg:vehicle_9m120_driver_missile', 'jeg:vehicle_9m336_missile', 'jeg:vehicle_9m120_passenger_missile', 'jeg:vehicle_kh39_missile']], 'Control MI-28 through a 10-block climb and safe landing, and land enemy hits with its rockets and any guided missile.', '实际操控MI-28升高至少10格后安全着陆，并用火箭及任意制导导弹有效命中敌人。'),
    ]:
        hits = [group('hit_' + str(index), 'vehicle_hit', ['jeg:' + vehicle + '/' + weapon for weapon in alternatives]) for index, alternatives in enumerate(weapons)]
        add('vehicles/' + path, parent, 'vehicle_assembling_table', en, zh, desc + ' Separate operations count. Driving requires driver input; boarding and teleporting do not count. Helicopter landing must cause no hull damage at no more than 0.4 blocks/tick downward.', zh_desc + ' 各操作可分次完成，须在驾驶席实际操控；登乘或传送不算驾驶。直升机着陆须无车体损伤且每tick下降不超过0.4格。', [group('drive', 'drive', ['jeg:' + vehicle])] + hits, xp=25, frame='goal')

    core = [row['id'] for row in objectives if not row['optional'] and not row['id'].endswith('/root')]
    for tab, path, parent, icon, en, zh in [
        ('training', 'training/qualified', 'training/kill', 'combat_pistol', 'Qualified Combatant', '合格战斗员'),
        ('battle', 'battle/veteran', 'battle/raid', 'infantry_rifle', 'Front-line Veteran', '前线老兵'),
        ('special', 'special/expert', 'special/defuse', 'monitor', 'Special Operations Expert', '特战专家'),
        ('vehicles', 'vehicles/expedition_complete', 'exploration/after_battle', 'vehicle_assembling_table', 'Expedition Complete', '远征归来'),
    ]:
        members = [row['id'] for row in objectives if row['chapter'] == tab and row['id'] in core]
        completion['jeg:guide/' + path] = members
        add(path, parent, icon, en, zh, 'Complete this chapter\u2019s core field objectives. Independent challenges do not count.', '完成本章核心实战目标；独立挑战不计入。', [('complete', [{'trigger': 'minecraft:tick', 'conditions': {'player': {'type_specific': {'type': 'minecraft:player', 'advancements': {ident: True for ident in members}}}}}])], xp=25, frame='goal')
    chapters = list(completion)
    completion['jeg:guide/training/complete'] = chapters
    add('training/complete', 'training/qualified', 'holy_shotgun', 'Battle Hardened', '久经沙场', 'Complete all four chapters: combat roles, systems, five vehicle missions and the End expedition. Equivalent equipment is your choice; independent challenges are excluded.', '完成四章核心目标，掌握作战角色与系统，完成五种载具任务及末地远征；同类装备任选，独立挑战不计入。', [('mastery', [{'trigger': 'minecraft:tick', 'conditions': {'player': {'type_specific': {'type': 'minecraft:player', 'advancements': {ident: True for ident in chapters}}}}}])], xp=250, frame='challenge')

    # Keep vanilla save identifiers and criterion names, with no live listeners, display or rewards.
    archived = sorted(set(legacy) - set(nodes))
    for path in archived:
        nodes[path] = {'criteria': {key: {'trigger': 'minecraft:impossible'} for key in legacy[path]['criteria']}, 'requirements': legacy[path]['requirements'], 'sends_telemetry_event': False}
    for path in (resource / 'data/jeg/advancement/recipes').rglob('*.json'):
        node = read_json(path)
        assert all(ident.startswith('jeg:') and ident[4:] in recipes for ident in node.get('rewards', {}).get('recipes', [])), path
        nodes['recipes/' + path.relative_to(resource / 'data/jeg/advancement/recipes').with_suffix('').as_posix()] = node

    def proves(source, target):
        if source['trigger'] != target['trigger'] or target['trigger'] == 'minecraft:tick':
            return False
        old, new = source.get('conditions', {}), target.get('conditions', {})
        # An old specific action proves a new unrestricted action; never the reverse.
        return all(old.get(key) == value for key, value in new.items())

    migration = {'criteria': {}, 'completion': completion}
    for row in objectives:
        for key, criterion in row['criteria'].items():
            sources = [['jeg:' + path, old_key] for path, old in legacy.items() for old_key, source in old['criteria'].items() if proves(source, criterion)]
            if sources:
                migration['criteria'].setdefault(row['id'], {})[key] = sources

    registered = set(re.findall(r'(?:REGISTER.register|registerAttachment)\(\s*"([^\"]+)"', item_source)) | set(guns) | set(ammo)
    if modern:
        registered.update(harnesses)
    coverage = {}
    roles = {'rifle': 'arsenal/category_rifle', 'pistol': 'arsenal/category_pistol', 'smg': 'battle/close_quarters', 'shotgun': 'battle/close_quarters', 'sniper': 'battle/precision', 'lmg': 'battle/support', 'heavy': 'battle/support', 'special': 'arsenal/use_flare_gun'}
    special = {'grenade': 'special/fuse', 'stun_grenade': 'special/stun', 'smoke_grenade': 'special/smoke', 'molotov_cocktail': 'special/molotov', 'water_bomb': 'special/water_response', 'drone': 'special/remote_control', 'monitor': 'special/remote_control', 'c4_bomb': 'special/place_c4', 'detonator': 'special/remote_ambush', 'defuser': 'special/defuse', 'claymore_mine': 'special/ambush', 'tm_62': 'special/ambush', 'c4_vest': 'exploration/c4_crew', 'javelin': 'special/guidance', 'igla_9k38': 'special/guidance'}
    for item in sorted(registered):
        kind, reason = 'alternative', 'Equivalent equipment; individual collection is not required'
        if item in EXCLUDED or item.endswith('_spawn_egg') or item == 'gunsmith_manual':
            coverage[item] = {'kind': 'excluded', 'reason': EXCLUDED.get(item, 'Developer/creative-only item')}
            continue
        if item in ('extended_mag', 'drum_mag'):
            kind, target, reason = 'reference', 'training/logistics', 'Legacy capacity attachment requires magazineFeed disabled; the core task uses physical extended/drum magazines'
        elif item in special:
            target = special[item]
        elif item in guns:
            target = roles[categories.get(item, 'special')]
        elif item in slots:
            target = 'training/electronics' if item in ('flashlight', 'laser_pointer') else 'training/loadout'
        elif item.endswith('spray_can'):
            target = 'training/signature'
        elif item.endswith('badge'):
            target = 'gunsmith/kill_effect'
        elif item.startswith('bulletproof_'):
            target = 'training/protection'
        elif item.startswith('armored_joy_harness_'):
            target = 'training/airlift'
        elif item in magazines:
            target = 'training/logistics' if 'extended' in item or 'drum' in item else 'training/ammunition_ready'
        elif item in ('repair_kit', 'coolant', 'enhanced_coolant'):
            target = 'training/maintenance'
        elif item == 'repair_tool':
            target = 'vehicles/repair'
        elif item == 'magazine_loader':
            target = 'training/logistics'
        elif item in ('vehicle_container', 'vehicle_assembling_table'):
            target = 'vehicles/workshop'
        elif item == 'vehicle_charging_station':
            target = 'vehicles/resupply'
        elif item == 'crowbar':
            target = 'vehicles/recover'
        else:
            kind, target, reason = 'reference', 'vehicles/crew' if 'missile' in item or item in ('small_rocket', 'small_shell', 'autocannon_shell') else 'training/ready', 'Supply or component; use its loaded recipe and compatible weapon tooltip, without an acquisition checklist'
        coverage[item] = {'kind': kind, 'objectives': ['jeg:guide/' + target], 'reason': reason}
    catalogue = {'tabs': list(TABS), 'visible_count': len(objectives), 'core_count': len(core), 'vehicles': list(VEHICLES), 'weapons': guns, 'attachments': sorted(slots), 'magazines': magazines, 'ammo': ammo, 'objectives': objectives, 'mastery': core, 'chapter_completion': completion, 'archived': ['jeg:' + path for path in archived], 'item_coverage': coverage}
    return nodes, languages, catalogue, migration


def validate(nodes, languages, catalogue, migration):
    visible = {path: node for path, node in nodes.items() if 'display' in node}
    modern = any(item.startswith('armored_joy_harness_') for item in catalogue['item_coverage'])
    assert len(visible) == (73 if modern else 72), len(visible)
    assert len(catalogue['mastery']) == (54 if modern else 53)
    assert sorted(path.split('/')[1] for path, node in visible.items() if 'parent' not in node) == sorted(TABS)
    assert max(Counter(node.get('parent') for node in visible.values() if 'parent' in node).values()) <= 3
    for path, node in visible.items():
        for field in ('title', 'description'):
            assert all(node['display'][field]['translate'] in language for language in languages.values()), path
        assert node['requirements'] and all(group and all(key in node['criteria'] for key in group) for group in node['requirements']), path
        seen, cursor = set(), path
        while True:
            assert cursor not in seen, path
            seen.add(cursor)
            parent = nodes[cursor].get('parent')
            if not parent:
                break
            assert parent.startswith('jeg:') and parent[4:] in visible, parent
            cursor = parent[4:]
        assert len(seen) <= 9, (path, len(seen))
    optional = {row['id'] for row in catalogue['objectives'] if row['optional']}
    assert not (set(catalogue['mastery']) & (optional | set(catalogue['archived'])))
    assert catalogue['vehicles'] == list(VEHICLES)
    for row in catalogue['item_coverage'].values():
        assert row['kind'] in ('alternative', 'reference', 'excluded')
        assert all(ident[4:] in visible for ident in row.get('objectives', []))
    for target, criteria in migration['criteria'].items():
        assert target[4:] in visible
        for criterion, sources in criteria.items():
            assert criterion in nodes[target[4:]]['criteria']
            assert all(source[4:] in nodes and key in nodes[source[4:]]['criteria'] for source, key in sources)
    for ident in catalogue['archived']:
        node = nodes[ident[4:]]
        assert not any(key in node for key in ('display', 'parent', 'rewards'))
        assert all(value['trigger'] == 'minecraft:impossible' for value in node['criteria'].values())


def run(module, check):
    nodes, languages, catalogue, migration = generate(module)
    validate(nodes, languages, catalogue, migration)
    source = '\n'.join(path.read_text(encoding='utf-8') for path in (module / 'src/main/java').rglob('*.java'))
    for row in catalogue['objectives']:
        for criterion in row['criteria'].values():
            if criterion['trigger'] == 'jeg:gameplay_action':
                action = criterion['conditions']['action']
                assert '"' + action + '"' in source, f'Missing gameplay action: {action}'
    resources = module / 'src/main/resources'
    expected = {resources / f'data/jeg/advancement/{key}.json': value for key, value in nodes.items()}
    expected.update({resources / f'assets/jeg/lang/{locale}.json': value for locale, value in languages.items()})
    expected[resources / 'data/jeg/advancement_migration.json'] = migration
    expected[module / 'docs/advancement_coverage.json'] = catalogue
    for path, value in expected.items():
        if check:
            assert path.exists() and read_json(path) == value, f'Stale generated file: {path}'
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    assert set((resources / 'data/jeg/advancement/guide').rglob('*.json')) <= set(expected), 'Unmanaged guide resource'
    print(f'{module.name}: {catalogue["visible_count"]} visible nodes, {catalogue["core_count"]} core goals, 4 tabs, at most 3 children; legacy records frozen')


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
    for target in targets:
        run(target, args.check)
