"""Writes the commands that build the forest homestead of the Panoramas page in a fresh flat world (see world_harness/).

The camera stands in the middle of a forest clearing (x 0, z 0) and looks north at a log cabin. Around it: a woodshed with a
chopping block (east), a fire pit and a pond (south-east), raised garden beds (south), a sheep pen with a small barn (south-west),
a chicken coop with its run (west) and a well with beehives (north-west). A mixed forest on gentle hills closes the clearing.

Usage: python3 build_homestead.py <commands.txt> [map.png]   # map.png: a top-down map of the build, to check the layout
"""
import math
import random
import sys

GROUND = -61          # the grass layer of the flat world
Y0 = GROUND + 1       # the first block above the ground
R = random.Random(11)


class Scene:

    def __init__(self):
        self.blocks = {}          # (x, y, z) -> block state
        self.late = {}            # blocks that need their neighbors first, like hanging lanterns
        self.features = []        # (feature, x, y, z)
        self.entities = []        # summon commands
        self.setup = []
        self.height = {}          # (x, z) -> height of the ground above GROUND
        self.reserved = set()     # (x, z) columns that natural things keep away from

    def set(self, x, y, z, state, late=False):
        (self.blocks if late else self.late).pop((x, y, z), None)
        (self.late if late else self.blocks)[(x, y, z)] = state

    def get(self, x, y, z):
        return self.blocks.get((x, y, z), self.late.get((x, y, z)))

    def fill(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, state)

    def reserve(self, x1, z1, x2, z2, margin=0):
        for x in range(min(x1, x2) - margin, max(x1, x2) + margin + 1):
            for z in range(min(z1, z2) - margin, max(z1, z2) + margin + 1):
                self.reserved.add((x, z))

    def ground_y(self, x, z):
        return GROUND + self.height.get((x, z), 0)

    def summon(self, entity, x, y, z, nbt=''):
        self.entities.append('summon %s %.2f %.2f %.2f %s' % (entity, x, y, z, nbt))


def pick(options):
    """Weighted pick: [(state, weight), ...]."""
    total = sum(w for _, w in options)
    r = R.random() * total
    for state, w in options:
        r -= w
        if r <= 0:
            return state
    return options[-1][0]


STONE_LOW = [('minecraft:mossy_cobblestone', 5), ('minecraft:cobblestone', 4), ('minecraft:tuff', 1)]
STONE_MID = [('minecraft:cobblestone', 7), ('minecraft:mossy_cobblestone', 3), ('minecraft:stone', 1), ('minecraft:tuff', 1)]
LOGS_MIX = [('minecraft:oak_log', 5), ('minecraft:spruce_log', 4), ('minecraft:jungle_log', 1)]


def roof_y(z):
    return -49 - abs(z + 16)


# ---------------------------------------------------------------- the cabin (north)

