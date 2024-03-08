import urllib.request, json 

hex_chars = [f"{i:02x}" for i in range(256)]
print(hex_chars)

for hex in hex_chars:
    with urllib.request.urlopen(f"https://raw.githubusercontent.com/TomHarte/ProcessorTests/main/nes6502/v1/{hex}.json") as url:
        data = json.load(url)
        with open(f"./{hex}.json", "w") as file:
            json.dump(data, file, indent=4)
        print(f"Got {hex}")


