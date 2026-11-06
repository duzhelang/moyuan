import zipfile, shutil, os
src = r"D:\毕设\2027届本科毕业论文（设计）开题答辩材料\《南京传媒学院本科毕业论文（设计）开题报告》_杜哲浪.docx"
bak = src + ".bak"
if not os.path.exists(bak):
    shutil.copy2(src, bak)
print("备份:", bak)
with zipfile.ZipFile(src) as z:
    xml = z.read("word/document.xml").decode("utf-8")
print("document.xml 长度:", len(xml))
print("keepLines 出现次数:", xml.count("<w:keepLines"))
i = 0
while True:
    i = xml.find("<w:keepLines", i)
    if i < -1: break
    if i == -1: break
    print("位置:", i, "上下文:", xml[max(0,i-300):i+120].replace("\n"," ")[-420:])
    i += 1