def cabin(s):
    x1, x2, zf, zb = -6, 6, -12, -20
    s.reserve(-8, -23, 9, -8)
    # Stone plinth and floor
    for x in range(x1, x2 + 1):
        for z in range(zb, zf + 1):
            edge = x in (x1, x2) or z in (zb, zf)
            s.set(x, Y0, z, pick(STONE_LOW) if edge else 'minecraft:spruce_planks')
    # Log walls with notched corners: front and back logs on even rows, side logs on odd rows reach past the corners
    for k in range(6):
        y = -59 + k
        if k % 2 == 0:
            for x in range(x1 - 1, x2 + 2):
                s.set(x, y, zf, 'minecraft:spruce_log[axis=x]')
                s.set(x, y, zb, 'minecraft:spruce_log[axis=x]')
            for z in range(zb + 1, zf):
                s.set(x1, y, z, 'minecraft:spruce_log[axis=z]')
                s.set(x2, y, z, 'minecraft:spruce_log[axis=z]')
        else:
            for z in range(zb - 1, zf + 2):
                s.set(x1, y, z, 'minecraft:spruce_log[axis=z]')
                s.set(x2, y, z, 'minecraft:spruce_log[axis=z]')
            for x in range(x1 + 1, x2):
                s.set(x, y, zf, 'minecraft:spruce_log[axis=x]')
                s.set(x, y, zb, 'minecraft:spruce_log[axis=x]')
    # Interior air
    s.fill(x1 + 1, -59, zb + 1, x2 - 1, -54, zf - 1, 'minecraft:air')
    # Door with a frame
    for y in (-59, -58, -57):
        s.set(-1, y, zf, 'minecraft:stripped_spruce_log[axis=y]')
        s.set(1, y, zf, 'minecraft:stripped_spruce_log[axis=y]')
    s.set(0, -57, zf, 'minecraft:stripped_spruce_log[axis=x]')
    s.set(0, -59, zf, 'minecraft:spruce_door[facing=south,half=lower,hinge=left,open=false]')
    s.set(0, -58, zf, 'minecraft:spruce_door[facing=south,half=upper,hinge=left,open=false]')
    # Front windows with frames, shutters and panes
    for wx in (-4, 3):
        for y in (-58, -57):
            s.set(wx - 1, y, zf, 'minecraft:stripped_spruce_log[axis=y]')
            s.set(wx + 2, y, zf, 'minecraft:stripped_spruce_log[axis=y]')
            s.set(wx, y, zf, 'minecraft:glass_pane')
            s.set(wx + 1, y, zf, 'minecraft:glass_pane')
            s.set(wx - 1, y, zf + 1, 'minecraft:spruce_trapdoor[facing=south,open=true,half=bottom]')
            s.set(wx + 2, y, zf + 1, 'minecraft:spruce_trapdoor[facing=south,open=true,half=bottom]')
        for x in range(wx - 1, wx + 3):
            s.set(x, -56, zf, 'minecraft:stripped_spruce_log[axis=x]')
        s.set(wx, -59, zf, 'minecraft:stripped_spruce_log[axis=x]')
        s.set(wx + 1, -59, zf, 'minecraft:stripped_spruce_log[axis=x]')
    # Side windows (west and east), with shutters
    for sx, out, face in ((x1, x1 - 1, 'west'), (x2, x2 + 1, 'east')):
        for y in (-58, -57):
            s.set(sx, y, -13 - 1, 'minecraft:glass_pane')
            s.set(sx, y, -15 + 0, 'minecraft:stripped_spruce_log[axis=y]')
            s.set(sx, y, -13 + 0, 'minecraft:stripped_spruce_log[axis=y]')
            s.set(out, y, -15, 'minecraft:spruce_trapdoor[facing=%s,open=true,half=bottom]' % face)
            s.set(out, y, -13, 'minecraft:spruce_trapdoor[facing=%s,open=true,half=bottom]' % face)
        s.set(sx, -56, -14, 'minecraft:stripped_spruce_log[axis=z]')
    # Gables with a loft window
    for gx in (x1, x2):
        for z in range(zb + 1, zf):
            for y in range(-53, roof_y(z)):
                s.set(gx, y, z, 'minecraft:spruce_planks')
        for z in range(zb + 1, zf):
            s.set(gx, -54, z, 'minecraft:stripped_spruce_log[axis=z]')
        s.set(gx, -52, -16, 'minecraft:glass_pane')
        s.set(gx, -51, -16, 'minecraft:glass_pane')
    # Roof: a long front slope that covers the porch, a short back slope
    for x in range(x1 - 1, x2 + 2):
        for z in range(-21, -8):
            if z == -16:
                continue
            state = 'minecraft:dark_oak_stairs[facing=%s,half=bottom,shape=straight]' % ('north' if z > -16 else 'south')
            if z == -9:
                state = 'minecraft:spruce_stairs[facing=north,half=bottom,shape=straight]'
            s.set(x, roof_y(z), z, state)
        s.set(x, -50, -16, 'minecraft:dark_oak_planks')
        s.set(x, -49, -16, 'minecraft:stripped_dark_oak_log[axis=x]')
        # The roof is solid: fill under the stairs inside the cabin
        s.set(x, -55, -21, 'minecraft:spruce_stairs[facing=north,half=top]')
    # Trim under the gable overhangs
    for z in range(-21, -8):
        if z == -16:
            continue
        for gx, face in ((x1 - 1, 'east'), (x2 + 1, 'west')):
            if s.get(gx, roof_y(z) - 1, z) is None:
                s.set(gx, roof_y(z) - 1, z, 'minecraft:spruce_stairs[facing=%s,half=top]' % face)
    # Porch: deck, posts, beam, railing and steps
    for x in range(x1, x2 + 1):
        for z in (-11, -10, -9):
            s.set(x, Y0, z, 'minecraft:spruce_planks' if z != -9 else 'minecraft:stripped_spruce_log[axis=x]')
        s.set(x, -57, -9, 'minecraft:stripped_spruce_log[axis=x]')
    for px in (x1, -2, 2, x2):
        s.set(px, -59, -9, 'minecraft:spruce_log[axis=y]')
        s.set(px, -58, -9, 'minecraft:spruce_log[axis=y]')
    for x in list(range(x1 + 1, -2)) + list(range(3, x2)):
        s.set(x, -59, -9, 'minecraft:spruce_fence')
    for x in (-1, 0, 1):
        s.set(x, Y0, -8, 'minecraft:spruce_stairs[facing=north,half=bottom]')
    s.set(x1, -59, -10, 'minecraft:spruce_fence')
    s.set(x2, -59, -10, 'minecraft:spruce_fence')
    s.set(-4, -58, -9, 'minecraft:lantern[hanging=true]', late=True)
    s.set(4, -58, -9, 'minecraft:lantern[hanging=true]', late=True)
    # Two dormers on the front slope
    for a in (-5, 3):
        c = a + 1
        for z in (-13,):
            s.set(c, -52, z, 'minecraft:air')
        for y in (-53, -52):
            s.set(a, y, -12, 'minecraft:spruce_planks')
            s.set(a + 2, y, -12, 'minecraft:spruce_planks')
            s.set(c, y, -12, 'minecraft:glass_pane')
            s.set(a, y, -13, 'minecraft:spruce_planks')
            s.set(a + 2, y, -13, 'minecraft:spruce_planks')
        s.set(c, -51, -12, 'minecraft:spruce_planks')
        s.set(c, -54, -11, 'minecraft:spruce_trapdoor[facing=south,open=false,half=top]')
        for z in (-11, -12, -13, -14):
            s.set(a, -51, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom,shape=straight]')
            s.set(a + 2, -51, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight]')
            s.set(c, -50, z, 'minecraft:dark_oak_slab[type=bottom]')
            if z != -11:
                s.set(c, -51, z, 'minecraft:dark_oak_planks')
        s.set(a, -52, -11, 'minecraft:spruce_stairs[facing=east,half=top]')
        s.set(a + 2, -52, -11, 'minecraft:spruce_stairs[facing=west,half=top]')
    # A small gable over the steps, sitting on the long front slope
    for z in (-9, -10, -11):
        for dx, face, y in ((-2, 'east', -55), (-1, 'east', -54), (1, 'west', -54), (2, 'west', -55)):
            s.set(dx, y, z, 'minecraft:dark_oak_stairs[facing=%s,half=bottom,shape=straight]' % face)
        s.set(0, -53, z, 'minecraft:dark_oak_slab[type=bottom]')
        s.set(0, -54, z, 'minecraft:dark_oak_planks')
    s.set(0, -53, -8, 'minecraft:dark_oak_slab[type=bottom]')
    for dx, face, y in ((-2, 'east', -55), (-1, 'east', -54), (1, 'west', -54), (2, 'west', -55)):
        s.set(dx, y, -8, 'minecraft:dark_oak_stairs[facing=%s,half=bottom,shape=straight]' % face)
    for dx in (-1, 0, 1):
        s.set(dx, -56, -9, 'minecraft:stripped_spruce_log[axis=x]')
    s.set(0, -55, -9, 'minecraft:stripped_spruce_log[axis=z]')
    s.set(-1, -55, -9, 'minecraft:spruce_planks')
    s.set(1, -55, -9, 'minecraft:spruce_planks')
    s.set(-1, -56, -8, 'minecraft:spruce_stairs[facing=east,half=top]')
    s.set(1, -56, -8, 'minecraft:spruce_stairs[facing=west,half=top]')
    s.set(0, -54, -8, 'minecraft:dark_oak_planks')
    s.set(0, -55, -8, 'minecraft:stripped_spruce_log[axis=z]')
    s.set(0, -56, -8, 'minecraft:lantern[hanging=true]', late=True)
    # Flower beds along the porch, shrubs at the corners
    for x in list(range(-6, -1)) + list(range(2, 7)):
        s.set(x, GROUND, -8, 'minecraft:podzol' if R.random() < 0.6 else 'minecraft:rooted_dirt')
        plant = pick([('minecraft:fern', 3), ('minecraft:red_tulip', 1), ('minecraft:allium', 1), ('minecraft:oxeye_daisy', 1),
                      ('minecraft:lily_of_the_valley', 1), ('minecraft:azalea', 1), ('minecraft:flowering_azalea', 2), ('minecraft:sweet_berry_bush[age=2]', 1)])
        s.set(x, Y0, -8, plant, late=True)
    for x, z in ((-7, -12), (-7, -11), (7, -12), (-8, -13), (-7, -20), (-7, -19), (7, -20)):
        s.set(x, Y0, z, 'minecraft:oak_leaves[persistent=true]')
        if R.random() < 0.5:
            s.set(x, Y0 + 1, z, 'minecraft:oak_leaves[persistent=true]')
    # Bench under the west window, barrels with plants under the east window
    s.set(-4, -59, -11, 'minecraft:spruce_stairs[facing=north,half=bottom]')
    s.set(-3, -59, -11, 'minecraft:spruce_stairs[facing=north,half=bottom]')
    s.set(-5, -59, -11, 'minecraft:spruce_trapdoor[facing=west,open=true,half=bottom]')
    s.set(-2, -59, -11, 'minecraft:spruce_trapdoor[facing=east,open=true,half=bottom]')
    s.set(3, -59, -11, 'minecraft:barrel[facing=up]')
    s.set(4, -59, -11, 'minecraft:barrel[facing=up]')
    s.set(3, -58, -11, 'minecraft:potted_fern', late=True)
    s.set(4, -58, -11, 'minecraft:potted_red_tulip', late=True)
    s.set(5, -59, -10, 'minecraft:oak_log[axis=x]')
    s.set(5, -59, -11, 'minecraft:spruce_log[axis=x]')
    s.set(5, -58, -11, 'minecraft:birch_log[axis=x]')
    s.set(0, -59, -11, 'minecraft:brown_carpet', late=True)
    s.set(0, -59, -10, 'minecraft:brown_carpet', late=True)
    s.set(-5, -59, -10, 'minecraft:potted_azure_bluet', late=True)
    # Something warm inside, behind the windows
    s.set(-4, -59, -13, 'minecraft:barrel[facing=up]')
    s.set(-4, -58, -13, 'minecraft:lantern[hanging=false]', late=True)
    s.set(4, -59, -13, 'minecraft:bookshelf')
    s.set(5, -59, -13, 'minecraft:bookshelf')
    s.set(4, -58, -13, 'minecraft:lantern[hanging=false]', late=True)
    s.set(-5, -59, -19, 'minecraft:crafting_table')
    # Chimney on the east gable, with smoke from a campfire on hay
    for y in range(Y0, -46):
        for z in (-17, -16, -15):
            s.set(7, y, z, pick(STONE_LOW if y < -57 else STONE_MID))
            if y <= -55:
                s.set(8, y, z, pick(STONE_LOW if y < -57 else STONE_MID))
    for z in (-17, -16, -15):
        s.set(8, -54, z, 'minecraft:cobblestone_stairs[facing=west,half=bottom]')
        s.set(7, -47, z, 'minecraft:stone_bricks')
    s.set(7, -47, -16, 'minecraft:hay_block')
    s.set(7, -46, -17, 'minecraft:cobblestone_wall')
    s.set(7, -46, -15, 'minecraft:cobblestone_wall')
    s.set(7, -46, -16, 'minecraft:campfire[lit=true,signal_fire=true,facing=west]', late=True)
    # A sloped stone plinth along the sides and the back
    for z in range(-19, -12):
        s.set(-7, Y0, z, 'minecraft:%s_stairs[facing=east,half=bottom]' % R.choice(['cobblestone', 'mossy_cobblestone']))
        if z not in (-17, -16, -15):
            s.set(7, Y0, z, 'minecraft:%s_stairs[facing=west,half=bottom]' % R.choice(['cobblestone', 'mossy_cobblestone']))
    for x in range(-5, 6):
        s.set(x, Y0, -21, 'minecraft:%s_stairs[facing=south,half=bottom]' % R.choice(['cobblestone', 'mossy_cobblestone']))
    # Rain barrel at the front corner
    s.set(8, Y0, -11, 'minecraft:water_cauldron[level=3]')
    s.set(8, Y0, -10, 'minecraft:spruce_trapdoor[facing=south,open=false,half=bottom]')
    s.summon('minecraft:cat', -1.3, -59.0, -10.4, '{variant:"minecraft:red",Sitting:1b,NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[160f,0f]}')


