from pathlib import Path
from PIL import Image, ImageDraw
out=Path(__file__).resolve().parent
im=Image.new('RGBA',(48,48),(0,0,0,0));d=ImageDraw.Draw(im)
outline='#384951'
d.polygon([(4,22),(42,6),(33,41),(23,30),(17,35),(16,27)],fill=outline)
d.polygon([(8,22),(38,10),(18,26)],fill='#F6FAF8')
d.polygon([(19,27),(38,12),(23,29),(19,32)],fill='#91A9AF')
d.polygon([(25,28),(39,12),(32,37)],fill='#D2E0E0')
d.line([(25,28),(32,37)],fill='#B8CDD1',width=1)
im.save(out/'icon-48.png')
im.resize((768,768),Image.Resampling.NEAREST).save(out/'icon-preview.png')
im.resize((480,480),Image.Resampling.NEAREST).save(out/'icon-platform.png')
