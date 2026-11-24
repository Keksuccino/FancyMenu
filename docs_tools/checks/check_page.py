"""Checks a docs page: bold UI labels against en_us.json, links and anchors into other pages, and image files.

Usage: python3 check_page.py <docs repo> <workspace> <page name> [more page names]
Example: python3 check_page.py ../../docs_fancymenu ../../fancymenu-26.3.0 scrollable-areas text-in-menus

Bold text counts as a label (menu paths with "->" get split). Labels match with or without their value ("Mode: %s" matches "Mode").
Bold text that is no UI label (like "Pixel" or "bold") shows up as MISS too, so read the list instead of trusting it blindly.
Code blocks and inline code get skipped.
"""
import json, os, re, sys

docs, workspace, pages = sys.argv[1], sys.argv[2], sys.argv[3:]
lang = json.load(open(os.path.join(workspace, "common/src/main/resources/assets/fancymenu/lang/en_us.json")))
labels = set()
for value in lang.values():
    value = re.sub(r"§.", "", value).strip()
    labels.add(value)
    labels.add(re.sub(r":? ?%s.*$", "", value).strip())
    # Labels with a value that is part of the text, like "Case Mode: Normal"
    labels.add(value.split(": ")[0].strip())


def strip_code_blocks(text):
    return re.sub(r"^(`{3,}|~{3,}).*?^\1", "", text, flags=re.S | re.M)


def strip_code(text):
    return re.sub(r"`+[^`\n]*`+", "", strip_code_blocks(text))


def slug(heading):
    heading = re.sub(r"[^\w\- ]", "", heading.strip().lower())
    return heading.replace(" ", "-")


def anchors(page):
    path = os.path.join(docs, "docs", page + ".md")
    # Inline code stays in headings, because it is part of their anchors, like "## Name (`identifier`)"
    return {slug(h) for h in re.findall(r"^#+ (.+)$", strip_code_blocks(open(path).read()), flags=re.M)}


problems = 0
for page in pages:
    text = strip_code(open(os.path.join(docs, "docs", page + ".md")).read())
    print(f"== {page}")
    for label in sorted(set(re.findall(r"\*\*([^*\n]+)\*\*", text))):
        missing = [part for part in (p.strip() for p in label.split("->")) if part not in labels]
        if missing:
            problems += 1
            print(f"MISS label  {label}  (not in en_us.json: {missing})")
    for target, anchor in sorted(set(re.findall(r"\]\(\./([^)#\s]+)(?:#([^)\s]+))?\)", text))):
        if not os.path.exists(os.path.join(docs, "docs", target + ".md")):
            problems += 1
            print(f"MISS page   ./{target}")
        elif anchor and anchor not in anchors(target):
            problems += 1
            print(f"MISS anchor ./{target}#{anchor}")
    for anchor in sorted(set(re.findall(r"\]\(#([^)\s]+)\)", text))):
        if anchor not in anchors(page):
            problems += 1
            print(f"MISS anchor #{anchor}")
    for asset in sorted(set(re.findall(r"refs/heads/wiki-2026/(assets/[^?\"\s]+)", text))):
        if not os.path.exists(os.path.join(docs, asset)):
            problems += 1
            print(f"MISS image  {asset}")
print(f"{problems} problem(s)")
