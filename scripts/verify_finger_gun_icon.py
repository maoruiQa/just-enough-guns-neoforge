"""Check the Finger Gun GUI resource route and original JEG display transform."""
import json
from pathlib import Path

module = Path(__file__).resolve().parents[1]
assets = module / 'src/main/resources/assets/jeg'
if module.name.endswith('1.21.1'):
    model_path = assets / 'models/item/finger_gun.json'
else:
    item = json.loads((assets / 'items/finger_gun.json').read_text())['model']
    gui = [case['model'] for case in item['cases'] if 'gui' in case['when']]
    assert len(gui) == 1 and gui[0]['type'] == 'minecraft:special'
    assert gui[0]['model']['type'] == 'geckolib:geckolib'
    namespace, path = gui[0]['base'].split(':')
    assert namespace == 'jeg'
    model_path = assets / 'models' / (path + '.json')
    assert item['fallback'] == {'type': 'minecraft:model', 'model': 'jeg:item/finger_gun'}

model = json.loads(model_path.read_text())
assert model['parent'] == 'builtin/entity'
assert model['display']['gui'] == {
    'rotation': [90, -45, 90], 'translation': [10, 5, 0], 'scale': [1.5, 1.5, 1.5]
}
geo_root = 'geo' if module.name.endswith('1.21.1') else 'geckolib/models'
geometry = json.loads((assets / geo_root / 'item/gun/finger_gun.geo.json').read_text())['minecraft:geometry'][0]
assert {'left_arm', 'right_arm'} <= {bone['name'] for bone in geometry['bones']}
print(module.name + ': Finger Gun GUI resource check passed')
