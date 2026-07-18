import xml.etree.ElementTree as ET
root = ET.parse('target/site/jacoco/jacoco.xml').getroot()
for pkg in root.findall('package'):
    for cls in pkg.findall('class'):
        instr = line = branch = 0
        for c in cls.findall('counter'):
            t = c.get('type')
            m = int(c.get('missed'))
            if t == 'INSTRUCTION': instr = m
            elif t == 'LINE': line = m
            elif t == 'BRANCH': branch = m
        if instr or line or branch:
            print(pkg.get('name'), cls.get('name'), instr, line, branch)
