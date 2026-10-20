from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import math
root=Path('src/main/resources/assets/fluxtweaks/textures/particle');root.mkdir(parents=True,exist_ok=True)
N=256
def polygon(draw,points,fill=255):draw.polygon([(int(x*N),int(y*N)) for x,y in points],fill=fill)
for name in ['stars','snowflakes','ghosts','hearts','diamonds','thunders','moons','clouds','triangles','cubes']:
 im=Image.new('L',(N,N));d=ImageDraw.Draw(im)
 if name=='stars':
  polygon(d,[(.5+(.42 if k%2==0 else .18)*math.cos(k*math.pi/5-math.pi/2),.5+(.42 if k%2==0 else .18)*math.sin(k*math.pi/5-math.pi/2)) for k in range(10)])
 elif name=='snowflakes':
  for k in range(6):
   a=k*math.pi/3;cx=cy=N*.5;ex=cx+N*.4*math.cos(a);ey=cy+N*.4*math.sin(a);d.line((cx,cy,ex,ey),fill=255,width=12)
   for t in [.48,.72]:
    bx=cx+(ex-cx)*t;by=cy+(ey-cy)*t
    for sign in [-1,1]:d.line((bx,by,bx-N*.13*math.cos(a+sign*.8),by-N*.13*math.sin(a+sign*.8)),fill=255,width=9)
 elif name=='ghosts':
  d.ellipse((42,24,214,196),fill=255);polygon(d,[(.165,.4),(.165,.86),(.3,.76),(.43,.9),(.56,.76),(.7,.9),(.835,.8),(.835,.4)])
  for x in [91,165]:d.ellipse((x-14,91,x+14,131),fill=0)
 elif name=='hearts':
  d.ellipse((22,35,143,153),fill=255);d.ellipse((113,35,234,153),fill=255);polygon(d,[(.095,.38),(.5,.9),(.905,.38)])
 elif name=='diamonds':polygon(d,[(.5,.07),(.88,.5),(.5,.93),(.12,.5)])
 elif name=='thunders':polygon(d,[(.52,.04),(.22,.57),(.48,.57),(.36,.96),(.82,.37),(.55,.37),(.7,.04)])
 elif name=='moons':d.ellipse((30,20,225,231),fill=255);d.ellipse((100,-5,266,177),fill=0)
 elif name=='clouds':
  d.rounded_rectangle((22,116,234,207),radius=43,fill=255)
  for box in [(41,72,135,171),(91,42,198,171),(156,91,233,186)]:d.ellipse(box,fill=255)
 elif name=='triangles':polygon(d,[(.5,.08),(.93,.87),(.07,.87)])
 elif name=='cubes':
  polygon(d,[(.5,.07),(.9,.3),(.5,.54),(.1,.3)],255);polygon(d,[(.1,.3),(.5,.54),(.5,.94),(.1,.7)],175);polygon(d,[(.5,.54),(.9,.3),(.9,.7),(.5,.94)],220)
 for glow in [False,True]:
  alpha=im if not glow else ImageChops.lighter(im,im.filter(ImageFilter.GaussianBlur(13)).point(lambda p:int(p*.65)))
  result=Image.new('RGBA',(N,N),(255,255,255,0));result.putalpha(alpha);result.resize((128,128),Image.Resampling.LANCZOS).save(root/(name+('_glow' if glow else '')+'.png'))
for glowing in [False,True]:
 im=Image.new('L',(N,N));d=ImageDraw.Draw(im);d.ellipse((22,22,234,234),outline=255,width=5)
 if glowing:im=ImageChops.lighter(im,im.filter(ImageFilter.GaussianBlur(9)).point(lambda p:min(255,p*3)))
 result=Image.new('RGBA',(N,N),(255,255,255,0));result.putalpha(im);result.save(root/('circle_glow.png' if glowing else 'circle.png'))
print('Generated',len(list(root.glob('*.png'))),'original procedural effect textures')
