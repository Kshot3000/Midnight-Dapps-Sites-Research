"""Check the donation details in the native window and the shipped README."""

from pathlib import Path
from xml.etree import ElementTree

APP = Path(__file__).resolve().parents[1]
XAML_NAME = "{http://schemas.microsoft.com/winfx/2006/xaml}Name"
XAML = "{http://schemas.microsoft.com/winfx/2006/xaml/presentation}"
ADDRESSES = {
    "AdaAddress": "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
    "MidnightAddress": "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
}
ATTRIBUTION = ["@kshot9000", "Independent builder — not affiliated with the Midnight Foundation or IOG."]

xaml = (APP / "MainWindow.xaml").read_text(encoding="utf-8")
window = ElementTree.fromstring(xaml)
readme = (APP / "README.md").read_text(encoding="utf-8")
for name, address in ADDRESSES.items():
    fields = [field for field in window.iter(XAML + "TextBox") if field.get(XAML_NAME) == name]
    if len(fields) != 1 or fields[0].get("Text") != address or f"`{address}`" not in readme:
        raise SystemExit(f"{name} is missing or differs from the project brief.")

for text in ATTRIBUTION:
    if text not in xaml or text not in readme:
        raise SystemExit(f"Missing attribution: {text}")

print("PASS · Both exact addresses and attribution are present in the GUI and README")
