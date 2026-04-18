import os
import re

ui_dir = 'android-app/app/src/main/java/com/dailywork/attedance/ui'

pattern = re.compile(r'Text\(\s*"([^"]+)"')

all_strings = set()

for root, _, files in os.walk(ui_dir):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as file:
                content = file.read()
                matches = pattern.findall(content)
                for m in matches:
                    if not m.startswith("$") and not m.startswith("+91") and not m.startswith("/") and not m.startswith("-") and not m.startswith("+"):
                        all_strings.add(m)

print(sorted(list(all_strings)))