# ---------------------------------------------------------------- woodshed and chopping block (east)

def woodshed(s):
    s.reserve(9, -6, 19, 9)
    zs, ze = -4, 2
    for x in range(13, 16):
        for z in range(zs, ze + 1):
            s.set(x, Y0, z, 'minecraft:spruce_slab[type=top]')
    # Three posts in front, two in the back, railed sides
    for z in (zs, -1, ze):
        for y in range(-59, -55):
            s.set(13, y, z, 'minecraft:spruce_log[axis=y]')
    for z in (zs, ze):
        for y in range(-59, -57):
            s.set(15, y, z, 'minecraft:spruce_log[axis=y]')
        s.set(14, -59, z, 'minecraft:spruce_fence')
        s.set(14, -58, z, 'minecraft:spruce_fence')
    for z in range(zs, ze + 1):
        if z not in (zs, -1, ze):
            s.set(13, -56, z, 'minecraft:stripped_spruce_log[axis=z]')
    for z in range(zs - 1, ze + 2):
        s.set(12, -55, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight]')
        s.set(13, -55, z, 'minecraft:dark_oak_slab[type=top]')
        s.set(14, -56, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight]')
        s.set(15, -57, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight]')
        s.set(16, -58, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom,shape=straight]')
    s.set(13, -57, 1, 'minecraft:lantern[hanging=true]', late=True)
    # The woodpile: two deep, uneven, the top logs partly lying lengthwise
    for z in range(zs + 1, ze):
        back_top = -58
        front_top = R.choice([-59, -58, -58, -57, -57])
        for x, top in ((15, back_top), (14, front_top)):
            for y in range(-59, top + 1):
                lengthwise = (y == top) and (y > -59) and R.random() < 0.35
                wood = 'minecraft:spruce_log' if R.random() < 0.8 else 'minecraft:oak_log'
                s.set(x, y, z, wood + ('[axis=z]' if lengthwise else '[axis=x]'))
    # Chopping block with an axe and split wood around it
    s.set(10, Y0, -1, 'minecraft:oak_log[axis=y]')
    s.entities.append('summon minecraft:item_display 10.50 -58.95 -0.50 {item:{id:"minecraft:iron_axe",count:1},'
                      'transformation:{left_rotation:[0f,0.3826834f,0f,0.9238795f],right_rotation:[0f,0f,0.9238795f,0.3826834f],'
                      'translation:[0f,0.25f,0f],scale:[0.85f,0.85f,0.85f]}}')
    for i in range(9):
        a = R.random() * math.tau
        d = 0.9 + R.random() * 1.5
        x = 10.5 + math.cos(a) * d
        z = -0.5 + math.sin(a) * d
        yaw = R.random() * 360
        q = (0.0, math.sin(math.radians(yaw) / 2), 0.0, math.cos(math.radians(yaw) / 2))
        wood = R.choice(['stripped_oak_log', 'stripped_birch_log', 'oak_log', 'stripped_spruce_log'])
        s.entities.append('summon minecraft:block_display %.2f -60.00 %.2f {block_state:{Name:"minecraft:%s",Properties:{axis:"z"}},'
                          'transformation:{left_rotation:[%.4ff,%.4ff,%.4ff,%.4ff],right_rotation:[0f,0f,0f,1f],translation:[-0.11f,0f,-0.25f],'
                          'scale:[0.22f,0.22f,0.5f]}}' % (x, z, wood, *q))
    s.set(9, Y0, -3, 'minecraft:grindstone[face=floor,facing=north]')
    # Sawhorse with a log on it
    s.set(10, Y0, 3, 'minecraft:spruce_fence')
    s.set(12, Y0, 3, 'minecraft:spruce_fence')
    for x in (10, 11, 12):
        s.set(x, -59, 3, 'minecraft:spruce_log[axis=x]')
    # A firewood rick between two cribs, covered with slabs
    for z in range(5, 9):
        top = R.choice([-59, -59, -58])
        for y in range(-60, top + 1):
            s.set(13, y, z, 'minecraft:spruce_log[axis=x]' if R.random() < 0.75 else 'minecraft:oak_log[axis=x]')
        if R.random() < 0.7:
            s.set(13, top + 1, z, 'minecraft:spruce_slab[type=bottom]')
    for z in (4, 9):
        for i, y in enumerate((-60, -59)):
            s.set(13, y, z, 'minecraft:spruce_log[axis=%s]' % ('z' if i % 2 == 0 else 'x'))
    # Long logs waiting to get cut
    for z in range(-3, 2):
        s.set(18, Y0, z, 'minecraft:spruce_log[axis=z]')
        s.set(17, Y0, z, 'minecraft:spruce_log[axis=z]')
    for z in range(-2, 1):
        s.set(18, -59, z, 'minecraft:spruce_log[axis=z]')
    # Lantern on a post
    s.set(12, Y0, -5, 'minecraft:spruce_fence')
    s.set(12, -59, -5, 'minecraft:spruce_fence')
    s.set(12, -58, -5, 'minecraft:lantern[hanging=false]', late=True)


