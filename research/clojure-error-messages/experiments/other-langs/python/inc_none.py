config = {"port": 8080}

def next_port(c):
    return c.get("prot") + 1

next_port(config)
