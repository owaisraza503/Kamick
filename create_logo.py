import subprocess
import os

# Build rich 1024x1024 Kamick by Raza logo matching user uploaded graphic
cmd = [
    "convert",
    "-size", "1024x1024",
    "xc:white",

    # Bottom dark crescent arc
    "-stroke", "#262626", "-strokewidth", "14", "-fill", "none",
    "-draw", "arc 200,680 824,840 20,160",

    # Red Sun/Circle Emblem
    "-stroke", "#1A1A1A", "-strokewidth", "16", "-fill", "#DC2626",
    "-draw", "circle 512,460 512,150",

    # Red cape folds
    "-stroke", "#991B1B", "-strokewidth", "8", "-fill", "#B91C1C",
    "-draw", "polygon 220,380 340,480 260,570 190,470",
    "-draw", "polygon 804,380 684,480 764,570 834,470",

    # Pen 1 (Left G-Pen Dip Nib)
    "-stroke", "#111827", "-strokewidth", "12", "-fill", "#1F2937",
    "-draw", "polygon 220,290 320,390 350,360 250,260",
    "-fill", "#E2E8F0",
    "-draw", "polygon 250,260 220,290 170,220",
    "-fill", "#0F172A",
    "-draw", "circle 215,255 215,258",

    # Pen 2 (Right G-Pen Dip Nib)
    "-stroke", "#111827", "-strokewidth", "12", "-fill", "#1F2937",
    "-draw", "polygon 804,290 704,390 674,360 774,260",
    "-fill", "#E2E8F0",
    "-draw", "polygon 774,260 804,290 854,220",
    "-fill", "#0F172A",
    "-draw", "circle 809,255 809,258",

    # Cat Head Outline & Base (White)
    "-stroke", "#18181B", "-strokewidth", "16", "-fill", "#FFFFFF",
    # Cat Ears Left & Right
    "-draw", "polygon 300,320 280,160 410,240",
    "-draw", "polygon 724,320 744,160 614,240",

    # Inner Ear (Pink)
    "-stroke", "none", "-fill", "#FCA5A5",
    "-draw", "polygon 315,300 300,185 395,245",
    "-draw", "polygon 709,300 724,185 629,245",

    # Cat Face Main (Cheeks & Head)
    "-stroke", "#18181B", "-strokewidth", "16", "-fill", "#FFFFFF",
    "-draw", "ellipse 512,380 230,190 0,360",
    # Cheek fur tufts left & right
    "-draw", "polygon 280,360 220,400 310,430",
    "-draw", "polygon 270,420 230,460 330,470",
    "-draw", "polygon 744,360 804,400 714,430",
    "-draw", "polygon 754,420 794,460 694,470",

    # Black Shinobi Headband
    "-stroke", "#0F172A", "-strokewidth", "14", "-fill", "#18181B",
    "-draw", "roundrectangle 320,250 704,335 15,15",
    # Headband shine/band fold
    "-stroke", "#475569", "-strokewidth", "6", "-fill", "none",
    "-draw", "line 340,275 684,275",

    # Cat Eyes (Fierce Anime Amber Eyes)
    "-stroke", "#0F172A", "-strokewidth", "10", "-fill", "#F59E0B",
    "-draw", "polygon 370,395 440,365 445,415 385,425",
    "-draw", "polygon 654,395 584,365 579,415 639,425",
    # Cat Pupils
    "-stroke", "none", "-fill", "#0F172A",
    "-draw", "ellipse 420,390 12,20 0,360",
    "-draw", "ellipse 604,390 12,20 0,360",
    # Eye highlights
    "-fill", "#FFFFFF",
    "-draw", "circle 424,383 424,387",
    "-draw", "circle 608,383 608,387",

    # Cat Nose & Mouth
    "-stroke", "#0F172A", "-strokewidth", "6", "-fill", "#EF4444",
    "-draw", "polygon 500,420 524,420 512,434",
    "-stroke", "#0F172A", "-strokewidth", "6", "-fill", "none",
    "-draw", "arc 494,430 512,448 30,160",
    "-draw", "arc 512,430 530,448 20,150",

    # Open Manga Comic Book in Hands
    "-stroke", "#0F172A", "-strokewidth", "16", "-fill", "#F8FAFC",
    "-draw", "polygon 260,430 500,450 500,600 240,580",
    "-draw", "polygon 524,450 764,430 784,580 524,600",
    # Book Spine & Cover Edge
    "-stroke", "#0F172A", "-strokewidth", "12", "-fill", "#334155",
    "-draw", "polygon 500,450 524,450 524,600 500,600",
    # Left Comic Page Panels
    "-stroke", "#0F172A", "-strokewidth", "8", "-fill", "#E2E8F0",
    "-draw", "rectangle 280,455 480,520",
    "-draw", "rectangle 280,535 480,570",
    # Manga face speedlines in left panel
    "-stroke", "#0F172A", "-strokewidth", "4",
    "-draw", "line 300,465 350,510",
    "-draw", "line 320,460 370,515",
    "-draw", "line 460,465 420,510",

    # Right Comic Page Panels
    "-stroke", "#0F172A", "-strokewidth", "8", "-fill", "#E2E8F0",
    "-draw", "rectangle 544,455 744,570",
    # Speech bubble on right page with 3 dots
    "-stroke", "#0F172A", "-strokewidth", "8", "-fill", "#FFFFFF",
    "-draw", "ellipse 644,495 55,35 0,360",
    "-stroke", "none", "-fill", "#0F172A",
    "-draw", "circle 625,495 625,499",
    "-draw", "circle 644,495 644,499",
    "-draw", "circle 663,495 663,499",

    # Cat Paws Holding the Book
    "-stroke", "#0F172A", "-strokewidth", "14", "-fill", "#FFFFFF",
    "-draw", "ellipse 260,540 38,28 0,360",
    "-draw", "ellipse 764,540 38,28 0,360",

    # Title: "Kamick" (Stylized 3D Comic Text)
    # Shadow
    "-font", "DejaVu-Sans-Bold", "-pointsize", "140",
    "-stroke", "#0F172A", "-strokewidth", "26", "-fill", "#0F172A",
    "-gravity", "North",
    "-annotate", "+0+576", "Kamick",
    "-annotate", "+4+580", "Kamick",
    # Inner 3D Extrusion
    "-stroke", "#94A3B8", "-strokewidth", "14", "-fill", "#E2E8F0",
    "-annotate", "+0+570", "Kamick",
    # Face White
    "-stroke", "#0F172A", "-strokewidth", "8", "-fill", "#FFFFFF",
    "-annotate", "+0+565", "Kamick",

    # Subtitle: "by Raza"
    # Red accent dashes
    "-draw", "line 340,725 380,725",
    "-draw", "line 644,725 684,725",
    # Yellow Text
    "-font", "DejaVu-Sans-Bold", "-pointsize", "62",
    "-stroke", "#B45309", "-strokewidth", "4", "-fill", "#FBBF24",
    "-gravity", "North",
    "-annotate", "+0+700", "by Raza",

    "app/src/main/res/drawable/kamick_logo.png"
]

subprocess.run(cmd, check=True)
print("Logo generated successfully!")