# ---------------------------------------------------------------- chicken coop and run (west)

def chickens(s):
    s.reserve(-17, -7, -7, 3)
    xa, xb, za, zb = -16, -8, -6, 2
    for x in range(xa, xb + 1):
        for z in (za, zb):
            s.set(x, Y0, z, 'minecraft:spruce_fence')
    for z in range(za, zb + 1):
        s.set(xa, Y0, z, 'minecraft:spruce_fence')
        s.set(xb, Y0, z, 'minecraft:spruce_fence')
    s.set(xb, Y0, -2, 'minecraft:spruce_fence_gate[facing=east,open=false]')
    s.set(xb, -59, -3, 'minecraft:lantern[hanging=false]', late=True)
    # The coop on stilts
    cx1, cx2, cz1, cz2 = -15, -13, -5, -3
    for x in (cx1, cx2):
        for z in (cz1, cz2):
            s.set(x, Y0, z, 'minecraft:spruce_fence')
    for x in range(cx1, cx2 + 1):
        for z in range(cz1, cz2 + 1):
            s.set(x, -59, z, 'minecraft:spruce_planks')
            for y in (-58, -57):
                corner = x in (cx1, cx2) and z in (cz1, cz2)
                side = x in (cx1, cx2) or z in (cz1, cz2)
                if corner:
                    s.set(x, y, z, 'minecraft:stripped_spruce_log[axis=y]')
                elif side:
                    s.set(x, y, z, 'minecraft:birch_planks')
    s.set(cx2, -58, -4, 'minecraft:air')
    s.set(-14, -56, cz1, 'minecraft:birch_planks')
    s.set(-14, -56, cz2, 'minecraft:birch_planks')
    for z in range(cz1 - 1, cz2 + 2):
        s.set(-16, -57, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom]')
        s.set(-15, -56, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom]')
        s.set(-14, -55, z, 'minecraft:dark_oak_slab[type=bottom]')
        s.set(-13, -56, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom]')
        s.set(-12, -57, z, 'minecraft:dark_oak_stairs[facing=west,half=bottom]')
    # Ramp
    s.set(-12, -59, -4, 'minecraft:spruce_slab[type=bottom]')
    s.set(-11, Y0, -4, 'minecraft:spruce_slab[type=top]')
    s.set(-14, Y0, -4, 'minecraft:hay_block[axis=x]')
    s.set(-9, Y0, 1, 'minecraft:water_cauldron[level=2]')
    s.set(-9, Y0, -5, 'minecraft:composter[level=5]')
    # Worn ground in the run
    for x in range(xa + 1, xb):
        for z in range(za + 1, zb):
            r = R.random()
            if r < 0.35:
                s.set(x, GROUND, z, 'minecraft:coarse_dirt')
            elif r < 0.5:
                s.set(x, GROUND, z, 'minecraft:dirt')
            elif r < 0.6:
                s.set(x, GROUND, z, 'minecraft:rooted_dirt')
    for x, z, yaw, baby in ((-11.3, -1.2, 70, False), (-10.2, 0.6, 200, False), (-12.8, 0.9, 300, False), (-9.6, -3.8, 110, False),
                            (-12.4, -4.3, 20, False), (-10.8, 0.1, 230, True), (-11.6, 1.2, 150, True)):
        s.summon('minecraft:chicken', x, -60.0, z, '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[%df,0f]%s}'
                 % (yaw, ',Age:-24000' if baby else ''))
    s.summon('minecraft:chicken', -11.5, -58.95, -3.6, '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[90f,0f]}')


# ---------------------------------------------------------------- sheep pen and barn (south-west)

