import os
import re

ui_dir = 'android-app/app/src/main/java/com/dailywork/attedance/ui'
strings_xml_path = 'android-app/app/src/main/res/values/strings.xml'
hi_strings_xml_path = 'android-app/app/src/main/res/values-hi/strings.xml'

# Load existing strings to not overwrite
existing_keys = set()
with open(strings_xml_path, 'r', encoding='utf-8') as f:
    for line in f:
        m = re.search(r'name="([^"]+)"', line)
        if m:
            existing_keys.add(m.group(1))

def generate_key(s):
    s_clean = re.sub(r'[^a-zA-Z0-9\s]', '', s)
    s_clean = s_clean.strip()
    s_clean = re.sub(r'\s+', '_', s_clean).lower()

    if s_clean and s_clean[0].isdigit():
        s_clean = "num_" + s_clean

    if len(s_clean) == 0:
        return 'rupee_symbol' if '₹' in s else 'blank'
    if len(s_clean) > 40:
        return s_clean[:40] + "_msg"
    return s_clean

# Find all Text("...") and title = { Text("...") } and label = { Text("...") }
pattern = re.compile(r'Text\(\s*"([^"]+)"')
# We need to replace exactly the full string. To be safe, we'll replace Text("string") with Text(stringResource(R.string.key))

strings_to_add = {}

for root, _, files in os.walk(ui_dir):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as file:
                content = file.read()

            matches = pattern.findall(content)
            new_content = content
            changed = False

            for m in matches:
                # Skip strings with template vars or simple symbols unless we want to parse them
                if "$" in m or m in ["+", "-", "/", "+91 ", "₹", "🛠️", "OR"] or len(m.strip()) == 0:
                    continue

                k = generate_key(m)

                # handle duplicates within our newly generated set and existing
                orig_k = k
                idx = 1
                while k in existing_keys or (k in strings_to_add and strings_to_add[k] != m):
                    k = f"{orig_k}_{idx}"
                    idx += 1

                strings_to_add[k] = m

                # Create replacement
                # Using androidx.compose.ui.res.stringResource so we don't need to add imports everywhere
                replacement = f'Text(androidx.compose.ui.res.stringResource(com.dailywork.attedance.R.string.{k})'
                # Replace exact match of Text("m"
                # Need to handle exact quotes
                import re
                escaped_m = re.escape(m)
                # re.sub with care
                new_content, count = re.subn(rf'Text\(\s*"{escaped_m}"', replacement, new_content)
                if count > 0:
                    changed = True

            if changed:
                with open(path, 'w', encoding='utf-8') as file:
                    file.write(new_content)

# Update strings.xml
with open(strings_xml_path, 'r', encoding='utf-8') as f:
    content = f.read()

# remove </resources>
content = content.replace("</resources>", "")

for k, v in strings_to_add.items():
    v_escaped = v.replace("'", "\\'").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    content += f'    <string name="{k}">{v_escaped}</string>\n'

content += "</resources>\n"

with open(strings_xml_path, 'w', encoding='utf-8') as f:
    f.write(content)

# Also create Hindi equivalents (using English text for now, but ready to be translated)
with open(hi_strings_xml_path, 'r', encoding='utf-8') as f:
    hi_content = f.read()

hi_content = hi_content.replace("</resources>", "")

for k, v in strings_to_add.items():
    v_escaped = v.replace("'", "\\'").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    hi_content += f'    <string name="{k}">{v_escaped}</string>\n'

hi_content += "</resources>\n"

with open(hi_strings_xml_path, 'w', encoding='utf-8') as f:
    f.write(hi_content)

print(f"Extracted {len(strings_to_add)} strings.")