def sheep_pen(s):
    s.reserve(-23, 3, -9, 14)
    xa, xb, za, zb = -21, -10, 4, 13
    for x in range(xa, xb + 1):
        for z in (za, zb):
            s.set(x, Y0, z, 'minecraft:oak_fence')
    for z in range(za, zb + 1):
        s.set(xa, Y0, z, 'minecraft:oak_fence')
        s.set(xb, Y0, z, 'minecraft:oak_fence')
    s.set(xb, Y0, 6, 'minecraft:oak_fence_gate[facing=east,open=false]')
    # Open barn at the back
    bx1, bx2, bz1, bz2 = -20, -17, 6, 11
    for x in range(bx1, bx2 + 1):
        for z in range(bz1, bz2 + 1):
            s.set(x, GROUND, z, 'minecraft:coarse_dirt' if R.random() < 0.5 else 'minecraft:dirt')
    for z in (bz1, bz2):
        for y in range(-60, -55):
            s.set(bx2, y, z, 'minecraft:spruce_log[axis=y]')
        for y in range(-60, -56):
            s.set(bx1, y, z, 'minecraft:spruce_log[axis=y]')
    for z in range(bz1 + 1, bz2):
        for y in (-60, -59, -58, -57):
            s.set(bx1, y, z, 'minecraft:spruce_planks' if y < -57 else 'minecraft:stripped_spruce_log[axis=z]')
    for z in range(bz1 - 1, bz2 + 2):
        s.set(bx2 + 1, -55, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom]')
        s.set(bx2, -55, z, 'minecraft:dark_oak_slab[type=bottom]')
        s.set(bx2 - 1, -55, z, 'minecraft:dark_oak_slab[type=bottom]')
        s.set(bx2 - 2, -56, z, 'minecraft:dark_oak_slab[type=top]')
        s.set(bx1, -56, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom]') if z not in range(bz1, bz2 + 1) else None
        s.set(bx1 - 1, -57, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom]')
    for z in range(bz1, bz2 + 1):
        s.set(bx1, -56, z, 'minecraft:dark_oak_stairs[facing=east,half=bottom]')
    for x, z, y in ((-19, 7, -60), (-19, 8, -60), (-18, 7, -60), (-19, 7, -59), (-19, 10, -60)):
        s.set(x, y, z, 'minecraft:hay_block[axis=%s]' % R.choice('xz'))
    s.set(-18, Y0, 10, 'minecraft:water_cauldron[level=3]')
    s.set(-17, Y0, 10, 'minecraft:water_cauldron[level=3]')
    for x, z, yaw, color in ((-14.2, 8.4, 40, 0), (-12.4, 10.6, 250, 0), (-15.6, 11.2, 120, 12), (-13.0, 6.2, 300, 8)):
        s.summon('minecraft:sheep', x, -60.0, z, '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[%df,0f],Color:%db}' % (yaw, color))
    s.summon('minecraft:cow', -16.0, -60.0, 6.5, '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[80f,0f]}')
    s.summon('minecraft:sheep', -11.8, -60.0, 8.6, '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[200f,0f],Age:-24000}')


# ---------------------------------------------------------------- garden (south)

def garden(s):
    s.reserve(-9, 5, 8, 17)
    xa, xb, za, zb = -8, 7, 5, 17
    for x in range(xa, xb + 1):
        for z in (za, zb):
            s.set(x, Y0, z, 'minecraft:birch_fence')
    for z in range(za, zb + 1):
        s.set(xa, Y0, z, 'minecraft:birch_fence')
        s.set(xb, Y0, z, 'minecraft:birch_fence')
    s.set(0, Y0, za, 'minecraft:birch_fence_gate[facing=north,open=true]')
    s.set(1, Y0, za, 'minecraft:birch_fence_gate[facing=north,open=true]')
    # Paths between the beds
    for x in range(xa + 1, xb):
        for z in range(za + 1, zb):
            s.set(x, GROUND, z, 'minecraft:dirt_path' if R.random() < 0.7 else 'minecraft:coarse_dirt')
    beds = [(-7, 'minecraft:carrots[age=7]'), (-2, 'minecraft:wheat[age=7]'), (3, 'minecraft:potatoes[age=7]')]
    for bx, crop in beds:
        for x in range(bx, bx + 4):
            for z in range(7, 15):
                edge = x in (bx, bx + 3) or z in (7, 14)
                if edge:
                    s.set(x, Y0, z, 'minecraft:stripped_spruce_log[axis=%s]' % ('z' if x in (bx, bx + 3) else 'x'))
                else:
                    s.set(x, Y0, z, 'minecraft:farmland[moisture=7]')
                    c = crop
                    if crop.startswith('minecraft:potatoes') and z > 10:
                        c = 'minecraft:beetroots[age=3]'
                    if crop.startswith('minecraft:carrots') and z > 11:
                        c = 'minecraft:carrots[age=%d]' % R.choice([4, 5, 6])
                    s.set(x, -59, z, c, late=True)
    # Scarecrow in the wheat
    s.set(0, -59, 12, 'minecraft:oak_fence')
    s.set(0, -58, 12, 'minecraft:oak_fence')
    s.set(0, -57, 12, 'minecraft:hay_block[axis=y]')
    s.set(-1, -57, 12, 'minecraft:oak_fence')
    s.set(1, -57, 12, 'minecraft:oak_fence')
    s.set(0, -56, 12, 'minecraft:carved_pumpkin[facing=north]')
    s.set(0, -55, 12, 'minecraft:brown_carpet', late=True)
    # Composter, water barrel, berries and pumpkins
    s.set(6, Y0, 15, 'minecraft:composter[level=7]')
    s.set(6, Y0, 16, 'minecraft:water_cauldron[level=3]')
    s.set(-7, Y0, 16, 'minecraft:barrel[facing=up]')
    for z in range(8, 15, 2):
        s.set(-8, Y0, z, 'minecraft:birch_fence')
    for x, z in ((-10, 7), (-11, 9), (-10, 11), (-11, 13), (-10, 15)):
        s.set(x, Y0, z, 'minecraft:sweet_berry_bush[age=3]')
    for x, z in ((-4, 19), (-6, 20), (-3, 21), (5, 19), (6, 21)):
        s.set(x, Y0, z, 'minecraft:pumpkin')
        s.reserved.add((x, z))
    s.entities.append('summon minecraft:item_display 2.80 -58.90 6.70 {item:{id:"minecraft:iron_hoe",count:1},'
                      'transformation:{left_rotation:[0f,0f,0.2588190f,0.9659258f],right_rotation:[0f,0f,0f,1f],'
                      'translation:[0f,0f,0f],scale:[0.9f,0.9f,0.9f]}}')


# ---------------------------------------------------------------- fire pit (south-east) and pond

def fire_pit(s):
    s.reserve(4, 3, 10, 10)
    cx, cz = 7, 7
    s.set(cx, Y0, cz, 'minecraft:campfire[lit=true,signal_fire=false,facing=north]', late=True)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                continue
            s.set(cx + dx, GROUND, cz + dz, 'minecraft:coarse_dirt')
            if dx == 0 or dz == 0:
                s.set(cx + dx, Y0, cz + dz, R.choice(['minecraft:cobblestone_slab[type=bottom]', 'minecraft:mossy_cobblestone_slab[type=bottom]']))
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2), (-2, 1), (1, 2)):
        s.set(cx + dx, GROUND, cz + dz, 'minecraft:coarse_dirt' if R.random() < 0.6 else 'minecraft:rooted_dirt')
    for x in range(6, 9):
        s.set(x, Y0, 4, 'minecraft:stripped_spruce_log[axis=x]')
    for z in range(6, 9):
        s.set(10, Y0, z, 'minecraft:stripped_oak_log[axis=z]')
    s.set(5, Y0, 9, 'minecraft:oak_log[axis=y]')
    s.summon('minecraft:wolf', 9.6, -60.0, 4.8, '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Sitting:1b,Rotation:[225f,0f]}')


POND = [(11, 9), (18, 15)]


def pond_shape(x, z):
    cx, cz = 16.5, 14.0
    a = math.atan2(z - cz, x - cx)
    r = 3.6 + 0.6 * math.sin(a * 3 + 0.5) + 0.4 * math.sin(a * 5 - 1.0)
    return math.hypot((x - cx) * 0.85, z - cz) - r


def pond(s):
    s.reserve(11, 9, 23, 20)
    for x in range(9, 25):
        for z in range(7, 21):
            d = pond_shape(x, z)
            if d < 0:
                s.set(x, GROUND, z, 'minecraft:water')
                if d < -1.2:
                    s.set(x, GROUND - 1, z, 'minecraft:water')
                    s.set(x, GROUND - 2, z, R.choice(['minecraft:gravel', 'minecraft:mud', 'minecraft:dirt']))
                else:
                    s.set(x, GROUND - 1, z, R.choice(['minecraft:gravel', 'minecraft:dirt', 'minecraft:mud']))
                if d < -0.8 and R.random() < 0.12:
                    s.set(x, Y0, z, 'minecraft:lily_pad', late=True)
            elif d < 0.9:
                s.set(x, GROUND, z, pick([('minecraft:coarse_dirt', 3), ('minecraft:dirt', 2), ('minecraft:gravel', 1), ('minecraft:mud', 1),
                                          ('minecraft:grass_block', 3)]))
    for x, z, h in ((19, 10, 3), (20, 11, 2), (12, 15, 2), (13, 17, 3), (18, 18, 2), (21, 16, 2)):
        if pond_shape(x, z) >= 0:
            for y in range(h):
                s.set(x, Y0 + y, z, 'minecraft:sugar_cane', late=True)
    for x, z, state in ((14, 10, 'minecraft:mossy_cobblestone'), (15, 9, 'minecraft:stone'), (21, 14, 'minecraft:mossy_cobblestone')):
        if pond_shape(x, z) >= 0:
            s.set(x, Y0, z, state)


# ---------------------------------------------------------------- well and beehives (north-west)

def well(s):
    s.reserve(-14, -13, -8, -7)
    cx, cz = -11, -10
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                s.set(cx, GROUND, cz, 'minecraft:water')
                s.set(cx, GROUND - 1, cz, 'minecraft:water')
                continue
            s.set(cx + dx, Y0, cz + dz, pick(STONE_LOW))
            s.set(cx + dx, -59, cz + dz, 'minecraft:cobblestone_wall' if (dx == 0 or dz == 0) else 'minecraft:mossy_cobblestone_wall')
    s.set(cx, Y0, cz, 'minecraft:air')
    for dx in (-1, 1):
        s.set(cx + dx, -58, cz, 'minecraft:spruce_fence')
        s.set(cx + dx, -57, cz, 'minecraft:spruce_fence')
    for dx, face in ((-2, 'east'), (-1, 'east'), (0, None), (1, 'west'), (2, 'west')):
        for dz in (-1, 0, 1):
            if face is None:
                s.set(cx, -55, cz + dz, 'minecraft:spruce_slab[type=bottom]')
            else:
                y = -56 if abs(dx) == 1 else -57
                s.set(cx + dx, y, cz + dz, 'minecraft:spruce_stairs[facing=%s,half=bottom]' % face)
    s.set(cx, -56, cz, 'minecraft:spruce_log[axis=x]')
    s.set(cx, -57, cz, 'minecraft:chain[axis=y]', late=True)
    s.set(cx, -58, cz, 'minecraft:lantern[hanging=true]', late=True)
    s.entities.append('summon minecraft:item_display -9.30 -59.80 -8.60 {item:{id:"minecraft:bucket",count:1},'
                      'transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6f,0.6f,0.6f]}}')
    # Beehives on posts in a flower patch
    for x, z, face in ((-15, -12, 'east'), (-15, -9, 'east')):
        s.set(x, Y0, z, 'minecraft:stripped_spruce_log[axis=y]')
        s.set(x, -59, z, 'minecraft:beehive[facing=%s,honey_level=5]' % face)
    for i in range(0):
        s.summon('minecraft:bee', -13.5 + R.random() * 2, -58.6 + R.random() * 0.8, -11.5 + R.random() * 3,
                 '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[%df,0f]}' % R.randint(0, 359))


# ---------------------------------------------------------------- paths

PATHS = [
    # polylines of (x, z) and their half width
    ([(0.5, -7.5), (1.2, -4), (0.4, 0), (1.0, 3), (0.9, 5)], 0.95),
    ([(0.9, 17), (1.4, 22), (0.4, 28), (1.8, 36), (0.8, 46), (2.2, 60)], 0.8),
    ([(1.0, -1.5), (4, -1.2), (8, -0.6), (11.5, -1.2)], 0.7),
    ([(0.2, -1.0), (-3.5, -1.8), (-7.5, -2.0)], 0.7),
    ([(0.8, 2.5), (-3.5, 4.2), (-7.5, 5.6), (-9.5, 6.2)], 0.65),
    ([(-1.0, -7.8), (-4.5, -8.6), (-8.5, -9.6)], 0.6),
    ([(1.4, 2.5), (4, 3.3), (5.6, 4.6)], 0.55),
    ([(3.5, -7.6), (6.5, -6.6), (9.5, -4.4)], 0.55),
]


def dist_to_path(x, z):
    best = 99.0
    for points, w in PATHS:
        for (ax, az), (bx, bz) in zip(points, points[1:]):
            dx, dz = bx - ax, bz - az
            t = max(0.0, min(1.0, ((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz)))
            d = math.hypot(x - (ax + dx * t), z - (az + dz * t)) - w
            best = min(best, d)
    return best


def paths(s):
    for x in range(-25, 26):
        for z in range(-12, 62):
            d = dist_to_path(x + 0.5, z + 0.5) + (R.random() - 0.5) * 0.7
            if s.get(x, GROUND, z) is not None or s.get(x, Y0, z) is not None:
                continue
            if d < -0.25:
                s.set(x, GROUND, z, pick([('minecraft:dirt_path', 10), ('minecraft:coarse_dirt', 1), ('minecraft:grass_block', 2)]))
            elif d < 0.3:
                s.set(x, GROUND, z, pick([('minecraft:dirt_path', 3), ('minecraft:coarse_dirt', 1), ('minecraft:grass_block', 6)]))
            if d < 0.4:
                s.reserved.add((x, z))


# ---------------------------------------------------------------- terrain, ground cover and the forest

def noise(x, z):
    return (math.sin(x * 0.11 + 0.7) * math.cos(z * 0.09 - 0.3) + 0.7 * math.sin(x * 0.05 - z * 0.07 + 1.9)
            + 0.5 * math.sin((x + z) * 0.13 + 0.4) + 0.3 * math.cos((x - z) * 0.21))


def terrain(s, radius):
    for x in range(-radius, radius + 1):
        for z in range(-radius, radius + 1):
            r = math.hypot(x, z)
            if r < 24:
                continue
            if (x, z) in s.reserved:
                continue
            t = min(1.0, (r - 24) / 18.0) * max(0.0, min(1.0, (dist_to_path(x + 0.5, z + 0.5) - 1.0) / 5.0))
            h = int(round(max(0.0, (noise(x, z) + 0.9) * 1.6 * t)))
            if h > 0:
                s.height[(x, z)] = h


def emit_terrain(s, commands):
    rows = {}
    for (x, z), h in s.height.items():
        rows.setdefault((z, h), []).append(x)
    for (z, h), xs in rows.items():
        xs.sort()
        start = prev = xs[0]
        for x in xs[1:] + [None]:
            if x is not None and x == prev + 1:
                prev = x
                continue
            if h > 1:
                commands.append('fill %d %d %d %d %d %d minecraft:dirt' % (start, GROUND, z, prev, GROUND + h - 1, z))
            commands.append('fill %d %d %d %d %d %d minecraft:grass_block' % (start, GROUND + h, z, prev, GROUND + h, z))
            if x is not None:
                start = prev = x


def free(s, x, z, margin=0):
    for dx in range(-margin, margin + 1):
        for dz in range(-margin, margin + 1):
            if (x + dx, z + dz) in s.reserved:
                return False
    return True


TREE_TYPES = [('minecraft:spruce', 30), ('minecraft:birch', 14), ('minecraft:fancy_oak', 8), ('minecraft:oak', 12),
              ('minecraft:mega_spruce', 10), ('minecraft:pine', 8), ('minecraft:birch_bees_0002', 4), ('minecraft:oak_leaf_litter', 6),
              ('minecraft:birch_leaf_litter', 6), ('minecraft:super_birch_bees_0002', 2)]
NEAR_TREES = [('minecraft:birch', 4), ('minecraft:fancy_oak', 3), ('minecraft:oak_leaf_litter', 3), ('minecraft:spruce', 4),
              ('minecraft:birch_leaf_litter', 2)]
UNDERGROWTH = [('minecraft:grass', 6), ('minecraft:taiga_grass', 4), ('minecraft:large_fern', 3), ('minecraft:bush', 3),
               ('minecraft:firefly_bush', 1), ('minecraft:berry_bush', 1), ('minecraft:leaf_litter', 4), ('minecraft:forest_flowers', 2),
               ('minecraft:wildflower', 2), ('minecraft:brown_mushroom', 1), ('minecraft:tall_grass', 2)]


def forest(s, radius):
    placed = []
    attempts = 0
    while attempts < 60000:
        attempts += 1
        a = R.random() * math.tau
        r = 17 + (R.random() ** 0.8) * (radius - 17)
        x, z = int(round(math.cos(a) * r)), int(round(math.sin(a) * r))
        if not free(s, x, z, 3):
            continue
        spacing = 3.2 if r < 30 else 4.2
        if any((x - px) ** 2 + (z - pz) ** 2 < spacing * spacing for px, pz in placed):
            continue
        placed.append((x, z))
        kind = pick(NEAR_TREES if r < 24 else TREE_TYPES)
        s.features.append((kind, x, s.ground_y(x, z) + 1, z))
    # Fallen trees and rocks near the edge, undergrowth everywhere in the forest
    for i in range(14):
        a = R.random() * math.tau
        r = 19 + R.random() * 25
        x, z = int(math.cos(a) * r), int(math.sin(a) * r)
        if free(s, x, z, 4):
            s.features.append((R.choice(['minecraft:fallen_oak_tree', 'minecraft:fallen_spruce_tree', 'minecraft:fallen_birch_tree']),
                               x, s.ground_y(x, z) + 1, z))
    for i in range(10):
        a = R.random() * math.tau
        r = 18 + R.random() * 30
        x, z = int(math.cos(a) * r), int(math.sin(a) * r)
        if free(s, x, z, 3):
            s.features.append(('minecraft:forest_rock', x, s.ground_y(x, z) + 1, z))
    for i in range(420):
        a = R.random() * math.tau
        r = 16 + R.random() * (radius - 16)
        x, z = int(math.cos(a) * r), int(math.sin(a) * r)
        if free(s, x, z, 2):
            s.features.append((pick(UNDERGROWTH), x, s.ground_y(x, z) + 1, z))
    return placed


PLANTS = [('minecraft:short_grass', 40), ('minecraft:fern', 7), ('TALL_GRASS', 7), ('LARGE_FERN', 2), ('minecraft:bush', 3)]
FLOWER_KINDS = ['minecraft:dandelion', 'minecraft:poppy', 'minecraft:oxeye_daisy', 'minecraft:cornflower', 'minecraft:azure_bluet',
                'minecraft:lily_of_the_valley', 'WILDFLOWERS', 'minecraft:allium', 'minecraft:red_tulip', 'minecraft:white_tulip']


def clearing_cover(s):
    for x in range(-24, 25):
        for z in range(-24, 25):
            r = math.hypot(x, z)
            if r > 16.5 or (x, z) in s.reserved:
                continue
            if s.get(x, GROUND, z) is not None or s.get(x, Y0, z) is not None or s.get(x, Y0 + 1, z) is not None:
                continue
            n = math.sin(x * 0.35 + 1.1) * math.cos(z * 0.31 - 0.6)
            if n > 0.72 and R.random() < 0.5:
                s.set(x, GROUND, z, R.choice(['minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:rooted_dirt', 'minecraft:moss_block']))
            density = 0.55 + 0.25 * min(1.0, r / 16.0)
            # Flowers grow in patches, each patch with one or two kinds
            patch = math.sin(x * 0.55 + 0.3) * math.sin(z * 0.47 - 1.2) + 0.5 * math.sin((x - z) * 0.8)
            if patch > 0.55 and R.random() < 0.65:
                f = FLOWER_KINDS[int((math.floor(x / 4) * 7 + math.floor(z / 4) * 3 + (1 if R.random() < 0.25 else 0)) % len(FLOWER_KINDS))]
            elif R.random() < density:
                f = pick(PLANTS)
            else:
                f = None
            if f is not None:
                if f == 'WILDFLOWERS':
                    s.set(x, Y0, z, 'minecraft:wildflowers[flower_amount=%d,facing=%s]' % (R.randint(1, 4), R.choice(['north', 'east', 'south', 'west'])), late=True)
                elif f in ('TALL_GRASS', 'LARGE_FERN'):
                    name = 'minecraft:tall_grass' if f == 'TALL_GRASS' else 'minecraft:large_fern'
                    s.set(x, Y0, z, name + '[half=lower]', late=True)
                    s.set(x, Y0 + 1, z, name + '[half=upper]', late=True)
                else:
                    s.set(x, Y0, z, f, late=True)


# ---------------------------------------------------------------- output

def build():
    s = Scene()
    radius = 88
    s.setup += [
        'gamerule advance_time false', 'gamerule advance_weather false', 'gamerule random_tick_speed 0', 'gamerule spawn_mobs false',
        'weather clear', 'difficulty peaceful',
        'forceload add -%d -%d %d %d' % (radius + 8, radius + 8, radius + 8, radius + 8),
    ]
    for bx in range(-radius - 16, radius + 16, 32):
        for bz in range(-radius - 16, radius + 16, 32):
            s.setup.append('fillbiome %d -64 %d %d -41 %d minecraft:forest' % (bx, bz, bx + 31, bz + 31))
    cabin(s)
    woodshed(s)
    chickens(s)
    sheep_pen(s)
    garden(s)
    fire_pit(s)
    pond(s)
    well(s)
    paths(s)
    terrain(s, radius)
    for feature, x, z in (('minecraft:fancy_oak', -11, -18), ('minecraft:birch', 17, -9), ('minecraft:birch', 19, -7),
                          ('minecraft:fancy_oak', 22, 4), ('minecraft:spruce', -23, 0), ('minecraft:fancy_oak', -12, 19)):
        s.features.append((feature, x, Y0, z))
        s.reserve(x, z, x, z, margin=1)
    # Mossy boulders at the edge of the clearing
    for bx, bz in ((-6, 9.5), (9, -9), (-15, -3.5), (15.5, 3), (-4, 20), (11, 19), (-19, -12), (5, -24)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                x, z = int(bx) + dx, int(bz) + dz
                if (x, z) in s.reserved or R.random() < 0.3:
                    continue
                s.set(x, Y0, z, pick([('minecraft:mossy_cobblestone', 4), ('minecraft:cobblestone', 2), ('minecraft:stone', 2), ('minecraft:tuff', 1)]))
                if dx == 0 and dz == 0 and R.random() < 0.6:
                    s.set(x, Y0 + 1, z, 'minecraft:mossy_cobblestone')
                elif R.random() < 0.4:
                    s.set(x, Y0 + 1, z, 'minecraft:moss_carpet', late=True)
        s.reserve(int(bx), int(bz), int(bx), int(bz), margin=1)
    trees = forest(s, radius)
    clearing_cover(s)

    commands = list(s.setup)
    emit_terrain(s, commands)
    for feature, x, y, z in s.features:
        commands.append('place feature %s %d %d %d' % (feature, x, y, z))
    # Blocks bottom up, then the ones that hang on others, then the clearing gets cleaned of anything trees dropped into it
    for (x, y, z), state in sorted(s.blocks.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
        if state == 'minecraft:air' and y > GROUND:
            continue
        commands.append('setblock %d %d %d %s' % (x, y, z, state))
    for (x, y, z), state in sorted(s.late.items(), key=lambda kv: (-kv[0][1], kv[0][0], kv[0][2])):
        commands.append('setblock %d %d %d %s' % (x, y, z, state))
    commands += s.entities
    commands.append('kill @e[type=minecraft:item]')
    commands.append('gamemode spectator @a')
    return s, commands, trees


COLORS = {
    'grass_block': (95, 160, 70), 'dirt_path': (190, 160, 100), 'coarse_dirt': (120, 90, 60), 'dirt': (130, 95, 65), 'gravel': (140, 135, 130),
    'water': (60, 110, 220), 'farmland': (90, 60, 40), 'spruce': (110, 80, 50), 'dark_oak': (70, 45, 25), 'stone': (130, 130, 130),
    'cobble': (120, 120, 120), 'birch': (220, 210, 170), 'oak': (170, 135, 80), 'fence': (150, 110, 70), 'hay': (220, 190, 60),
    'podzol': (100, 70, 40), 'moss': (90, 120, 50), 'mud': (70, 60, 60), 'rooted': (140, 100, 70), 'flower': (230, 120, 160),
}


def draw_map(s, trees, png):
    from PIL import Image, ImageDraw
    size, scale = 120, 6
    img = Image.new('RGB', (size * scale, size * scale), (60, 120, 50))
    d = ImageDraw.Draw(img)
    top = {}
    for (x, y, z), state in list(s.blocks.items()) + list(s.late.items()):
        if state == 'minecraft:air':
            continue
        if (x, z) not in top or top[(x, z)][0] < y:
            top[(x, z)] = (y, state)
    for (x, z), (y, state) in top.items():
        name = state.split('[')[0].replace('minecraft:', '')
        c = (200, 200, 200)
        for key, col in COLORS.items():
            if key in name:
                c = col
                break
        if name in ('short_grass', 'tall_grass', 'fern') or 'wildflowers' in name:
            continue
        if any(f in name for f in ('dandelion', 'poppy', 'daisy', 'cornflower', 'bluet', 'lily_of')):
            c = COLORS['flower']
        px, pz = (x + size // 2) * scale, (z + size // 2) * scale
        d.rectangle([px, pz, px + scale - 1, pz + scale - 1], fill=c)
    for x, z in trees:
        px, pz = (x + size // 2) * scale, (z + size // 2) * scale
        d.ellipse([px - 8, pz - 8, px + 8, pz + 8], outline=(20, 60, 20), width=2)
    px, pz = (size // 2) * scale, (size // 2) * scale
    d.ellipse([px - 5, pz - 5, px + 5, pz + 5], fill=(255, 0, 0))
    img.save(png)


if __name__ == '__main__':
    scene, cmds, tree_spots = build()
    with open(sys.argv[1], 'w') as f:
        f.write('\n'.join(cmds) + '\n')
    print(len(cmds), 'commands,', len(scene.blocks) + len(scene.late), 'blocks,', len(scene.features), 'features,', len(scene.entities), 'entities')
    if len(sys.argv) > 2:
        draw_map(scene, tree_spots, sys.argv[2])
